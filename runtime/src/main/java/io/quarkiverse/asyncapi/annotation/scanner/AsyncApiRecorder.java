package io.quarkiverse.asyncapi.annotation.scanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.enterprise.context.ApplicationScoped;

import com.asyncapi.v2.model.AsyncAPI;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.runtime.annotations.Recorder;

/**
 * @since 09.02.2023
 */
@Recorder
@ApplicationScoped
public class AsyncApiRecorder {

    static final String FOLDER = "target/classes/META-INF/resources";
    public static final Path FILE = Path.of(FOLDER + "/asyncApi.yaml");
    private AsyncAPI asyncAPI;

    public void setAsyncAPI(AsyncAPI aAsyncAPI) {
        this.asyncAPI = aAsyncAPI;
        try {
            String json = ObjectMapperFactory.yaml().writeValueAsString(asyncAPI);
            store(json);
        } catch (JsonProcessingException e) {
            System.err.println("io.quarkiverse.asyncapi.annotation.scanner.ScannedAsyncApi.scanAsyncAPIs()");
            e.printStackTrace();
        }
    }

    void store(String aJson) {
        try {
            Path dir = Path.of(FOLDER);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            if (!Files.exists(FILE)) {
                Files.createFile(FILE);
            }
            Files.writeString(FILE, aJson, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AsyncAPI getAsyncAPI() {
        return asyncAPI;
    }
}
