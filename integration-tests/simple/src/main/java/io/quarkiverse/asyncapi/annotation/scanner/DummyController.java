package io.quarkiverse.asyncapi.annotation.scanner;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.reactivestreams.Publisher;

import io.smallrye.reactive.messaging.annotations.Broadcast;

/**
 * @since 07.02.2023
 */
@ApplicationScoped
public class DummyController {

    //no inject, there is no implementation for Emitter in classpath -> Quarkus won't start
    @Channel("channel-x")
    @Broadcast
    Emitter<TestMessage<TestMessageData>> emitter;
    //no inject, there is no implementation for Publisher in classpath -> Quarkus won't start
    @Channel("channel-y")
    Publisher<TestMessage<TestMessageData>> publisher;
}
