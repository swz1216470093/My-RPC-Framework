package com.swz.rpc.serializer;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * @author 向前走不回头
 * @date 2021/7/23
 */
public class JsonSerializer implements Serializer{
    @Override
    public <T> byte[] serialize(T object) {
        final Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        return gson.toJson(object).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        final Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        return gson.fromJson(new String(bytes,StandardCharsets.UTF_8),clazz);
    }
    static class ClassCodec implements com.google.gson.JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            try {
                String str = jsonElement.getAsString();
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Class<?> aClass, Type type, JsonSerializationContext jsonSerializationContext) {
            // class -> json
            return new JsonPrimitive(aClass.getName());
        }
    }
}
