package io.quarkiverse.asyncapi.annotation.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.enterprise.context.ApplicationScoped;

import com.asyncapi.v2.model.AsyncAPI;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkiverse.asyncapi.config.ObjectMapperFactory;
import io.quarkus.runtime.annotations.Recorder;

/**
 * @since 09.02.2023
 */
@Recorder
@ApplicationScoped
public class ScannedAsyncApi {

    private AsyncAPI asyncAPI;

    public void setAsyncAPI(AsyncAPI aAsyncAPI) {
        this.asyncAPI = aAsyncAPI;
        try {
            String json = ObjectMapperFactory.yaml().writeValueAsString(asyncAPI);
            store(json);
        } catch (JsonProcessingException e) {
            System.err.println("io.quarkiverse.asyncapi.generator.AsyncAPIResourceGenerator.scanAsyncAPIs()");
            e.printStackTrace();
        }
    }

    void store(String aJson) {
        try {
            Path path = Path.of("generatedsources");
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            Files.writeString(path, aJson, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AsyncAPI getAsyncAPI() {
        return asyncAPI;
    }
}
