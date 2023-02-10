package io.quarkiverse.asyncapi.annotation.scanner;

import static org.jboss.jandex.Type.Kind.PARAMETERIZED_TYPE;
import static org.jboss.jandex.Type.Kind.PRIMITIVE;

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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.JandexReflection;
import org.jboss.jandex.Type;

import com.asyncapi.v2.model.AsyncAPI;
import com.asyncapi.v2.model.channel.ChannelItem;
import com.asyncapi.v2.model.channel.message.Message;
import com.asyncapi.v2.model.channel.operation.Operation;
import com.asyncapi.v2.model.component.Components;
import com.asyncapi.v2.model.info.Contact;
import com.asyncapi.v2.model.info.Info;
import com.asyncapi.v2.model.info.License;
import com.asyncapi.v2.model.schema.Schema;

/**
 * @since 09.02.2023
 * @author christiant
 */
public class AsyncApiAnnotationScanner {

    public static final String CONFIG_PREFIX = "io.quarkiverse.asyncapi.";

    IndexView index;
    AsyncApiRuntimeConfig config;

    public AsyncAPI scan(IndexView aIndex, AsyncApiRuntimeConfig aConfig) {
        index = aIndex;
        config = aConfig;
        return AsyncAPI.builder()
                .asyncapi("2.6.0")
                .id(getConfigValue("id", "urn:com:kafka:server"))
                .info(getInfo())
                .defaultContentType(getConfigValue("defaultContentType", "application/json"))
                .channels(getChannels())
                .components(getGlobalComponents())
                .build();
    }

    public Info getInfo() {
        return Info.builder() //TODO implement Annotation to define it (use OpenApi???)
                .title(config.info().title())
                .version(getConfigValue("version", "1"))
                .description(getConfigValue("description", ""))
                .license(License.builder()
                        .name(getConfigValue("license.name", "Commercial"))
                        .url(getConfigValue("license.url", "https://gec.io/"))
                        .build())
                .contact(Contact.builder()
                        .name(getConfigValue("contact.name", "Contact and Support"))
                        .url(getConfigValue("contact.url", "https://gec.io/kontakt/"))
                        .email(getConfigValue("contact.email", "support@gec.io"))
                        .build())
                .build();
    }

    public Map<String, ChannelItem> getChannels() {
        return index.getAnnotations("org.eclipse.microprofile.reactive.messaging.Channel")
                .stream()
                .filter(annotation -> !annotation.value().asString().isEmpty())
                .map(annotation -> getChannelItem(annotation))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, TreeMap::new));
    }

    String getConfigValue(String aPostfix, String aDefault) {
        return ConfigProvider.getConfig().getOptionalValue(CONFIG_PREFIX + aPostfix, String.class).orElse(aDefault);
    }

    AbstractMap.SimpleEntry<String, ChannelItem> getChannelItem(AnnotationInstance aAnnotationInstance) {
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
                    case CLASS ->
                        messageType = genericMessageType.asClassType();
                    case PARAMETERIZED_TYPE ->
                        messageType = genericMessageType.asParameterizedType();
                    default ->
                        throw new IllegalArgumentException("unhandled messageType " + genericMessageType.kind());
                }
                break;

            //TODO other annotation-targets
            default:
                throw new IllegalArgumentException("unknwon messageType " + aAnnotationInstance);
        }
        String channel = aAnnotationInstance.value().asString();
        String configKey = "mp.messaging." + (isEmitter ? "outgoing" : "incomming") + "." + channel + ".topic";
        String topic = ConfigProvider.getConfig()
                .getOptionalValue(configKey, String.class)
                .orElse(channel);
        MyKafkaChannelBinding channelBinding = new MyKafkaChannelBinding(topic);
        ConfigProvider.getConfig()
                .getOptionalValue(CONFIG_PREFIX + ".topic." + topic + ".description", String.class)
                .ifPresent(channelBinding::setDescription);
        ChannelItem.ChannelItemBuilder channelBuilder = ChannelItem.builder()
                .bindings(Map.of("kafka", channelBinding));
        Operation operation = Operation.builder()
                .message(getMessage(messageType))
                .operationId(operationId)
                .build();
        addSchemaAnnotationData(aAnnotationInstance.target(), operation);
        ChannelItem channelItem = isEmitter
                ? channelBuilder.publish(operation).build()
                : channelBuilder.subscribe(operation).build();
        ConfigProvider.getConfig()
                .getOptionalValue(CONFIG_PREFIX + ".channel." + channel + ".description", String.class)
                .ifPresent(channelItem::setDescription);
        return new AbstractMap.SimpleEntry<>(channel, channelItem);
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

    Message getMessage(Type aMessageType) {
        return Message.builder()
                .name(aMessageType.name().toString()) //TODO expect to be overriden by annotation
                .contentType("application/json")
                .payload(getSchema(aMessageType, new HashMap<>()))
                .build();
    }

    Schema getSchema(Type aType, Map<String, Type> typeVariableMap) {
        Schema.SchemaBuilder schemaBuilder = Schema.builder();
        switch (aType.kind()) {
            case PRIMITIVE ->
                getPrimitiveSchema(aType, schemaBuilder);
            case ARRAY ->
                getArraySchema(aType, typeVariableMap, schemaBuilder);
            case CLASS, PARAMETERIZED_TYPE ->
                getClassSchema(aType, schemaBuilder, typeVariableMap);
            default -> //TODO other types
                schemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
        }
        return schemaBuilder.build();
    }

    void getClassSchema(Type aType, Schema.SchemaBuilder aSchemaBuilder, Map<String, Type> aTypeVariableMap) {
        ClassInfo classInfo = index.getClassByName(aType.name());
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
                addSchemaAnnotationData(classInfo, aSchemaBuilder);
                if (aType.kind().equals(Type.Kind.PARAMETERIZED_TYPE)) {
                    for (int i = 0; i < aType.asParameterizedType().arguments().size(); i++) {
                        aTypeVariableMap.put(classInfo.typeParameters().get(i).identifier(),
                                aType.asParameterizedType().arguments().get(i));
                    }
                }
                //TODO consider getter-annotations, too
                List<FieldInfo> allFieldsRecursiv = getAllFieldsRecursiv(classInfo, aTypeVariableMap);
                Map<String, Schema> properties = allFieldsRecursiv.stream().collect(Collectors.toMap(
                        FieldInfo::name,
                        f -> getFieldSchema(f, aTypeVariableMap), (a, b) -> b, TreeMap::new));
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
                            .items(getSchema(collectionType, aTypeVariableMap));
                }
                //TODO other non-indexed tyes from jre, e.g. maps...
            }
        }
    }

    Schema getFieldSchema(FieldInfo aFieldInfo, Map<String, Type> aTypeVariableMap) {
        Schema schema = getSchema(aTypeVariableMap.getOrDefault(aFieldInfo.type().toString(), aFieldInfo.type()),
                aTypeVariableMap);
        addSchemaAnnotationData(aFieldInfo, schema);
        return schema;
    }

    void addSchemaAnnotationData(AnnotationTarget aAnnotationTarget, Schema aSchema) {
        addSchemaAnnotationStringData(aAnnotationTarget, "description", aSchema::setDescription);
    }

    void addSchemaAnnotationData(AnnotationTarget aAnnotationTarget, Schema.SchemaBuilder aSchemaBuilder) {
        addSchemaAnnotationStringData(aAnnotationTarget, "description", aSchemaBuilder::description);
    }

    void addSchemaAnnotationData(AnnotationTarget aAnnotationTarget, Operation aOperation) {
        addSchemaAnnotationStringData(aAnnotationTarget, "description", aOperation::setDescription);
    }

    void addSchemaAnnotationStringData(AnnotationTarget aAnnotationTarget, String aAnnotationFieldName,
            Consumer<String> aSetter) {
        AnnotationInstance annotation = aAnnotationTarget.declaredAnnotation(
                DotName.createSimple("org.eclipse.microprofile.openapi.annotations.media.Schema"));
        if (annotation != null) {
            AnnotationValue value = annotation.value(aAnnotationFieldName);
            if (value != null) {
                aSetter.accept(value.asString());
            }
        }
    }

    void getJavaLangPackageSchema(Type aType, Schema.SchemaBuilder aSchemaBuilder) {
        switch (aType.name().withoutPackagePrefix()) {
            case "Boolean" ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.BOOLEAN);
            case "Character", "String" ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.STRING);
            case "Integer", "Long", "Short" ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.INTEGER);
            case "Double", "Float" ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.NUMBER);
            default ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
        }
    }

    void getArraySchema(Type aType, Map<String, Type> aTypeVariableMap, Schema.SchemaBuilder aSchemaBuilder) {
        Type arrayType = aType.asArrayType().component().kind().equals(Type.Kind.TYPE_VARIABLE)
                ? aTypeVariableMap.get(aType.asArrayType().component().toString())
                : aType.asArrayType().component();
        aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.ARRAY).items(getSchema(arrayType, aTypeVariableMap));
    }

    void getPrimitiveSchema(Type aType, Schema.SchemaBuilder aSchemaBuilder) {
        switch (aType.asPrimitiveType().primitive()) {
            case BOOLEAN ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.BOOLEAN);
            case CHAR ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.STRING);
            case INT, LONG, SHORT ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.INTEGER);
            case DOUBLE, FLOAT ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.NUMBER);
            default ->
                aSchemaBuilder.type(com.asyncapi.v2.model.schema.Type.OBJECT);
        }
    }

    List<FieldInfo> getAllFieldsRecursiv(ClassInfo aClassInfo, Map<String, Type> aTypeVariableMap) {
        ArrayList<FieldInfo> fieldInfos = new ArrayList<>();
        if (aClassInfo.fields() != null) {
            aClassInfo.fields().stream()
                    .filter(f -> !Modifier.isStatic(f.flags()))
                    .filter(Predicate.not(FieldInfo::isEnumConstant))
                    .forEach(fieldInfos::add);
        }
        ClassInfo superClass = index.getClassByName(aClassInfo.superName());
        if (superClass != null) {
            fieldInfos.addAll(getAllFieldsRecursiv(superClass, aTypeVariableMap));
        }
        return fieldInfos;
    }
}
