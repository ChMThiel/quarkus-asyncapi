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
    void shouldScanAndFilterEmitterAnnotations_CheckOutGoingTransferMessage() throws Exception {
        //given
        String yaml = Files.readAllLines(Path.of(FOLDER + "/asyncApi.yaml")).stream().collect(Collectors.joining("\n"));
        assertThat(yaml).isNotNull();
        System.out.println(yaml);
        JsonNode asyncAPI = ObjectMapperFactory.yaml().readTree(yaml);
        //when
        assertThat(asyncAPI.at("/channels")).isInstanceOf(ObjectNode.class);
        assertThat(asyncAPI.at("/channels")).hasSizeGreaterThanOrEqualTo(5);
        assertThat(asyncAPI.at("/channels/prices").isMissingNode()).isFalse();
        assertThat(asyncAPI.at("/channels/prices-intern").isMissingNode()).as("intern channels should be ignored").isTrue();
        //TransferMessage
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/contentType").asText())
                .isEqualTo("application/json");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/name").asText())
                .isEqualTo("io.quarkiverse.asyncapi.annotation.scanner.TransferMessage");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload")).hasSize(3);
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/description").asText())
                .isEqualTo("TransferMessage description");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/type").asText()).isEqualTo("object");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties")).hasSize(10);
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/action/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/description/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/kafkaKey/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/trigger/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/type/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/user/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/maxBookDate/$ref").asText())
                .isEqualTo("#/components/schemas/OffsetDateTime");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/minBookDate/$ref").asText())
                .isEqualTo("#/components/schemas/OffsetDateTime");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/plantId/$ref").asText())
                .isEqualTo("#/components/schemas/UUID");
        //
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/value/type").asText())
                .isEqualTo("object");
        assertThat(asyncAPI.at("/channels/transfer-channel1/publish/message/payload/properties/value/properties")).hasSize(3);

        //BookDate Only Ref available
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/bookDate/$ref").asText())
                .isEqualTo("#/components/schemas/OffsetDateTime");
        //Part
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/description")
                .asText())
                .isEqualTo("Part definition. Part may be a product, a raw material, an equipment or any other part");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/type").asText())
                .isEqualTo("object");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties"))
                .hasSize(3);
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties/partTypeId/$ref")
                .asText())
                .isEqualTo("#/components/schemas/UUID");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties/partTypeId/description")
                .asText())
                .isEqualTo("reference to parttype of the part");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties/partGroupId/$ref")
                .asText())
                .isEqualTo("#/components/schemas/UUID");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties/partGroupId/description")
                .asText())
                .isEqualTo("reference to partgroup of the part");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/part/properties/externalId/type")
                .asText())
                .isEqualTo("string");
        //Company
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/description")
                .asText())
                .isEqualTo("Company");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/type").asText())
                .isEqualTo("object");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties"))
                .hasSize(7);
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/payload/type")
                .asText())
                .isEqualTo("string");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/name/type")
                .asText())
                .isEqualTo("string");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/tenantId/description")
                .asText())
                .isEqualTo("Id references the Tenant");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/description/type")
                .asText())
                .isEqualTo("string");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/info1/type")
                .asText())
                .isEqualTo("string");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/info2/type")
                .asText())
                .isEqualTo("string");
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/publish/message/payload/properties/value/properties/company/properties/info3/type")
                .asText())
                .isEqualTo("string");
        //Servers
        assertThat(asyncAPI
                .at("/servers/testServer/protocol").asText())
                .isEqualTo("kafka");
        //MyKafkaChannelBinding
        assertThat(asyncAPI
                .at("/channels/transfer-channel1/description").asText())
                .isEqualTo("description of the transfer-channel1 from application.properties");
        assertThat(asyncAPI
                .at("/channels/transfer-channel2/description").asText())
                .isEqualTo("description of the transfer-channel2 from application.properties");
    }

    @Test
    void shouldScanAndFilterEmitterAnnotations_CheckIncomingGecMessage() throws Exception {
        //given
        String yaml = Files.readAllLines(Path.of(FOLDER + "/asyncApi.yaml")).stream().collect(Collectors.joining("\n"));
        assertThat(yaml).isNotNull();
        System.out.println(yaml);
        JsonNode asyncAPI = ObjectMapperFactory.yaml().readTree(yaml);
        //when
        assertThat(asyncAPI.at("/channels")).isInstanceOf(ObjectNode.class);
        assertThat(asyncAPI.at("/channels")).hasSizeGreaterThanOrEqualTo(5);
        //GecMessage
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/contentType").asText())
                .isEqualTo("application/json");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/name").asText())
                .isEqualTo("io.quarkiverse.asyncapi.annotation.scanner.GecMessage");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload")).hasSize(3);
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/description").asText())
                .isEqualTo("GecMessage description");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/type").asText()).isEqualTo("object");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties")).hasSize(5);
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties/action/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties/kafkaKey/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties/user/type").asText())
                .isEqualTo("string");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties/plantId/$ref").asText())
                .isEqualTo("#/components/schemas/UUID");
        //Part
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties/value/type").asText())
                .isEqualTo("object");
        assertThat(asyncAPI.at("/channels/incoming-channel-part/subscribe/message/payload/properties/value/properties"))
                .hasSize(10);
        assertThat(asyncAPI
                .at("/channels/incoming-channel-part/subscribe/message/payload/properties/value/description")
                .asText()).isEqualTo("Part definition. Part may be a product, a raw material, an equipment or any other part");
        assertThat(asyncAPI.at(
                "/channels/incoming-channel-part/subscribe/message/payload/properties/value/properties/partTypeId/$ref")
                .asText()).isEqualTo("#/components/schemas/UUID");
        assertThat(asyncAPI.at(
                "/channels/incoming-channel-part/subscribe/message/payload/properties/value/properties/partTypeId/description")
                .asText()).isEqualTo("reference to parttype of the part");
        assertThat(asyncAPI.at(
                "/channels/incoming-channel-part/subscribe/message/payload/properties/value/properties/partGroupId/$ref")
                .asText()).isEqualTo("#/components/schemas/UUID");
        assertThat(asyncAPI.at(
                "/channels/incoming-channel-part/subscribe/message/payload/properties/value/properties/partGroupId/description")
                .asText()).isEqualTo("reference to partgroup of the part");
        assertThat(asyncAPI.at(
                "/channels/incoming-channel-part/subscribe/message/payload/properties/value/properties/externalId/type")
                .asText()).isEqualTo("string");
    }
}
