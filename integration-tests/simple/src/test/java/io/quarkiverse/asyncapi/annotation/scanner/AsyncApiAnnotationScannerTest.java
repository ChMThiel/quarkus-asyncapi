package io.quarkiverse.asyncapi.annotation.scanner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
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
import com.asyncapi.v2.model.component.Components;
import com.asyncapi.v2.model.info.Contact;
import com.asyncapi.v2.model.info.Info;
import com.asyncapi.v2.model.info.License;
import com.asyncapi.v2.model.schema.Schema;

import io.quarkiverse.asyncapi.config.ObjectMapperFactory;
import io.quarkus.test.junit.QuarkusTest;

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
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, TreeMap::new));
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
                .components(getGlobalComponents())
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

    Components getGlobalComponents() {
        return Components.builder()
                .schemas(Map.of(
                        "OffsetDateTime", Schema.builder()
                                .format("date-time")
                                .type(com.asyncapi.v2.model.schema.Type.STRING)
                                .examples(List.of("2022-03-10T12:15:50-04:00"))
                                .build(),
                        "UUID", Schema.builder()
                                .format("uuid")
                                .pattern("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")
                                .type(com.asyncapi.v2.model.schema.Type.STRING)
                                .build()))
                .build();
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
        switch (aType.kind()) {
            case PRIMITIVE:
                getPrimitiveSchema(aType, schemaBuilder);
                break;
            case ARRAY:
                getArraySchema(aType, typeVariableMap, schemaBuilder, aJandex);
                break;
            case CLASS:
            case PARAMETERIZED_TYPE:
                getClassSchema(aType, schemaBuilder, aJandex, typeVariableMap);
                break;
            default:
                //TODO other types
                schemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
                break;
        }
        return schemaBuilder
                .description("TODO read field-description by annotation")
                .build();
    }

    void getClassSchema(Type aType, Schema.SchemaBuilder aSchemaBuilder, Index aJandex, Map<String, Type> aTypeVariableMap) {
        ClassInfo classInfo = aJandex.getClassByName(aType.name());
        if (aType.name().packagePrefix().startsWith("java.lang")) {
            getJavaLangPackageSchema(aType, aSchemaBuilder);
        } else if (classInfo != null && classInfo.isEnum()) {
            aSchemaBuilder.enumValue(classInfo.enumConstants().stream().map(FieldInfo::name).map(Object.class::cast).toList());
        } else if (aType.name().equals(DotName.createSimple(OffsetDateTime.class))) {
            aSchemaBuilder.ref("#/components/schemas/OffsetDateTime");
        } else if (aType.name().equals(DotName.createSimple(UUID.class))) {
            aSchemaBuilder.ref("#/components/schemas/UUID");
        } else {
            aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
            if (classInfo != null) {
                if (aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)) {
                    for (int i = 0; i < aType.asParameterizedType().arguments().size(); i++) {
                        aTypeVariableMap.put(classInfo.typeParameters().get(i).identifier(),
                                aType.asParameterizedType().arguments().get(i));
                    }
                }
                //TODO consider getter-annotations, too
                List<FieldInfo> allFieldsRecursiv = getAllFieldsRecursiv(classInfo, aJandex, aTypeVariableMap);
                Map<String, Schema> properties = allFieldsRecursiv.stream()
                        .collect(Collectors.toMap(
                                FieldInfo::name,
                                f -> getSchema(aTypeVariableMap.getOrDefault(f.type().toString(), f.type()), aJandex,
                                        aTypeVariableMap),
                                (a, b) -> b,
                                TreeMap::new));
                aSchemaBuilder.properties(properties);
            } else {
                //class is not in jandex...try to get the class by reflection
                Class<?> rawClass = JandexReflection.loadRawType(aType);
                if (Collection.class.isAssignableFrom(rawClass)) {
                    Type collectionType = aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)
                            ? aTypeVariableMap.getOrDefault(aType.asParameterizedType().arguments().get(0).toString(),
                                    aType.asParameterizedType().arguments().get(0))
                            : aType;
                    aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.ARRAY)
                            .items(getSchema(collectionType, aJandex, aTypeVariableMap));
                }
                //TODO other non-indexed tyes from jre, e.g. maps...
            }
        }
    }

    void getJavaLangPackageSchema(Type aType, Schema.SchemaBuilder aSchemaBuilder) {
        switch (aType.name().withoutPackagePrefix()) {
            case "Boolean":
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.BOOLEAN);
                break;
            case "Character":
            case "String":
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.STRING);
                break;
            case "Integer":
            case "Long":
            case "Short":
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.INTEGER);
                break;
            case "Double":
            case "Float":
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.NUMBER);
                break;
            default:
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
        }
    }

    void getArraySchema(Type aType, Map<String, Type> aTypeVariableMap, Schema.SchemaBuilder aSchemaBuilder, Index aJandex) {
        Type arrayType = aType.asArrayType().component().kind().equals(Type.Kind.TYPE_VARIABLE)
                ? aTypeVariableMap.get(aType.asArrayType().component().toString())
                : aType.asArrayType().component();
        aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.ARRAY).items(getSchema(arrayType, aJandex, aTypeVariableMap));
    }

    void getPrimitiveSchema(Type aType, Schema.SchemaBuilder aSchemaBuilder) {
        switch (aType.asPrimitiveType().primitive()) {
            case BOOLEAN:
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.BOOLEAN);
                break;
            case CHAR:
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.STRING);
                break;
            case INT:
            case LONG:
            case SHORT:
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.INTEGER);
                break;
            case DOUBLE:
            case FLOAT:
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.NUMBER);
                break;
            default:
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
        }
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
