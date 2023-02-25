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

                Object obj = getFieldValue(f, instance);

                if(isPrimitive(obj, null) || (f.getType().isArray() || obj.getClass().isArray()) && (array2list(obj).stream().map(value -> isPrimitive(null, value.getClass())).allMatch(value -> value) || array2list(obj).size() == 0)) {
                    add(name, f, instance, comment, path, null);
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
                close();

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

        if(existingObject2string.containsKey(value)) {
            String output = existingObject2string.get(value);


            if(path.contains(".")) {
                veml.remove(veml.size() -1);
                veml.remove(veml.size() -1);
                path = path.substring(0, path.lastIndexOf("."));
            }
            else path = "";

            if(output.startsWith(path)) return output.replace(path + ".", "");
            else return "super." + output;
        }

        if(!isPrimitive(value, null)) {
            existingObject2string.put(value, path);
            if(existingObject2string2 != null) existingObject2string2.put(value, (isArray ? "[" : "{") + path);
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
            List<?> arrayValue = array2list(value);

            if(arrayValue.size() == 0 || arrayValue.stream().filter(v -> !isPrimitive(v, null)).toList().size() == 0) {
                builder.append("[");

                for(int i = 0;i < arrayValue.size();i++) {
                    Object o = arrayValue.get(i);
                    builder.append(parseValue(o, type.componentType(), path + "[" + i + "]", true, existingObject2string2));
                    builder.append(", ");
                }

                if(arrayValue.size() >= 1) {
                    return builder.substring(0, builder.length() - 2) + "]" + (value.getClass() == type ? "" : " " + value.getClass().getComponentType().getTypeName());
                }
                return builder + "]" + (value.getClass() == type ? "" : " " + value.getClass().getComponentType().getTypeName());
            } else {

                veml.add("");
                veml.add("|" + path + "|" + (value.getClass() == type ? "" : " " + value.getClass().getComponentType().getTypeName()));

                for(int i = 0;i < arrayValue.size();i++) {
                    Object o = arrayValue.get(i);
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
            if(!veml.get(veml.size() -1).equals("")) veml.add("");
            veml.add((isArray ? "[" : "{") + (isArray ? path.substring(0, path.lastIndexOf("[")) : path) + (isArray ? "]" : "}") + (value.getClass() == type ? "" : " " + value.getClass().getTypeName()));
            parse(value, path);
        }
        return null;
    }

    private void close() {
        List<String> last = veml.stream().filter(s -> (s.startsWith("[") && s.endsWith("]") || s.startsWith("{") && s.endsWith("}")) || s.startsWith("[") && s.contains("=")).toList();

        if(last.size() != 0 && !last.get(last.size() - 1).contains("=") && last.get(last.size() - 1).length() != 2) {
            String open = last.get(last.size() - 1).substring(0, 1);
            veml.add(open.equals("{") ? "{}" : "[]");
            veml.add("");
        }
    }

    private List<Object> array2list(Object array) {
        Class<?> arrayType = array.getClass().getComponentType();
        List<Object> list = new ArrayList<>();

        if (arrayType == int.class) {
            int[] intArray = (int[]) array;
            for (int i : intArray) {
                list.add(i);
            }
        } else if (arrayType == float.class) {
            float[] floatArray = (float[]) array;
            for (float f : floatArray) {
                list.add(f);
            }
        } else if (arrayType == double.class) {
            double[] doubleArray = (double[]) array;
            for (double d : doubleArray) {
                list.add(d);
            }
        } else if (arrayType == boolean.class) {
            boolean[] booleanArray = (boolean[]) array;
            for (boolean b : booleanArray) {
                list.add(b);
            }
        } else if (arrayType == byte.class) {
            byte[] byteArray = (byte[]) array;
            for (byte b : byteArray) {
                list.add(b);
            }
        } else if (arrayType == char.class) {
            char[] charArray = (char[]) array;
            for (char c : charArray) {
                list.add(c);
            }
        } else if (arrayType == short.class) {
            short[] shortArray = (short[]) array;
            for (short s : shortArray) {
                list.add(s);
            }
        } else if (arrayType == long.class) {
            long[] longArray = (long[]) array;
            for (long l : longArray) {
                list.add(l);
            }
        } else {
            Collections.addAll(list, (Object[]) array);
        }
        return list;
    }
}