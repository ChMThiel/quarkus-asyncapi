package io.quarkiverse.asyncapi.annotation.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(UnFilteredProfile.class)
public class AsyncApiAnnotationScannerUnFilteredTest {

    @Test
    void shouldScanAndFilterEmitterAnnotations() throws Exception {
        //given
        String yaml = Files
                .readString(Paths.get(getClass().getClassLoader().getResource("META-INF/resources/asyncApi.yaml").toURI()));
        assertThat(yaml).isNotNull();
        JsonNode asyncAPI = ObjectMapperFactory.yaml().readTree(yaml);
        //when
        assertThat(asyncAPI.at("/channels")).isInstanceOf(ObjectNode.class);
        assertThat(asyncAPI.at("/channels")).hasSizeGreaterThanOrEqualTo(6);
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload")).hasSize(3);
        assertThat(
                asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties"))
                .hasSizeGreaterThan(3);
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties"))
                .hasSize(7);
    }
}
