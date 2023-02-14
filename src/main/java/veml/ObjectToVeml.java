package veml;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import static reflection.FieldReflection.*;

class ObjectToVeml {

    private final int[] modifiers;

    ObjectToVeml(int[] modifiers) {
        this.modifiers = modifiers;
    }

    List<String> veml = new ArrayList<>();
    HashMap<Object, String> existingObject2string = new HashMap<>();

    List<String> parse(Object instance, String path) {

        existingObject2string.put(instance, ".");

        for(Field f : getFields(instance.getClass(), true)) {
            if(Arrays.stream(modifiers).noneMatch(modifier -> modifier == 0 && (f.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0 || (modifier &  f.getModifiers()) != 0)) {
                VemlElement element = f.getAnnotation(VemlElement.class);

                String name;

                if(element == null) {
                    name = f.getName();
                } else {
                    if (!element.include()) continue;
                    name = element.name();
                }
                Object fieldValue = getFieldValue(f, instance);

                if(existingObject2string.containsKey(fieldValue)) {
                    veml.add(name + " = " + existingObject2string.get(fieldValue));
                    continue;
                }

                String value = parseValue(fieldValue, f.getType(), path + (path.equals("") ? "" : ".") + name, false);
                if(value != null) {
                    if(!isPrimitive(fieldValue)) existingObject2string.put(fieldValue, path + (path.equals("") ? "" : ".") + name);
                    veml.add(name + " = " + value);
                }
            }
        }

        return veml;
    }

    private boolean isPrimitive(Object value) {
        if(value == null) return true;

        Class<?> valueType = value.getClass();
        return valueType.isPrimitive() || valueType.equals(String.class) || valueType.equals(Class.class) || valueType.equals(Integer.class) || valueType.equals(Long.class) || valueType.equals(Short.class) || valueType.equals(Byte.class) || valueType.equals(Float.class) || valueType.equals(Double.class) || valueType.equals(Boolean.class) || valueType.equals(Character.class);
    }

    private String parseValue(Object value, Class<?> type, String path, boolean isArray) {
        if(value == null) return "null";

        Class<?> valueType = value.getClass();

        if(isPrimitive(value)) {

            if(valueType == String.class) return "\"" + value + "\"";
            if(valueType == Integer.class) return value + "";
            if(valueType == Double.class) return value + "d";
            if(valueType == Float.class) return value + "f";
            if(valueType == Long.class) return value + "l";
            if(valueType == Short.class) return value + "s";
            if(valueType == Byte.class) return value + "b";
            if(valueType == Character.class) return "'" + value + "'";
            if(valueType == Boolean.class) return value + "";
            if(valueType == Class.class) return ((Class<?>) value).getTypeName() + ".class";
        } else if(valueType.isArray()) {
            StringBuilder builder = new StringBuilder();
            Object[] arrayValue = (Object[]) value;

            if(Arrays.stream(arrayValue).filter(obj -> isPrimitive(obj)).toList().size() != 0) {
                builder.append("[");

                for(int i = 0;i < arrayValue.length;i++) {
                    builder.append(parseValue(arrayValue[i], arrayValue[i].getClass(), path, true));
                    builder.append(", ");
                }

                if(arrayValue.length > 1) {
                    return builder.substring(0, builder.length() - 2) + "]";
                }
                return builder + "]";
            } else {
                for(int i = 0;i < arrayValue.length;i++) {
                    builder.append(parseValue(arrayValue[i], arrayValue[i].getClass(), path, true));
                    builder.append(", ");
                }
            }

        } else {
            veml.add("");
            veml.add((isArray ? "[" : "{") + path + (isArray ? "]" : "}") + (value.getClass() == type ? "" : " " + value.getClass().getTypeName()));
            parse(value, path);
        }
        return null;
    }
}
