package io.quarkiverse.asyncapi.annotation.scanner;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.asyncapi.annotation.scanner")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface AsyncApiRuntimeConfig {

    /**
     * full qualified name of the implementing AsyncApiFilter
     *
     * @see AAFilter
     */
    Optional<String> filter();

    //TODO provide configs for all fixed parameters
    /**
     * Info-section of AysnApi
     */
    InfoConfig info();

    @ConfigGroup
    interface InfoConfig {

        /**
         * Info-Title
         */
        @WithDefault("Title")
        String title();
    }
}
