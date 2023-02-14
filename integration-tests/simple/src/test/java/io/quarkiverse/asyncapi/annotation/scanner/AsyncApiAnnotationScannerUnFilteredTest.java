package io.quarkiverse.asyncapi.annotation.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.asyncapi.v2.model.AsyncAPI;
import com.asyncapi.v2.model.channel.message.Message;
import com.asyncapi.v2.model.schema.Schema;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(UnFilteredProfile.class)
public class AsyncApiAnnotationScannerUnFilteredTest {

    @Test
    void shouldScanAndFilterEmitterAnnotations() throws Exception {
        //given
        String yaml = Files.readAllLines(AsyncApiRecorder.FILE).stream().collect(Collectors.joining("\n"));
        assertThat(yaml).isNotNull();
        AsyncAPI asyncAPI = ObjectMapperFactory.yaml().readValue(yaml, AsyncAPI.class);
        //when
        assertThat(asyncAPI.getChannels()).hasSize(4);
        Message message = (Message) asyncAPI.getChannels().get("transfer-channel1").getPublish().getMessage();
        Schema payload = (Schema) message.getPayload();
        assertThat(payload.getProperties()).hasSize(10);
        assertThat(payload.getProperties().get("value").getProperties()).hasSize(3);
        assertThat(payload.getProperties().get("value").getProperties().get("part").getProperties()).hasSizeGreaterThan(3);
        assertThat(payload.getProperties().get("value").getProperties().get("company").getProperties()).hasSize(7);
    }
}
