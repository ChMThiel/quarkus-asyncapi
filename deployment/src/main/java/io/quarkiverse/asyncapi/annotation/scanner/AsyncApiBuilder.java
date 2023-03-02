package io.quarkiverse.asyncapi.annotation.scanner;

import java.util.Map;

import org.jboss.jandex.IndexView;

import com.asyncapi.v2.model.AsyncAPI;
import com.asyncapi.v2.model.server.Server;

import io.quarkiverse.asyncapi.annotation.scanner.config.AsyncApiRuntimeConfig;

/**
 * @since 02.03.2023
 */
public class AsyncApiBuilder {

    final AsyncApiRuntimeConfig config;
    final IndexView index;

    public AsyncApiBuilder(IndexView aIndex, AsyncApiRuntimeConfig aConfig) {
        index = aIndex;
        config = aConfig;
    }

    AsyncAPI build() {
        ConfigResolver configResolver = new ConfigResolver(config);
        AsyncApiAnnotationScanner scanner = new AsyncApiAnnotationScanner(index, configResolver);
        AsyncAPI.AsyncAPIBuilder builder = AsyncAPI.builder()
                .asyncapi(config.version)
                //                id: 'https://github.com/smartylighting/streetlights-server'
                .id(configResolver.getConfiguredKafkaBootstrapServer())
                .info(configResolver.getInfo())
                .defaultContentType(config.defaultContentType)
                .channels(scanner.getChannels())
                .components(scanner.getGlobalComponents());
        Map<String, Server> servers = configResolver.getServers();
        if (servers != null) {
            builder.servers(servers);
        }
        return builder
                .build();
    }
}
