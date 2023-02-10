package io.quarkiverse.asyncapi.annotation.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @since 10.02.2023
 */
public class AsyncApiHandler implements Handler<RoutingContext> {

    public AsyncApiHandler() {
    }

    @Override
    public void handle(RoutingContext event) {
        HttpServerResponse resp = event.response();
        resp.headers().set("Content-Type", "application/yaml;charset=UTF-8");
        try {
            String yaml = Files.readAllLines(AsyncApiRecorder.FILE).stream().collect(Collectors.joining("\n"));
            resp.end(Buffer.buffer(yaml)); // see http://localhost:8080/asyncapi
        } catch (IOException iOException) {
            resp.end(Buffer.buffer("Unable to read file " + AsyncApiRecorder.FILE));
        }
    }
}
