package io.quarkiverse.asyncapi.annotation.scanner;

import com.asyncapi.v2.binding.kafka.KafkaChannelBinding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO replace with KafkaChannelBinding as soon the topic/descriptions are added there
 * @author christiant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyKafkaChannelBinding extends KafkaChannelBinding {

    private String topic;
    private String description;
}
