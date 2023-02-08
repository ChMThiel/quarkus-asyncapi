package io.quarkiverse.asyncapi.annotation.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;

import com.asyncapi.v2.model.AsyncAPI;
import com.asyncapi.v2.model.channel.ChannelItem;
import com.asyncapi.v2.model.channel.message.Message;
import com.asyncapi.v2.model.channel.operation.Operation;
import com.asyncapi.v2.model.info.Contact;
import com.asyncapi.v2.model.info.Info;
import com.asyncapi.v2.model.info.License;
import com.asyncapi.v2.model.schema.Schema;

import io.quarkiverse.asyncapi.config.ObjectMapperFactory;
import io.quarkus.test.junit.QuarkusTest;
import java.util.HashMap;
import org.jboss.jandex.DotName;

@QuarkusTest
public class AsyncApiAnnotationScannerTest {

    @ConfigProperty(name = "mp.messaging.outgoing.channel-x.topic")
    String topic;

    @Test
    void shouldScanEmitterAnnotations() throws Exception {
        //given
        assertThat(topic).isEqualTo("topic-x");
        //when
        Index index = readIndex();
        AsyncAPI result = scan(index);
        //then
        String openApiYaml = ObjectMapperFactory.yaml().writeValueAsString(result);
        System.out.println(openApiYaml);
    }

    AsyncAPI scan(Index aJandex) {
        Map<String, ChannelItem> channels = aJandex.getAnnotations(Channel.class).stream()
                .map(annotation -> getChannelItem(annotation, aJandex))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return AsyncAPI.builder()
                .asyncapi("2.6.0")
                .id("http://id/todo") //has to be an uri!
                .info(Info.builder() //TODO implement Annotation to define it (use OpenApi???)...or use application.properties
                        .title("Smart Manufacturing Operation Management Service API")
                        .version("111")
                        .description(
                                "Smart Manufacturing Operation Management Service provides Kafka-Topics to send/receive data")
                        .license(License.builder()
                                .name("Commercial")
                                .url("https://gec.io/")
                                .build())
                        .contact(Contact.builder()
                                .name("Contact and Support")
                                .url("https://gec.io/kontakt/")
                                .email("support@gec.io")
                                .build())
                        .build())
                .defaultContentType("application/json")
                .channels(channels)
                .build();
    }

    AbstractMap.SimpleEntry<String, ChannelItem> getChannelItem(AnnotationInstance aAnnotationInstance, Index aJandex) {
        ChannelItem.ChannelItemBuilder channelBuilder = ChannelItem.builder()
                .description("TODO read channel-description by annotation");
        boolean isEmitter;
        Type messageType;
        String operationId;
        switch (aAnnotationInstance.target().kind()) {
            case FIELD:
                FieldInfo field = aAnnotationInstance.target().asField();
                operationId = field.declaringClass().name() + "." + field.name();
                Type annotationTargetType = field.type();
                isEmitter = annotationTargetType.name().toString().contains("Emitter");
            Type genericMessageType = annotationTargetType.asParameterizedType().arguments().get(0);
                switch(genericMessageType.kind()) {
                    case CLASS:
                        messageType = genericMessageType.asClassType();
                        break;
                    case PARAMETERIZED_TYPE:
                        messageType = genericMessageType.asParameterizedType();
                        break;
                        default:
                            throw new IllegalArgumentException("unhandled messageType "+genericMessageType.kind());
                }
                break;
            //TODO other annotation-targets
            default:
                operationId = UUID.randomUUID().toString();
                isEmitter = false;
                messageType = null;
        }
        Operation operation = Operation.builder()
                .description("TODO read operation-description by annotation")
                .message(getMessage(messageType, aJandex))
                .operationId(operationId)
                .build();
        ChannelItem channelItem = isEmitter
                ? channelBuilder.publish(operation).build()
                : channelBuilder.subscribe(operation).build();
        String configKey = "mp.messaging." + (isEmitter ? "outgoing" : "incomming") + "."
                + aAnnotationInstance.value().asString() + ".topic";
        String topic = ConfigProvider.getConfig()
                .getOptionalValue(configKey, String.class)
                .orElse(aAnnotationInstance.value().asString());
        return new AbstractMap.SimpleEntry<>(topic, channelItem);
    }

    Message getMessage(Type aMessageType, Index aJandex) {
        return Message.builder()
                .name(aMessageType.name().toString()) //TODO expect to be overriden by annotation
                .summary("TODO messageSummary by annotation")
                .description("TODO messageDescription by annotation")
                .contentType("application/json")
                .payload(getSchema(aMessageType, aJandex))
                .build();
    }

    Schema getSchema(Type aType, Index aJandex) {
        Schema.SchemaBuilder schemaBuilder = Schema.builder();
        String type;
        if (aType.kind().equals(Type.Kind.PRIMITIVE)) {
            switch (aType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                    type = com.asyncapi.v2.model.schema.Type.BOOLEAN;
                    break;
                case CHAR:
                    type = com.asyncapi.v2.model.schema.Type.STRING;
                    break;
                case INT:
                case LONG:
                case SHORT:
                    type = com.asyncapi.v2.model.schema.Type.INTEGER;
                    break;
                case DOUBLE:
                case FLOAT:
                    type = com.asyncapi.v2.model.schema.Type.NUMBER;
                    break;
                default:
                    type = com.asyncapi.v2.model.schema.Type.OBJECT;
            }
        } else if (aType.kind().equals(Type.Kind.CLASS) || aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE))  {
            if (aType.name().packagePrefix().startsWith("java.lang")) {
                switch (aType.name().withoutPackagePrefix()) {
                    case "Boolean":
                        type = com.asyncapi.v2.model.schema.Type.BOOLEAN;
                        break;
                    case "Character":
                    case "String":
                        type = com.asyncapi.v2.model.schema.Type.STRING;
                        break;
                    case "Integer":
                    case "Long":
                    case "Short":
                        type = com.asyncapi.v2.model.schema.Type.INTEGER;
                        break;
                    case "Double":
                    case "Float":
                        type = com.asyncapi.v2.model.schema.Type.NUMBER;
                        break;
                    default:
                        type = com.asyncapi.v2.model.schema.Type.OBJECT;
                }
            } else {
                type = com.asyncapi.v2.model.schema.Type.OBJECT;
                ClassInfo classByName = aJandex.getClassByName(aType.name());
                Map<String, Type> typeVariableMap = new HashMap<>();
                if(aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)) {
                    for(int i = 0; i < aType.asParameterizedType().arguments().size(); i++) {
                        typeVariableMap.put(
                                classByName.typeParameters().get(i).identifier(),
                                aType.asParameterizedType().arguments().get(i));
                    }
                }
                Map<String, Schema> properties = classByName
                        .fields() //TODO consider getter-annotations, too; Recursive??
                        .stream()
                        .collect(Collectors.toMap(FieldInfo::name, f -> getSchema(getType(f, typeVariableMap), aJandex)));
                schemaBuilder.properties(properties);
            }
        } else {
            //TODO other types
            type = com.asyncapi.v2.model.schema.Type.OBJECT;
        }
        return schemaBuilder
                .description("TODO read field-description by annotation")
                .type(type)
                .build();
    }

    Type getType(FieldInfo aF, Map<String, Type> typeVariableMap) {
        return typeVariableMap.getOrDefault(aF.type().toString(), aF.type());
    }
    
    //TODO how to get index during runtime...
    private Index readIndex() throws FileNotFoundException, IOException {
        File file = new File("target/classes/META-INF/jandex.idx");
        try (FileInputStream input = new FileInputStream(file)) {
            IndexReader reader = new IndexReader(input);
            return reader.read();
        }
    }
}
