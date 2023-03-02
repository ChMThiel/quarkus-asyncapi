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
     * Version of the WebComponent to be used in html-view to be found at [HOST]/asyncapi.html
     *
     * @see https://www.npmjs.com/package/@asyncapi/react-component
     */
    @WithDefault("1.0.0-next.47")
    String webcomponentversion();

    /**
     * Full qualified name of the implementing AsyncApiFilter
     *
     * @see AAFilter
     */
    Optional<String> filter();

    /**
     * AsyncApi specification version
     */
    @WithDefault("2.6.0")
    String version();

    /**
     * Default ContentType
     */
    @WithDefault("application/json")
    String defaultContentType();

    //    Map<String, Server> servers();
    /**
     * Info-section of AysncApi
     */
    //    @ConfigDocSection
    Info info();

    /**
     * Contact section
     */
    @ConfigGroup
    interface Contact {

        /**
         * Contact-Name
         */
        Optional<String> name();

        /**
         * Contact-Email
         *
         * @asciidoclet
         */
        @WithDefault("you@mail.org")
        Optional<String> email();

        /**
         * Contact-URL
         */
        Optional<String> url();
    }

    /**
     * License section
     */
    @ConfigGroup
    interface License {

        /**
         * License-Name
         */
        @WithDefault("Commercial")
        String name();

        /**
         * License-URL
         */
        Optional<String> url();
    }
}
