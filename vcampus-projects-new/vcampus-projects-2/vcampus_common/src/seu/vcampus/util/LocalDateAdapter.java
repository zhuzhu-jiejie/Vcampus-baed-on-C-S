package seu.vcampus.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateAdapter implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public JsonElement serialize(LocalDate date, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(FORMATTER.format(date)); // 格式化为 "2025-09-16"
    }

    @Override
    public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
        return LocalDate.parse(json.getAsString(), FORMATTER);
    }
}