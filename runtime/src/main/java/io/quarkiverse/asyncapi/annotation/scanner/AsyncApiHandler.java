package io.quarkiverse.asyncapi.annotation.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import com.asyncapi.v2.model.AsyncAPI;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @since 10.02.2023
 */
public class AsyncApiHandler implements Handler<RoutingContext> {

    public AsyncApiHandler() {
    }

    @Override
    public void handle(RoutingContext aRoutingContext) {
        HttpServerResponse resp = aRoutingContext.response();
        Format format = getFormat(aRoutingContext);
        resp.headers().set("Content-Type", format.getMimeType() + ";charset=UTF-8");
        try {
            String yaml = Files.readAllLines(AsyncApiRecorder.FILE).stream().collect(Collectors.joining("\n"));
            String output = switch (format) {
                case YAML ->
                    yaml;
                case JSON ->
                    convertYamlToJson(yaml, aRoutingContext);
                case HTML ->
                    getHtml(aRoutingContext);
                default ->
                    throw new AssertionError();
            };
            resp.end(Buffer.buffer(output)); // see http://localhost:8080/asyncapi
        } catch (IOException iOException) {
            resp.end(Buffer.buffer("Unable to read file " + AsyncApiRecorder.FILE));
        }
    }

    String convertYamlToJson(String aYaml, RoutingContext aRoutingContex) throws JsonProcessingException {
        AsyncAPI asyncAPI = ObjectMapperFactory.yaml().readValue(aYaml, AsyncAPI.class);
        return ObjectMapperFactory.json().writeValueAsString(asyncAPI);
    }

    String getHtml(RoutingContext aRoutingContext) {
        String version = ConfigProvider.getConfig()
                .getConfigValue("quarkus.asyncapi.annotation.scanner.webcomponentversion")
                .getValue();
        //TODO logo
        return """
                <!DOCTYPE html>
                <html lang="en">
                  <head>
                    <script src="https://unpkg.com/@webcomponents/webcomponentsjs@%s/webcomponents-bundle.js"></script>
                    <script
                      src="https://unpkg.com/@asyncapi/web-component@%s/lib/asyncapi-web-component.js"
                      defer
                    ></script>
                  </head>
                  <body>
                    <asyncapi-component
                      cssImportPath="https://unpkg.com/@asyncapi/react-component@%s/styles/default.css"
                      schemaUrl="http://%s/asyncapi.yaml"
                    >
                    </asyncapi-component>
                  </body>
                </html>
                """
                .formatted(version, version, version, aRoutingContext.request().host());
    }

    Format getFormat(RoutingContext event) {
        HttpServerRequest req = event.request();
        String path = event.normalizedPath();
        // Content negotiation with file extension
        Format format = Format.YAML;
        if (path.endsWith(".json")) {
            format = Format.JSON;
        } else if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            format = Format.YAML;
        } else if (path.endsWith(".html")) {
            format = Format.HTML;
        } else {
            // Content negotiation with Accept header
            String accept = req.headers().get("Accept");
            List<String> formatParams = event.queryParam("format");
            String formatParam = formatParams.isEmpty() ? null : formatParams.get(0);
            // Check Accept, then query parameter "format" for JSON; else use YAML.
            if ((accept != null && accept.contains(Format.JSON.getMimeType())) || ("JSON".equalsIgnoreCase(formatParam))) {
                format = Format.JSON;
            }
        }
        return format;
    }

    enum Format {
        JSON("application/json"),
        YAML("application/yaml"),
        HTML("text/html");

        private final String mimeType;

        Format(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }
    }
}
