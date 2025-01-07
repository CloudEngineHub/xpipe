package io.xpipe.app.util;

import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.xpipe.app.ext.LocalStore;
import io.xpipe.app.storage.*;
import io.xpipe.app.terminal.ExternalTerminalType;
import io.xpipe.core.util.JacksonMapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.CharArrayReader;
import java.io.IOException;
import java.util.UUID;

public class AppJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(VaultKeySecretValue.class);
        context.registerSubtypes(PasswordLockSecretValue.class);

        addSerializer(DataStoreEntryRef.class, new DataStoreEntryRefSerializer());
        addDeserializer(DataStoreEntryRef.class, new DataStoreEntryRefDeserializer());
        addSerializer(ContextualFileReference.class, new LocalFileReferenceSerializer());
        addDeserializer(ContextualFileReference.class, new LocalFileReferenceDeserializer());
        addSerializer(ExternalTerminalType.class, new ExternalTerminalTypeSerializer());
        addDeserializer(ExternalTerminalType.class, new ExternalTerminalTypeDeserializer());
        addSerializer(EncryptedValue.class, new EncryptedValueSerializer());
        addDeserializer(EncryptedValue.class, new EncryptedValueDeserializer());

        context.addSerializers(_serializers);
        context.addDeserializers(_deserializers);
    }

    public static class LocalFileReferenceSerializer extends JsonSerializer<ContextualFileReference> {

        @Override
        public void serialize(ContextualFileReference value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.serialize());
        }
    }

    public static class LocalFileReferenceDeserializer extends JsonDeserializer<ContextualFileReference> {

        @Override
        public ContextualFileReference deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return ContextualFileReference.of(p.getValueAsString());
        }
    }

    public static class ExternalTerminalTypeSerializer extends JsonSerializer<ExternalTerminalType> {

        @Override
        public void serialize(ExternalTerminalType value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.getId());
        }
    }

    public static class ExternalTerminalTypeDeserializer extends JsonDeserializer<ExternalTerminalType> {

        @Override
        public ExternalTerminalType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var id = p.getValueAsString();
            return ExternalTerminalType.ALL_ON_ALL_PLATFORMS.stream()
                    .filter(terminalType -> terminalType.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
    }

    @SuppressWarnings("all")
    public static class EncryptedValueSerializer extends JsonSerializer<EncryptedValue> {

        @Override
        public void serialize(EncryptedValue value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            value.getSecret().serialize(jgen, value.allowUserSecretKey());
        }
    }

    @SuppressWarnings("all")
    public static class EncryptedValueDeserializer extends JsonDeserializer<EncryptedValue> implements ContextualDeserializer {

        private boolean useCurrentSecretKey;
        private Class<?> type;

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
            JavaType wrapperType = property.getType();
            JavaType valueType = wrapperType.containedType(0);
            var deserializer = new EncryptedValueDeserializer();
            var useCurrentSecretKey = !wrapperType.equals(SimpleType.constructUnsafe(EncryptedValue.VaultKey.class));
            deserializer.useCurrentSecretKey = useCurrentSecretKey;
            deserializer.type = valueType.getRawClass();
            return deserializer;
        }

        @Override
        public EncryptedValue<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Object value;
            var secret = DataStorageSecret.deserialize(JacksonMapper.getDefault().readTree(p));
            if (secret == null) {
                var raw = JacksonMapper.getDefault().readValue(p, type);
                if (raw != null) {
                    value = raw;
                } else {
                    return null;
                }
            } else {
                value = JacksonMapper.getDefault().readValue(new CharArrayReader(secret.getSecret()), type);
            }
            var perUser = useCurrentSecretKey || secret.getEncryptedToken().isUser();
            return perUser ? new EncryptedValue.CurrentKey<>(value, secret) : new EncryptedValue.VaultKey<>(value, secret);
        }
    }

    @SuppressWarnings("all")
    public static class DataStoreEntryRefSerializer extends JsonSerializer<DataStoreEntryRef> {

        @Override
        public void serialize(DataStoreEntryRef value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            if (value == null) {
                jgen.writeNull();
                return;
            }

            jgen.writeStartObject();
            jgen.writeFieldName("storeId");
            jgen.writeString(value.get().getUuid().toString());
            jgen.writeEndObject();
        }
    }

    public static class DataStoreEntryRefDeserializer extends JsonDeserializer<DataStoreEntryRef<?>> {

        @Override
        public DataStoreEntryRef<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            var obj = (ObjectNode) p.getCodec().readTree(p);
            if (!obj.has("storeId") || !obj.required("storeId").isTextual()) {
                return null;
            }

            var text = obj.required("storeId").asText();
            if (text.isBlank()) {
                return null;
            }

            var id = UUID.fromString(text);
            // Keep an invalid entry if it is per-user, meaning that it will get removed later on
            var e = DataStorage.get()
                    .getStoreEntryIfPresent(id)
                    .filter(dataStoreEntry -> dataStoreEntry.getValidity() != DataStoreEntry.Validity.LOAD_FAILED
                            || !dataStoreEntry.getStoreNode().isAvailableForUser())
                    .orElse(null);
            if (e == null) {
                return null;
            }

            // Compatibility fix for legacy local stores
            var toUse = e.getStore() instanceof LocalStore ? DataStorage.get().local() : e;
            if (toUse == null) {
                return null;
            }

            return new DataStoreEntryRef<>(toUse);
        }
    }
}
