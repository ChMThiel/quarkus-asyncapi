package io.quarkiverse.asyncapi.annotation.scanner;

import com.asyncapi.v2.binding.kafka.KafkaChannelBinding;

/**
 * TODO replace with KafkaChannelBinding as soon the topic/descriptions are added there
 *
 * @author christiant
 * @see https://github.com/asyncapi/bindings/blob/master/kafka/README.md#channel-binding-object
 */
public class MyKafkaChannelBinding extends KafkaChannelBinding {

    //TODO read with org.apache.kafka.clients.admin.AdminClient;
    //    kafka:
    //        topic: 'my-specific-topic-name'
    //        partitions: 20
    //        replicas: 3
    //        topicConfiguration:
    //          cleanup.policy: ["delete", "compact"]
    //          retention.ms: 604800000
    //          retention.bytes: 1000000000
    //          delete.retention.ms: 86400000
    //          max.message.bytes: 1048588
    //        bindingVersion: '0.4.0'

    private String topic;

    public MyKafkaChannelBinding() {
        //default constructor for serialization
    }

    public MyKafkaChannelBinding(String aTopic) {
        topic = aTopic;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String aTopic) {
        this.topic = aTopic;
    }
}
