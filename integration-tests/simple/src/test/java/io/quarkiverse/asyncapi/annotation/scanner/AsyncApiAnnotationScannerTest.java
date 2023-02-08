package io.quarkiverse.asyncapi.annotation.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.JandexReflection;
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
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

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
                switch (genericMessageType.kind()) {
                    case CLASS:
                        messageType = genericMessageType.asClassType();
                        break;
                    case PARAMETERIZED_TYPE:
                        messageType = genericMessageType.asParameterizedType();
                        break;
                    default:
                        throw new IllegalArgumentException("unhandled messageType " + genericMessageType.kind());
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
                .payload(getSchema(aMessageType, aJandex, new HashMap<>()))
                .build();
    }

    Schema getSchema(Type aType, Index aJandex, Map<String, Type> typeVariableMap) {
        Schema.SchemaBuilder schemaBuilder = Schema.builder();
        if (aType.kind().equals(Type.Kind.PRIMITIVE)) {
            switch (aType.asPrimitiveType().primitive()) {
                case BOOLEAN:
                    schemaBuilder.type(com.asyncapi.v2.model.schema.Type.BOOLEAN);
                    break;
                case CHAR:
                    schemaBuilder.type(com.asyncapi.v2.model.schema.Type.STRING);
                    break;
                case INT:
                case LONG:
                case SHORT:
                    schemaBuilder.type(com.asyncapi.v2.model.schema.Type.INTEGER);
                    break;
                case DOUBLE:
                case FLOAT:
                    schemaBuilder.type(com.asyncapi.v2.model.schema.Type.NUMBER);
                    break;
                default:
                    schemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
            }
        } else if (aType.kind().equals(Type.Kind.ARRAY)) {
            Type arrayType = aType.asArrayType().component().kind().equals(Type.Kind.TYPE_VARIABLE)
                    ? typeVariableMap.get(aType.asArrayType().component().toString())
                    : aType.asArrayType().component();
            schemaBuilder
                    .type(com.asyncapi.v2.model.schema.Type.ARRAY)
                    .items(getSchema(arrayType, aJandex, typeVariableMap));
        } else if (aType.kind().equals(Type.Kind.CLASS) || aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)) {
            if (aType.name().packagePrefix().startsWith("java.lang")) {
                switch (aType.name().withoutPackagePrefix()) {
                    case "Boolean":
                        schemaBuilder.type(com.asyncapi.v2.model.schema.Type.BOOLEAN);
                        break;
                    case "Character":
                    case "String":
                        schemaBuilder.type(com.asyncapi.v2.model.schema.Type.STRING);
                        break;
                    case "Integer":
                    case "Long":
                    case "Short":
                        schemaBuilder.type(com.asyncapi.v2.model.schema.Type.INTEGER);
                        break;
                    case "Double":
                    case "Float":
                        schemaBuilder.type(com.asyncapi.v2.model.schema.Type.NUMBER);
                        break;
                    default:
                        schemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
                }
            } else {
                schemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
                ClassInfo classByName = aJandex.getClassByName(aType.name());
                if (classByName != null) {
                    if (aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)) {
                        for (int i = 0; i < aType.asParameterizedType().arguments().size(); i++) {
                            typeVariableMap.put(
                                    classByName.typeParameters().get(i).identifier(),
                                    aType.asParameterizedType().arguments().get(i));
                        }
                    }
                    List<FieldInfo> allFieldsRecursiv = getAllFieldsRecursiv(classByName, aJandex, typeVariableMap);
                    Map<String, Schema> properties = allFieldsRecursiv //TODO consider getter-annotations, too
                            .stream()
                            .collect(Collectors.toMap(
                                    FieldInfo::name,
                                    f -> getSchema(typeVariableMap.getOrDefault(f.type().toString(), f.type()), aJandex,
                                            typeVariableMap)));
                    schemaBuilder.properties(properties);
                } else {
                    //class is not in jandex...try to get the class by reflection
                    Class<?> rawClass = JandexReflection.loadRawType(aType);
                    if (Collection.class.isAssignableFrom(rawClass)) {
                        Type collectionType = aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)
                                ? typeVariableMap.getOrDefault(
                                        aType.asParameterizedType().arguments().get(0).toString(),
                                        aType.asParameterizedType().arguments().get(0))
                                : aType;
                        schemaBuilder
                                .type(com.asyncapi.v2.model.schema.Type.ARRAY)
                                .items(getSchema(collectionType, aJandex, typeVariableMap));
                    }
                    //TODO other non-indexed tyes from jre, e.g. maps...
                }
            }
        } else {
            //TODO other types
            schemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
        }
        return schemaBuilder
                .description("TODO read field-description by annotation")
                .build();
    }

    List<FieldInfo> getAllFieldsRecursiv(ClassInfo aClassInfo, Index aJandex, Map<String, Type> aTypeVariableMap) {
        ArrayList<FieldInfo> fieldInfos = new ArrayList<>();
        if (aClassInfo.fields() != null) {
            aClassInfo.fields().stream()
                    .filter(f -> !Modifier.isStatic(f.flags()))
                    .filter(Predicate.not(FieldInfo::isEnumConstant))
                    .forEach(fieldInfos::add);
        }
        ClassInfo superClass = aJandex.getClassByName(aClassInfo.superName());
        if (superClass != null) {
            fieldInfos.addAll(getAllFieldsRecursiv(superClass, aJandex, aTypeVariableMap));
        }
        return fieldInfos;
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
