package contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Jackson Serialization Behavior Test (Standalone)
 *
 * 目的: 验证当前 JacksonConfig 下各种数值类型的序列化行为
 * 不依赖完整Spring Boot启动，直接测试ObjectMapper配置
 */
class JacksonSerializationBehaviorStandaloneTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        // 模拟 JacksonConfig 的配置
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

        // 应用与 JacksonConfig 相同的配置
        Jackson2ObjectMapperBuilderCustomizer customizer = builderToCustomize -> {
            // Preserve 64-bit snowflake identifiers
            builderToCustomize.serializerByType(Long.class, ToStringSerializer.instance);
            builderToCustomize.serializerByType(Long.TYPE, ToStringSerializer.instance);

            // LocalDateTime 序列化/反序列化
            String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
            builderToCustomize.serializers(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimePattern)));
            builderToCustomize.deserializers(new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimePattern)));

            // LocalDate 序列化/反序列化
            String datePattern = "yyyy-MM-dd";
            builderToCustomize.serializers(new LocalDateSerializer(DateTimeFormatter.ofPattern(datePattern)));
            builderToCustomize.deserializers(new LocalDateDeserializer(DateTimeFormatter.ofPattern(datePattern)));
        };

        customizer.customize(builder);
        this.objectMapper = builder.build();
    }

    @Test
    @DisplayName("验证 Long 包装类型序列化为 JSON String")
    void test_Long_boxed_serialization() throws Exception {
        TestDTO dto = new TestDTO();
        dto.boxedLong = 123456789012345L;

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        System.out.println("=== Long (boxed) Serialization ===");
        System.out.println("JSON: " + json);
        System.out.println("boxedLong type: " + node.path("boxedLong").getNodeType());
        System.out.println("boxedLong value: " + node.path("boxedLong"));

        JsonNode boxedLong = node.path("boxedLong");
        assertTrue(boxedLong.isTextual(),
            "Long 包装类型应序列化为 STRING，实际: " + boxedLong.getNodeType());
        assertEquals("123456789012345", boxedLong.asText());
    }

    @Test
    @DisplayName("验证 long primitive 序列化行为")
    void test_long_primitive_serialization() throws Exception {
        TestDTO dto = new TestDTO();
        dto.primitiveLong = 987654321098765L;

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        System.out.println("=== long (primitive) Serialization ===");
        System.out.println("JSON: " + json);
        System.out.println("primitiveLong type: " + node.path("primitiveLong").getNodeType());
        System.out.println("primitiveLong value: " + node.path("primitiveLong"));

        JsonNode primitiveLong = node.path("primitiveLong");

        // 记录实际行为
        if (primitiveLong.isTextual()) {
            System.out.println("✓ Result: primitive long → STRING");
            assertEquals("987654321098765", primitiveLong.asText());
        } else if (primitiveLong.isNumber()) {
            System.out.println("✓ Result: primitive long → NUMBER");
            assertEquals(987654321098765L, primitiveLong.asLong());
        } else {
            fail("Unexpected type: " + primitiveLong.getNodeType());
        }
    }

    @Test
    @DisplayName("验证 Integer 包装类型序列化行为")
    void test_Integer_boxed_serialization() throws Exception {
        TestDTO dto = new TestDTO();
        dto.boxedInteger = 12345;

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        System.out.println("=== Integer (boxed) Serialization ===");
        System.out.println("JSON: " + json);
        System.out.println("boxedInteger type: " + node.path("boxedInteger").getNodeType());
        System.out.println("boxedInteger value: " + node.path("boxedInteger"));

        JsonNode boxedInteger = node.path("boxedInteger");
        assertTrue(boxedInteger.isNumber(),
            "Integer 包装类型应序列化为 NUMBER，实际: " + boxedInteger.getNodeType());
        assertEquals(12345, boxedInteger.asInt());
    }

    @Test
    @DisplayName("验证 int primitive 序列化行为")
    void test_int_primitive_serialization() throws Exception {
        TestDTO dto = new TestDTO();
        dto.primitiveInt = 67890;

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        System.out.println("=== int (primitive) Serialization ===");
        System.out.println("JSON: " + json);
        System.out.println("primitiveInt type: " + node.path("primitiveInt").getNodeType());
        System.out.println("primitiveInt value: " + node.path("primitiveInt"));

        JsonNode primitiveInt = node.path("primitiveInt");
        assertTrue(primitiveInt.isNumber(),
            "int primitive 应序列化为 NUMBER，实际: " + primitiveInt.getNodeType());
        assertEquals(67890, primitiveInt.asInt());
    }

    @Test
    @DisplayName("验证 null 值序列化行为")
    void test_null_value_serialization() throws Exception {
        TestDTO dto = new TestDTO();
        dto.boxedLong = null;
        dto.boxedInteger = null;

        String json = objectMapper.writeValueAsString(dto);
        JsonNode node = objectMapper.readTree(json);

        System.out.println("=== Null Value Serialization ===");
        System.out.println("JSON: " + json);

        assertTrue(node.path("boxedLong").isNull(), "null Long 应序列化为 null");
        assertTrue(node.path("boxedInteger").isNull(), "null Integer 应序列化为 null");
    }

    @Test
    @DisplayName("综合测试: 所有类型一起序列化")
    void test_all_types_together() throws Exception {
        TestDTO dto = new TestDTO();
        dto.boxedLong = 111L;
        dto.primitiveLong = 222L;
        dto.boxedInteger = 333;
        dto.primitiveInt = 444;

        String json = objectMapper.writeValueAsString(dto);
        System.out.println("\n=== All Types Together ===");
        System.out.println("JSON: " + json);

        JsonNode node = objectMapper.readTree(json);

        System.out.println("\n实测结果摘要:");
        System.out.println("┌──────────────────────┬────────────┬──────────────┐");
        System.out.println("│ 字段                 │ Java类型   │ JSON类型     │");
        System.out.println("├──────────────────────┼────────────┼──────────────┤");
        System.out.printf("│ %-20s │ %-10s │ %-12s │%n", "boxedLong", "Long", node.path("boxedLong").getNodeType());
        System.out.printf("│ %-20s │ %-10s │ %-12s │%n", "primitiveLong", "long", node.path("primitiveLong").getNodeType());
        System.out.printf("│ %-20s │ %-10s │ %-12s │%n", "boxedInteger", "Integer", node.path("boxedInteger").getNodeType());
        System.out.printf("│ %-20s │ %-10s │ %-12s │%n", "primitiveInt", "int", node.path("primitiveInt").getNodeType());
        System.out.println("└──────────────────────┴────────────┴──────────────┘");
    }

    // Test DTO
    static class TestDTO {
        public Long boxedLong;
        public long primitiveLong;
        public Integer boxedInteger;
        public int primitiveInt;

        public TestDTO() {}
    }
}
