package io.quarkiverse.asyncapi.annotation.scanner;

import static io.quarkiverse.asyncapi.annotation.scanner.ObjectMapperFactory.json;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import com.asyncapi.v2.model.AsyncAPI;
import com.asyncapi.v2.model.channel.ChannelItem;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkiverse.asyncapi.annotation.scanner.config.AsyncApiRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;

/**
 * @since 09.02.2023
 */
@Recorder
@ApplicationScoped
public class AsyncApiRecorder {

    private static final Logger LOGGER = Logger.getLogger(AsyncApiRecorder.class.getName());

    public static final String ASYNC_API_JSON = "asyncapi.json";
    public static final String ASYNC_API_YAML = "asyncapi.yaml";

    public void setAsyncAPI(AsyncAPI aAsyncAPI, AsyncApiRuntimeConfig aConfig) {
        try {
            AsyncAPI filteredAPI = filter(aAsyncAPI, aConfig);
            store(ObjectMapperFactory.yaml().writeValueAsString(filteredAPI), ASYNC_API_YAML);
            store(json().writeValueAsString(filteredAPI), ASYNC_API_JSON);
        } catch (JsonProcessingException e) {
            LOGGER.throwing("io.quarkiverse.asyncapi.annotation.scanner.AsyncApiRecorder", "scanAsyncAPIs", e);
        }
    }

    void store(String aContent, String aFileName) {
        try {
            Path file = Files.createFile(Paths.get(System.getProperty("java.io.tmpdir"), aFileName));
            LOGGER.info("AsycnApiRecorder.store " + aFileName + " to " + file);
            Files.writeString(file, aContent, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.throwing("io.quarkiverse.asyncapi.annotation.scanner.AsyncApiRecorder", "store", e);
        }
    }

    AsyncAPI filter(AsyncAPI aAsyncAPI, AsyncApiRuntimeConfig aConfig) {
        AsyncAPI result = aAsyncAPI;
        AsyncApiFilter filter = getFilter(aConfig);
        if (filter != null) {
            result = filter.filterAsyncAPI(aAsyncAPI);
            Map<String, ChannelItem> filteredChannels = result.getChannels().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), filter.filterChannelItem(e.getKey(), e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            result.setChannels(filteredChannels);
        }
        return result;
    }

    AsyncApiFilter getFilter(AsyncApiRuntimeConfig aConfig) {
        Optional<String> filterClassName = aConfig.filter;
        if (filterClassName.isPresent()) {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> c = loader.loadClass(filterClassName.get());
                return (AsyncApiFilter) c.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                LOGGER.log(Level.SEVERE, "Filter-class " + filterClassName + " not found", ex);
            }
        }
        return null;
    }
}
