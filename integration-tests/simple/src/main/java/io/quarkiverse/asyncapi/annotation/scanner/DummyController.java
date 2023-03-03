package io.quarkiverse.asyncapi.annotation.scanner;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.reactivestreams.Publisher;

import io.smallrye.mutiny.Multi;
import io.smallrye.reactive.messaging.annotations.Broadcast;

/**
 * @since 07.02.2023
 */
@ApplicationScoped
public class DummyController {

    //no inject, there is no implementation for Emitter in classpath -> Quarkus won't start
    @Channel("channel-x")
    @Broadcast
    @Schema(description = "testMessage Emmiter description")
    Emitter<TestMessage<TestMessageData>> emitter;
    //no inject, there is no implementation for Publisher in classpath -> Quarkus won't start
    @Channel("channel-y")
    @Schema(description = "testMessage Publisher description")
    Publisher<TestMessage<TestMessageData>> publisher;
    //no inject, there is no implementation for Publisher in classpath -> Quarkus won't start

    @Channel("transfer-channel1")
    @Schema(description = "transferEmitter description1")
    Emitter<TransferMessage<TransferWorkorderMessage>> transferEmitter1;

    @Channel("transfer-channel2")
    @Schema(description = "transferEmitter description2")
    Emitter<TransferMessage<String>> transferEmitter2;

    @Incoming("incoming-channel-string")
//    @Implementation({ GecMessage.class, Part.class })
    public void receiveMessage(String aData) {
        //Do nothing
    }

    @Incoming("incoming-channel-part")
    public void receiveMessage(GecMessage<Part> aPart) {
        //Do nothing
    }

    @Outgoing("outgoing-channel-string")
    @Implementation({ GecMessage.class, List.class, Part.class }) //GecMessage<List<Part>>
    public String sendMessage() {
        //Do nothing
        return null;
    }

    @Outgoing("outgoing-channel-part")
    public GecMessage<Part> sendMessageTyped() {
        //Do nothing
        return null;
    }

    //Ignore internal channels that have no application.properties
    @Channel("prices-intern")
    Multi<Double> pricesIntern;

    //no inject, there is no implementation for Multi in classpath -> Quarkus won't start
    @Channel("prices")
    Multi<Double> prices;

}
