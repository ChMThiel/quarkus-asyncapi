package io.quarkiverse.asyncapi.annotation.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.microprofile.config.ConfigProvider;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @since 10.02.2023
 */
public class AsyncApiHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = Logger.getLogger(AsyncApiHandler.class.getName());

    @Override
    public void handle(RoutingContext aRoutingContext) {
        HttpServerResponse resp = aRoutingContext.response();
        Format format = getFormat(aRoutingContext);
        resp.headers().set("Content-Type", format.getMimeType() + ";charset=UTF-8");
        String output = switch (format) {
            case YAML, JSON ->
                read(format);
            case HTML ->
                getHtml(aRoutingContext);
            default ->
                throw new AssertionError();
        };
        resp.end(Buffer.buffer(output)); // see http://localhost:8080/asyncapi
    }

    String read(Format aFormat) {
        try {
            String fileName = switch (aFormat) {
                case YAML ->
                    AsyncApiRecorder.ASYNC_API_YAML;
                case JSON ->
                    AsyncApiRecorder.ASYNC_API_JSON;
                default ->
                    throw new AssertionError();
            };
            Path path = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
            return Files.readString(path);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "AsyncApiHandler.read() Unable to read file as " + aFormat, e);
            return "Unable to read file";
        }
    }

    String getHtml(RoutingContext aRoutingContext) {
        String version = ConfigProvider.getConfig()
                .getValue("quarkus.asyncapi.annotation.scanner.webcomponentversion", String.class);
        String rootPath = ConfigProvider.getConfig()
                .getValue("quarkus.http.root-path", String.class);
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
                      schemaUrl="%s/asyncapi.yaml"
                    >
                    </asyncapi-component>
                  </body>
                </html>
                """
                .formatted(version, version, version, rootPath);
    }

    Format getFormat(RoutingContext aRoutingContext) {
        String path = aRoutingContext.normalizedPath();
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
            String accept = aRoutingContext.request().headers().get("Accept");
            List<String> formatParams = aRoutingContext.queryParam("format");
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
