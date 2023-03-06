package io.quarkiverse.asyncapi.annotation.scanner.kafka.binding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * TODO replace with com.asyncapi.v2.binding.kafka.KafkaChannelBinding as soon the topic/descriptions are added there
 *
 * @author christiant
 * @see https://github.com/asyncapi/bindings/blob/master/kafka/README.md#channel-binding-object
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@EqualsAndHashCode(callSuper = false)
public class KafkaChannelBinding extends com.asyncapi.v2.binding.kafka.KafkaChannelBinding {

    private String topic;
    private Integer partitions;
    private Integer replicas;
    private TopicConfiguration topicConfiguration;
    private final String bindingVersion = "0.4.0";
}
