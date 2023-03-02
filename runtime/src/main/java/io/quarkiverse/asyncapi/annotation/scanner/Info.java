package io.quarkiverse.asyncapi.annotation.scanner;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.smallrye.config.WithDefault;

/**
 * @since 02.03.2023
 */
@ConfigGroup
public interface Info {

    /**
     * Info-Title
     */
    @WithDefault(value = "AsyncApi")
    String title();

    /**
     * Project-version
     */
    @WithDefault(value = "1.0.0")
    String version();

    /**
     * Project-description
     */
    Optional<String> description();

    /**
     * Contact infos
     */
    //        @ConfigDocSection
    Optional<AsyncApiRuntimeConfig.Contact> contact();

    /**
     * License infos
     *
     * @return License
     */
    //        @ConfigDocSection
    Optional<AsyncApiRuntimeConfig.License> license();

}
