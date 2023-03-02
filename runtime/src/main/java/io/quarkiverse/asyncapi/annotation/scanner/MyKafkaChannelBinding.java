package io.quarkiverse.asyncapi.annotation.scanner;

import com.asyncapi.v2.binding.kafka.KafkaChannelBinding;

/**
 * TODO replace with KafkaChannelBinding as soon the topic/descriptions are added there
 *
 * @author christiant
 */
public class MyKafkaChannelBinding extends KafkaChannelBinding {

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
