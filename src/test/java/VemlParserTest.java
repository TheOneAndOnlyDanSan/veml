import org.junit.jupiter.api.Test;
import veml.VemlElement;
import veml.VemlParser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static reflection.FieldReflection.getFieldValue;
import static reflection.FieldReflection.getFields;

public class VemlParserTest {

    private boolean isPrimitive(Class<?> type, Object value) {
        if(value == null) {
            return true;
        }

        Class<?> valueType = value.getClass();
        return type.isPrimitive() || type.equals(String.class) || type.equals(Class.class) || valueType.equals(Integer.class) || valueType.equals(Long.class) || valueType.equals(Short.class) || valueType.equals(Byte.class) || valueType.equals(Float.class) || valueType.equals(Double.class) || valueType.equals(Boolean.class) || valueType.equals(Character.class);
    }


    public String toString(Object o) {
        StringBuilder builder = new StringBuilder();

        for(Field f : getFields(o.getClass(), true)) {

            Object value = getFieldValue(f, o);

            if(!isPrimitive(f.getType(), value)) {
                builder.append(f.getName());
                builder.append(" = ");
                builder.append("{");
                toString(value);
                builder.append("}");
            } else {
                builder.append(f.getName());
                builder.append(" = ");
                builder.append(value);
                builder.append(", ");
            }
        }
        return builder.deleteCharAt(builder.length() - 2).toString().replace(", this$0 = null", "");
    }

    @Test
    public void primitivesTest() {
        Object root = new Object() {
            public int i;
            protected short s;
            long l;
            char c;
            float f;
            double d;
            byte b;
            boolean bool;
            Class<?> clazz;
        };

        List<String> veml = List.of(
                "i = 1",
                "s = 1s",
                "l = 1l",
                "c = '1'",
                "f = 1f",
                "d = 1.1",
                "b = 1b",
                "bool = true",
                "clazz = int.class"
        );

        Object x = new VemlParser().ignoreWrongNames(true).parse(root.getClass(), veml);
        assertEquals("i = 1, s = 1, l = 1, c = 1, f = 1.0, d = 1.1, b = 1, bool = true, clazz = int ", toString(x));
    }
}
