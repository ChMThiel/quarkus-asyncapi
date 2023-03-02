package io.quarkiverse.asyncapi.annotation.scanner.config;

import java.util.Optional;

import io.quarkiverse.asyncapi.annotation.scanner.AAFilter;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

//@ConfigMapping(prefix = "quarkus.asyncapi.annotation.scanner")
@ConfigRoot(name = "asyncapi.annotation.scanner", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AsyncApiRuntimeConfig {

    /**
     * Version of the WebComponent to be used in html-view to be found at [HOST]/asyncapi.html
     *
     * @see https://www.npmjs.com/package/@asyncapi/react-component
     */
    @ConfigItem(defaultValue = "1.0.0-next.47")
    public String webcomponentversion;

    /**
     * Full qualified name of the implementing AsyncApiFilter
     *
     * @see AAFilter
     */
    @ConfigItem
    public Optional<String> filter = Optional.empty();

    /**
     * AsyncApi specification version
     */
    @ConfigItem(defaultValue = "2.6.0")
    public String version;

    /**
     * Default ContentType
     */
    @ConfigItem(defaultValue = "application/json")
    public String defaultContentType;

    //    Map<String, Server> servers();
    /**
     * Title
     */
    @ConfigItem(name = "info.title", defaultValue = "AsyncApi")
    public Optional<String> infoTitle = Optional.empty();
    /**
     * Project-version
     */
    @ConfigItem(name = "info.version", defaultValue = "1.0.0")
    public String infoVersion;
    /**
     * Project-description
     */
    @ConfigItem(name = "info.description")
    public Optional<String> infoDescription = Optional.empty();

    /**
     * Contact-Name
     */
    @ConfigItem(name = "info.contact.name")
    public Optional<String> infoContactName = Optional.empty();
    /**
     * Contact-Email
     */
    @ConfigItem(name = "info.contact.email", defaultValue = "you@mail.org")
    public Optional<String> infoContactEmail = Optional.empty();
    /**
     * Contact-URL
     */
    @ConfigItem(name = "info.contact.url")
    public Optional<String> infoContactUrl = Optional.empty();
    /**
     * License infos
     *
     * @return License
     */
    //        @ConfigDocSection
    /**
     * License-Name
     */
    @ConfigItem(name = "info.license.name", defaultValue = "Commercial")
    public Optional<String> infoLicensName = Optional.empty();
    /**
     * License-URL
     */
    @ConfigItem(name = "info.license.url")
    public Optional<String> infoLicenseUrl = Optional.empty();
}
