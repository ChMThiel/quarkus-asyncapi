package io.quarkiverse.asyncapi.annotation.scanner;

import static io.quarkiverse.asyncapi.annotation.scanner.AsyncApiRecorder.FOLDER;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AsyncApiAnnotationScannerFilteredTest {

    @Test
    void shouldScanAndFilterEmitterAnnotations() throws Exception {
        //given
        String yaml = Files.readAllLines(Path.of(FOLDER + "/asyncApi.yaml")).stream().collect(Collectors.joining("\n"));
        assertThat(yaml).isNotNull();
        //        System.out.println(yaml);
        JsonNode asyncAPI = ObjectMapperFactory.yaml().readTree(yaml);
        //when
        assertThat(asyncAPI.at("/channels")).isInstanceOf(ObjectNode.class);
        assertThat(asyncAPI.at("/channels")).hasSize(4);
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload")).hasSize(3);
        assertThat(
                asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties"))
                .hasSize(3);
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties"))
                .hasSize(7);
        assertThat(asyncAPI
                .at("/servers/testServer/protocol").asText())
                .isEqualTo("kafka");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/description").asText())
                .isEqualTo("description of the transfer-channel1 from application.properties");
    }
}
