package io.quarkiverse.asyncapi.annotation.scanner.kafka.binding;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * @since 02.03.2023
 */
public class KafkaResolver {

    public KafkaChannelBinding getKafkaChannelBindings(String aTopic) {
        KafkaChannelBinding.KafkaChannelBindingBuilder builder = KafkaChannelBinding.builder()
                .topic(aTopic);
        Optional<String> bootStrapServers = ConfigProvider.getConfig()
                .getOptionalValue("kafka.bootstrap.servers", String.class);
        if (bootStrapServers.isPresent()) {
            Map<String, Object> properties = Map.of("bootstrap.servers", bootStrapServers.get());
            try (AdminClient client = AdminClient.create(properties)) {
                System.out.println("Topic:" + aTopic);
                DescribeTopicsResult topicDescription = client.describeTopics(
                        Set.of(aTopic), new DescribeTopicsOptions().timeoutMs(1000));
                List<TopicPartitionInfo> partitionInfos = topicDescription.topicNameValues().get(aTopic).get().partitions();
                builder.partitions(partitionInfos.size())
                        .replicas(partitionInfos.get(0).replicas().size())
                        .topicConfiguration(getTopicConfiguration(client, aTopic));
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(KafkaResolver.class.getName()).log(Level.SEVERE, "kafka not available", ex);
            }
        } else {
            Logger.getLogger(KafkaResolver.class.getName()).log(Level.WARNING, "No kafka.bootstrap.server configured");
        }
        return builder.build();
    }

    TopicConfiguration getTopicConfiguration(AdminClient aClient, String aTopic) {
        Map<String, ConfigEntry> configMap = aClient
                .describeConfigs(List.of(new ConfigResource(ConfigResource.Type.TOPIC, aTopic)))
                .values().values().stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException interruptedException) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(Config::entries)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(ConfigEntry::name, Function.identity()));
        return TopicConfiguration.builder()
                .cleanupPolicy(configMap.get(TopicConfiguration.CLEANUP_POLICY).value())
                .retentionMs(Long.valueOf(configMap.get(TopicConfiguration.RETENTION_MS).value()))
                .retentionBytes(Long.valueOf(configMap.get(TopicConfiguration.RETENTION_BYTES).value()))
                .deleteRetentionMs(Long.valueOf(configMap.get(TopicConfiguration.DELETE_RETENTION_MS).value()))
                .maxMessageBytes(Long.valueOf(configMap.get(TopicConfiguration.MAX_MESSAGE_BYTES).value()))
                .build();
    }

}
