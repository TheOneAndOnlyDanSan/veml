import org.junit.jupiter.api.Test;
import veml.VemlElement;
import veml.VemlParser;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

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

            if(f.isSynthetic()) {
                continue;
            }

            Object value = getFieldValue(f, o);

            builder.append(f.getName());
            builder.append(" = ");
            if(!isPrimitive(f.getType(), value)) {
                if(value == null) {
                    builder.append("null");
                } else {
                    builder.append("{");
                    builder.append(toString(value));
                    builder.append("}");
                }
            } else if(f.getType().isArray()) {

                builder.append("[");
                for(Object obj : (Object[]) value) {
                    builder.append(toString(obj));
                }
                builder.append("]");
            } else {
                builder.append(value);
            }
            builder.append(", ");
        }
        if(builder.length() > 1) return builder.substring(0, builder.length() - 2);
        return builder.toString();
    }

    @Test
    public void primitivesTest() {
        Object root = new Object() {
            int i = 1;
            short s = 1;
            long l = 1;
            char c = '1';
            float f = 1;
            double d = 1;
            byte b = 1;
            boolean bool = true;
            Class<?> clazz = int.class;
            Object[] objects = new Object[]{1, 2, 3, "4", int.class};
            Object object = objects;
            Object[] objects2 = new Object[] {
                    new Object() {
                        int i = 1;
                        short s = 1;
                        long l = 1;
                        char c = '1';
                        float f = 1;
                        double d = 1;
                        byte b = 1;
                        boolean bool = true;
                        Class<?> clazz = int.class;
                        Object obj = null;
                    },
                    new Object() {
                        int i = 1;
                        short s = 1;
                        long l = 1;
                        char c = '1';
                        float f = 1;
                        double d = 1;
                        byte b = 1;
                        boolean bool = true;
                        Class<?> clazz = int.class;
                        Object obj = null;
                    }
            };
        };

        VemlParser parser = new VemlParser();
        System.out.println("");
        parser.stringify(root).forEach(System.out::println);
        System.out.println("");
        assertEquals(toString(root), toString(parser.parse(root.getClass(), parser.stringify(root))));
    }
}
