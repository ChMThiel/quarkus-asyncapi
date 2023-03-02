package io.quarkiverse.asyncapi.annotation.scanner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    static final String FOLDER = "target/classes/META-INF/resources";
    private static final Logger LOGGER = Logger.getLogger(AsyncApiRecorder.class.getName());

    public void setAsyncAPI(AsyncAPI aAsyncAPI, AsyncApiRuntimeConfig aConfig) {
        try {
            AsyncAPI filteredAPI = filter(aAsyncAPI, aConfig);
            store(ObjectMapperFactory.yaml().writeValueAsString(filteredAPI), "yaml");
            store(ObjectMapperFactory.json().writeValueAsString(filteredAPI), "json");
        } catch (JsonProcessingException e) {
            LOGGER.throwing("io.quarkiverse.asyncapi.annotation.scanner.AsyncApiRecorder", "scanAsyncAPIs", e);
        }
    }

    void store(String aContent, String aFileSuffix) {
        try {
            Path dir = Path.of(FOLDER);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = Path.of(FOLDER + "/asyncApi." + aFileSuffix);
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
            Files.writeString(file, aContent, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.throwing("io.quarkiverse.asyncapi.annotation.scanner.AsyncApiRecorder", "store", e);
        }
    }

    AsyncAPI filter(AsyncAPI aAsyncAPI, AsyncApiRuntimeConfig aConfig) {
        AsyncAPI result = aAsyncAPI;
        AAFilter filter = getFilter(aConfig);
        if (filter != null) {
            result = filter.filterAsyncAPI(aAsyncAPI);
            Map<String, ChannelItem> filteredChannels = result.getChannels().entrySet().stream()
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), filter.filterChannelItem(e.getKey(), e.getValue())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            result.setChannels(filteredChannels);
        }
        return result;
    }

    AAFilter getFilter(AsyncApiRuntimeConfig aConfig) {
        Optional<String> filterClassName = aConfig.filter;
        if (filterClassName.isPresent()) {
            try {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class<?> c = loader.loadClass(filterClassName.get());
                return (AAFilter) c.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
                LOGGER.log(Level.SEVERE, "Filter-class " + filterClassName + " not found", ex);
            }
        }
        return null;
    }
}
