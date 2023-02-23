import org.junit.jupiter.api.Test;
import veml.VemlElement;
import veml.VemlParser;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reflection.FieldReflection.*;

public class VemlParserTest {

    public static boolean deepEquals(Object obj1, Object obj2, int... modifiers) {
        return objectsEqual(obj1, obj2, new IdentityHashMap<>(), modifiers);
    }

    private static boolean objectsEqual(Object obj1, Object obj2, IdentityHashMap<Object, Object> visited, int... modifiers) {
        if (obj1 == obj2) {
            return true;
        }

        if (obj1 == null || obj2 == null) {
            return false;
        }

        if (obj1.getClass() != obj2.getClass()) {
            return false;
        }

        if(obj1.getClass().isArray()) {
            int length = Array.getLength(obj1);

            if (length != Array.getLength(obj2)) {
                return false;
            }

            for (int i = 0; i < length; i++) {
                if (!objectsEqual(Array.get(obj1, i), Array.get(obj2, i), visited, modifiers)) {
                    return false;
                }
            }

            return true;
        }

        if (visited.containsKey(obj1) && visited.get(obj1) == obj2) {
            return true;
        }

        visited.put(obj1, obj2);

        Field[] fields = getFields(obj1.getClass(), true);

        for (Field f : fields) {
            if (Arrays.stream(modifiers).anyMatch(modifier -> (modifier == 0 && (f.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0) || (modifier != 0 && (modifier & f.getModifiers()) != 0))) continue;

            VemlElement element;
            element = f.getAnnotation(VemlElement.class);

            if(element != null && element.ignore()) continue;

            if (!f.getType().isPrimitive() && !objectsEqual(getFieldValue(f, obj1), getFieldValue(f, obj2), visited, modifiers)) {
                return false;
            }
        }

        return true;
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
            AtomicBoolean bool = new AtomicBoolean(true);
            Class<?> clazz = int.class;
            Object[] objects = new Object[]{};
            Object[] objects2 = new Object[] {
                new Object() {
                    @VemlElement(comment = "comment") int i = 1;
                    short s = 1;
                    long l = 1;
                    char c = '1';
                    float f = 1;
                    double d = 1;
                    byte b = 1;
                    boolean bool = true;
                    Class<?> clazz = int.class;
                    Object obj = objects2;
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
            Object object = objects2[1];
        };

        VemlParser parser = new VemlParser().ignoreFieldsWithModifiers();

        System.out.println();
        parser.stringify(root).forEach(System.out::println);
        System.out.println();

        assertTrue(deepEquals(root, parser.parse(root.getClass(), parser.stringify(root))));
    }
}
