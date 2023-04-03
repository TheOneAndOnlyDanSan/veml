import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;

import static reflection.FieldReflection.*;

public class DeepEquals {

    private final IdentityHashMap<Object, Object> visited;

    public DeepEquals() {
        this.visited = new IdentityHashMap<>();
    }

    public DeepEquals(IdentityHashMap<Object, Object> visited) {
        this.visited = visited;
    }

    public static boolean deepEquals(Object obj1, Object obj2) {
        return new DeepEquals().objectsEqual(obj1, obj2);
    }

    public boolean objectsEqual(Object obj1, Object obj2) {
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
                if (!objectsEqual(Array.get(obj1, i), Array.get(obj2, i))) {
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
            if((boolean) getFieldValue(getField(Field.class, "trustedFinal"), f)) continue;

            Object value1 = getFieldValue(f, obj1);
            Object value2 = getFieldValue(f, obj2);

            if(f.getType().isPrimitive()) {
                if(!value1.equals(value2)) return false;
            } else if (!objectsEqual(value1, value2)) {
                return false;
            }
        }

        return true;
    }
}
