package io.quarkiverse.asyncapi.annotation.scanner;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = AsyncApiConfig.CONFIG_PREFIX, phase = ConfigPhase.RUN_TIME)
public class AsyncApiConfig {

    static final String CONFIG_PREFIX = "asyncapi.annotation.scanner";

    private static final String EXCLUDE = "exclude";
    private static final String PACKAGE = "package";

    public static final String EXCLUDED_FILES_PROP = getPropName(EXCLUDE);
    public static final String PACKAGE_PROP = getPropName(PACKAGE);

    private static final String getPropName(String suffix) {
        return "quarkus." + CONFIG_PREFIX + "." + suffix;
    }

    //    /**
    //     * List of files to be excluded
    //     */
    //    @ConfigItem(name = EXCLUDE)
    //    public Optional<List<String>> excluded;

}
