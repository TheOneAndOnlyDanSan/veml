package veml;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static reflection.FieldReflection.*;

class ObjectToVeml {

    private final int[] modifiers;

    ObjectToVeml(int[] modifiers) {
        this.modifiers = modifiers;
    }

    boolean doObjects = false;
    List<String> veml = new ArrayList<>();
    IdentityHashMap<Object, String> existingObject2string = new IdentityHashMap<>();

    List<String> parse(Object instance, String path) {

        if(path.equals("")) existingObject2string.put(instance, "super");

        LinkedHashMap<String, LinkedList<Object[]>> objects = new LinkedHashMap<>();
        for(Field f : getFields(instance.getClass(), true)) {
            if(!(Boolean) getFieldValue(getField(Field.class, "trustedFinal"), f) && Arrays.stream(modifiers).noneMatch(modifier -> modifier == 0 && (f.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0 || (modifier &  f.getModifiers()) != 0)) {
                VemlElement element = f.getAnnotation(VemlElement.class);

                String name;
                String comment = "";

                if(element == null) {
                    name = f.getName();
                } else {
                    if (element.ignore()) continue;
                    name = element.name().equals("") ? f.getName() : element.name();
                    comment = element.comment().equals("") ? "" : " //" + element.comment();
                }

                if(isPrimitive(getFieldValue(f, instance), null) || f.getType().isArray() && (isPrimitive(null, f.getType().getComponentType()) || ((Object[]) getFieldValue(f, instance)).length == 0)) {
                    add(name, f, instance, comment, path, existingObject2string);
                } else {
                    if (objects.containsKey(name)) {
                        objects.get(name).add(new Object[]{name, f, instance, comment, path});
                    } else {
                        Object[] objs = new Object[]{name, f, instance, comment, path};
                        LinkedList<Object[]> map = new LinkedList<>();
                        map.add(objs);
                        objects.put(name, map);
                    }
                }
            }
        }

        LinkedList<Object[]>[] list = new LinkedList[objects.size()];
        int put = 0;

        IdentityHashMap<Object, String> existingObject2stringList = new IdentityHashMap<>();

        for(LinkedList<Object[]> objses : objects.values()) {
            for(Object[] objs :objses){
                if(existingObject2string.containsKey(getFieldValue((Field) objs[1], objs[2]))) {
                    list[put] = new LinkedList<>();
                    list[put].add(objs);
                    put++;
                }
            }
        }

        for(LinkedList<Object[]> objses : objects.values()) {
            for(Object[] objs :objses){
                Object key = getFieldValue((Field) objs[1], objs[2]);
                if(!existingObject2string.containsKey(key)) {
                    list[put] = new LinkedList<>();
                    list[put].add(objs);
                    put++;
                }
            }
        }

        for(LinkedList<Object[]> objses : list) {
            for(Object[] objs : objses){
                Object value = getFieldValue((Field) objs[1], objs[2]);
                if(existingObject2stringList.containsKey(value)) {
                    if(existingObject2stringList.get(value).endsWith("]")) {
                        veml.add("[]");
                    } else {
                        veml.add("{}");
                    }
                    veml.add("");
                }

                add((String) objs[0], (Field) objs[1], objs[2], (String) objs[3], (String) objs[4], existingObject2stringList);
            }
        }

        return veml;
    }

    private void add(String name, Field f, Object instance, String comment, String path, IdentityHashMap<Object, String> existingObject2string) {
        Object fieldValue = getFieldValue(f, instance);

        String value = parseValue(fieldValue, f.getType(), path + (path.equals("") ? "" : ".") + name, false, existingObject2string);
        if(value != null) {
            veml.add(name + " = " + value + comment);
        }
    }

    private boolean isPrimitive(Object value, Class<?> valueType) {
        if(valueType == null) {
            if(value == null) return true;
            valueType = value.getClass();
        }

        return valueType.isPrimitive() || valueType.equals(String.class) || valueType.equals(Class.class) || valueType.equals(Integer.class) || valueType.equals(Long.class) || valueType.equals(Short.class) || valueType.equals(Byte.class) || valueType.equals(Float.class) || valueType.equals(Double.class) || valueType.equals(Boolean.class) || valueType.equals(Character.class);
    }

    private String parseValue(Object value, Class<?> type, String path, boolean isArray, IdentityHashMap<Object, String> existingObject2string2) {
        if(value == null) return "null";

        if(existingObject2string2.containsKey(value)) {
            return existingObject2string2.get(value);
        } else if(existingObject2string.containsKey(value)) {
            return existingObject2string.get(value);
        }

        if(!isPrimitive(value, null)) {
            existingObject2string.put(value, path);
            existingObject2string2.put(value, path);
        }

        Class<?> valueType = value.getClass();

        if(isPrimitive(value, null)) {

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

            if(arrayValue.length == 0 || Arrays.stream(arrayValue).filter(v -> !isPrimitive(v, null)).toList().size() == 0) {
                builder.append("[");

                for(int i = 0;i < arrayValue.length;i++) {
                    Object o = arrayValue[i];
                    builder.append(parseValue(o, type.componentType(), path + "[" + i + "]", true, existingObject2string2));
                    builder.append(", ");
                }

                if(arrayValue.length > 1) {
                    return builder.substring(0, builder.length() - 2) + "]";
                }
                return builder + "]" + (value.getClass() == type ? "" : " " + value.getClass().getTypeName());
            } else {

                veml.add("");
                veml.add("|" + path + "|" + (value.getClass() == type ? "" : " " + value.getClass().getTypeName()));

                for(int i = 0;i < arrayValue.length;i++) {
                    Object o = arrayValue[i];
                    o = parseValue(o, type.componentType(), path + "[" + i + "]", true, existingObject2string2);

                    if(!(o == null)) {
                        if(i != 0) {
                            List<String> last = veml.stream().filter(s -> s.startsWith("[") && s.endsWith("]") || s.startsWith("{") && s.endsWith("}") || s.startsWith("[") && s.contains("=")).toList();

                            if(last.size() != 0 && !last.get(last.size() - 1).contains("=")) {
                                veml.add("");
                            }
                        }
                        veml.add("[" + path + "] = " + o);
                    }
                }
            }

        } else {
            veml.add("");
            veml.add((isArray ? "[" : "{") + (isArray ? path.substring(0, path.lastIndexOf("[")) : path) + (isArray ? "]" : "}") + (value.getClass() == type ? "" : " " + value.getClass().getTypeName()));
            parse(value, path);
        }
        return null;
    }
}
