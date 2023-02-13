package veml;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import static reflection.FieldReflection.*;

class ObjectToVeml {

    private static HashMap<String, Object> object2hashmap(Object instance) {
        HashMap<String, Object> hashMap = new HashMap<>();

        for(Field f : getFields(instance.getClass(), true)) {
            if(Modifier.isStatic(f.getModifiers())) {
                continue;
            }
        }

        return hashMap;
    }

    List<String> parse(Object instance) {
        return null;
    }
}
