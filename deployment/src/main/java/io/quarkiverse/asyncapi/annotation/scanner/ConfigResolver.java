package io.quarkiverse.asyncapi.annotation.scanner;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;

import com.asyncapi.v2.model.info.Contact;
import com.asyncapi.v2.model.info.Info;
import com.asyncapi.v2.model.info.License;
import com.asyncapi.v2.model.server.Server;

import io.quarkiverse.asyncapi.annotation.scanner.config.AsyncApiRuntimeConfig;
import io.quarkiverse.asyncapi.annotation.scanner.config.Channel;

/**
 * @since 02.03.2023
 */
public class ConfigResolver {

    final AsyncApiRuntimeConfig config;

    public ConfigResolver(AsyncApiRuntimeConfig aConfig) {
        this.config = aConfig;
    }

    public Info getInfo() {
        Info.InfoBuilder infoBuilder = Info.builder() //TODO implement Annotation to define it (use OpenApi???)
                .version(config.infoVersion);
        config.infoTitle.ifPresent(infoBuilder::title);
        config.infoDescription.ifPresent(infoBuilder::description);
        License.LicenseBuilder licenseBuilder = License.builder();
        if (config.infoLicensName.isPresent() || config.infoLicenseUrl.isPresent()) {
            config.infoLicensName.ifPresent(licenseBuilder::name);
            config.infoLicenseUrl.ifPresent(licenseBuilder::url);
            infoBuilder.license(licenseBuilder.build());
        }
        if (config.infoContactEmail.isPresent() || config.infoContactEmail.isPresent() || config.infoContactUrl.isPresent()) {
            Contact.ContactBuilder contactBuilder = Contact.builder();
            config.infoContactName.ifPresent(contactBuilder::name);
            config.infoContactEmail.ifPresent(contactBuilder::email);
            config.infoContactUrl.ifPresent(contactBuilder::url);
            infoBuilder.contact(contactBuilder.build());
        }
        return infoBuilder.build();
    }

    public String getConfiguredKafkaBootstrapServer() {
        return ConfigProvider.getConfig()
                .getOptionalValue("kafka.bootstrap.servers", String.class)
                .orElse("urn:com:kafka:server");
    }

    public String getTopic(boolean aIsEmitter, String aChannel) {
        String configKey = "mp.messaging." + (aIsEmitter ? "outgoing" : "incoming") + "." + aChannel + ".topic";
        return ConfigProvider.getConfig().getOptionalValue(configKey, String.class).orElse(aChannel);
    }

    public Channel getChannel(String aChannel) {
        return config.channels.get(aChannel);
    }

    Map<String, Server> getServers() {
        if (config.servers.isEmpty()) {
            return null;
        }
        return config.servers.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> toOpenApiServer(e.getValue())));
    }

    Server toOpenApiServer(io.quarkiverse.asyncapi.annotation.scanner.config.Server aConfigServer) {
        return Server.builder()
                //TODO
                .protocol(aConfigServer.protocol)
                .url(aConfigServer.url)
                .build();
    }
}
