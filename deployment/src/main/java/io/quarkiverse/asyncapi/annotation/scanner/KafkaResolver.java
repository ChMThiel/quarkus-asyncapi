package io.quarkiverse.asyncapi.annotation.scanner;

import io.quarkiverse.asyncapi.annotation.scanner.config.AsyncApiRuntimeConfig;

/**
 * @since 02.03.2023
 */
public class KafkaResolver {

    final AsyncApiRuntimeConfig config;

    public KafkaResolver(AsyncApiRuntimeConfig aConfig) {
        this.config = aConfig;
    }

    public MyKafkaChannelBinding getKafkaChannelBindings() {
        return null;
    }

}
