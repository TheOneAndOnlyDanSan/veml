package veml;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import static reflection.ClassReflection.getClassByName;
import static reflection.ClassReflection.getPrimitiveClassByName;
import static reflection.FieldReflection.*;
import static reflection.ConstructorReflection.createInstanceWithoutConstructor;
import static reflection.FieldReflection.setFieldValue;

class VemlToObject {

    private final boolean ignoreWrongNames;
    private final int[] modifiers;

    VemlToObject(boolean ignoreWrongNames, int[] modifiers) {
        this.ignoreWrongNames = ignoreWrongNames;
        this.modifiers = modifiers;
    }

    private record Token(TYPE type, String value) {

        private enum TYPE {
            keyValuePair,
            object,
            objectArray
        }
    }

    private <T> T map2object(Class<T> clazz, LinkedHashMap<String, Object> hashMap, LinkedHashMap<Object, Object> hashmap2existingObjects) {
        T instance = createInstanceWithoutConstructor(clazz);

        List<String> keys = hashMap.keySet().stream().filter(Objects::nonNull).toList();
        HashMap<String, Field> fields = new HashMap<>();

        for(Field field : getFields(clazz, true)) {
            VemlElement name = field.getAnnotation(VemlElement.class);
            if(name != null) {
                if(fields.containsKey(name.name()) || fields.containsValue(field)) {
                    throw new IllegalArgumentException();
                }
                fields.put(name.name(), field);
            }
        }

        for(Field field : getFields(clazz, true)) {
            String name = field.getName();

            if((fields.containsKey(name) || fields.containsValue(field)) && !field.isAnnotationPresent(VemlElement.class)) {
                throw new IllegalArgumentException();
            }

            if(!fields.containsValue(field)) fields.put(name, field);
        }

        setFields:
        for(int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = hashMap.get(key);
            Class<?> valueType = value == null ? null : value.getClass();

            Field f = fields.get(key);

            if(f == null || Arrays.stream(modifiers).anyMatch(modifier -> modifier == 0 && (f.getModifiers() & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED)) == 0 || (modifier &  f.getModifiers()) != 0)) {
                if(ignoreWrongNames) continue setFields;
                else throw new IllegalArgumentException();
            }
            Class<?> fieldType = f.getType();

            if(isPrimitive(fieldType, value)) {
                setFieldValue(f, instance, value);
                hashmap2existingObjects.put(value, value);
            } else if(valueType != null && valueType.isArray()) {
                hashmap2existingObjects.get(((Object[]) value)[0]);
            } else if(valueType.equals(ArrayList.class)) {
                Class<?> componentType = fieldType.componentType();

                Object objValue = list2array(componentType == null ? fieldType : componentType, (List<?>) value, hashmap2existingObjects);

                setFieldValue(f, instance, objValue);
                hashmap2existingObjects.put(value, objValue);
            } else if(valueType.equals(LinkedHashMap.class)) {
                LinkedHashMap<String, Object> map = ((LinkedHashMap<String, Object>) value);
                String objectType = (String) map.get(null);

                Class<?> type = fieldType;
                if(objectType != null) {
                    type = getClassByName(objectType);
                }

                Object objValue = map2object(type, map, hashmap2existingObjects);

                setFieldValue(f, instance, objValue);
                hashmap2existingObjects.put(value, objValue);
            }
        }
        return instance;
    }

    private Object list2array(Class<?> type, List<?> list, LinkedHashMap<Object, Object> hashmap2existingObjects) {
        int size = list.size();
        Object array = Array.newInstance(type, size);
        for (int i = 0; i < size; i++) {
            Object value = list.get(i);

            if (value instanceof List) {
                Object objValue = list2array(type, (List<?>) value, hashmap2existingObjects);

                Array.set(array, i, objValue);
                hashmap2existingObjects.put(value, objValue);
            } else if(value instanceof HashMap<?,?>) {
                LinkedHashMap<String, Object> map = ((LinkedHashMap<String, Object>) value);
                String objectType = (String) map.get(null);

                if(objectType != null) {
                    type = getClassByName(objectType);
                }

                Object objValue = map2object(type, map, hashmap2existingObjects);

                Array.set(array, i, objValue);
                hashmap2existingObjects.put(value, objValue);
            } else {
                Array.set(array, i, value);
                hashmap2existingObjects.put(value, value);
            }
        }
        return array;
    }

    private boolean isPrimitive(Class<?> type, Object value) {
        if(value == null) {
            return true;
        }

        Class<?> valueType = value.getClass();
        return type.isPrimitive() || type.equals(String.class) || type.equals(Class.class) || valueType.equals(Integer.class) || valueType.equals(Long.class) || valueType.equals(Short.class) || valueType.equals(Byte.class) || valueType.equals(Float.class) || valueType.equals(Double.class) || valueType.equals(Boolean.class) || valueType.equals(Character.class);
    }

    private List<Token> getTokens(List<String> veml) {

        List<Token> tokens = new ArrayList<>();

        int depth = 0;
        String key = "";
        String value = "";

        for(String line : veml) {

            boolean inString = false;
            boolean isEscaped = false;
            boolean startComment = false;
            boolean finishedKey = false;
            boolean inObject = false;
            boolean inObjectArray = false;
            boolean finishedObjectArray = true;

            int objDepth = 0;

            line:
            for (String c : line.split("")) {

                if (depth != 0) {
                    finishedKey = true;
                }

                if (startComment && !c.equals("/")) {
                    throw new IllegalStateException();
                }

                if (!isEscaped && c.equals("\"")) {
                    inString = !inString;
                }

                isEscaped = c.equals("\\");

                if (!inString) {
                    switch (c) {
                        case "/" -> {
                            if (startComment) break line;
                            else startComment = true;
                        }
                        case "=" -> {
                            finishedKey = true;
                            continue;
                        }
                        case "{" -> {
                            inObject = true;
                            continue;
                        }
                        case "}" -> {
                            finishedKey = true;
                            continue;
                        }
                        case "[" -> {
                            if(finishedKey) {
                                depth++;
                            } else if(key.equals("")) {
                                objDepth++;
                                inObjectArray = true;
                                finishedObjectArray = false;
                                continue;
                            }
                        }
                        case "]" -> {
                            if(finishedKey) {
                                depth--;
                            } else if(!finishedObjectArray) {
                                objDepth--;
                                if(objDepth == 0) {
                                    finishedKey = true;
                                    finishedObjectArray = true;
                                    continue;
                                }
                            }
                        }
                    }
                }
                if (!startComment) {
                    if (!finishedKey) {
                        key += c;
                    } else {
                        value += c;
                    }
                }
            }

            key = key.trim();
            value = value.trim();

            if(isVariableName(key)) {
                if(ignoreWrongNames) continue;
                else throw new IllegalArgumentException();
            }

            if (depth == 0 && objDepth == 0 && !key.equals("") || inObject || inObjectArray) {

                if(!finishedKey) {
                    throw new IllegalArgumentException();
                }

                if (inObject) {
                    tokens.add(new Token(Token.TYPE.object, key + (value.equals("") ? "" : "-" + value)));
                } else if (inObjectArray) {
                    tokens.add(new Token(Token.TYPE.objectArray, key + (value.equals("") ? "" : "-" + value)));
                } else {
                    tokens.add(new Token(Token.TYPE.keyValuePair, key + "=" + value));
                }

                key = "";
                value = "";
            }
        }
        return tokens;
    }

    public boolean isVariableName(String name) {
        if (name.isEmpty()) {
            return false;
        }

        if (!Character.isDigit(name.charAt(0))) {
            return false;
        }

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '$' || c == '_')) {
                return false;
            }
        }

        return !Arrays.asList(
                "_", "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                "class", "const", "continue", "default", "do", "double", "else", "enum",
                "extends", "false", "final", "finally", "float", "for", "goto", "if", "implements",
                "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
                "private", "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true",
                "try", "void", "volatile", "while").contains(name);
    }

    <T> T parse(Class<T> clazz, List<String> veml) {

        Iterator<Token> tokens = getTokens(veml).iterator();

        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        LinkedHashMap<String, Object> currentObj = root;

        while (tokens.hasNext()) {
            Token token = tokens.next();

            String value = token.value;

            switch (token.type) {
                case keyValuePair -> {
                    String key = value.substring(0, value.indexOf("="));
                    value = value.substring(value.indexOf("=") + 1);

                    if(key.contains(".")) {
                        getHashMapObject(key.substring(0, key.lastIndexOf(".")), null, root).put(key.substring(key.lastIndexOf(".") +1), string2object(value, root));
                    } else {
                        if(currentObj.containsKey(key)) {
                            throw new IllegalArgumentException();
                        }
                        currentObj.put(key, string2object(value, root));
                    }
                }
                case objectArray -> {
                    if(value.contains("[new]")) throw new IllegalArgumentException();
                    currentObj = getHashMapObject(value.contains("-") ? value.substring(0, value.indexOf("-")) + "[new]" : value, value.contains("-") ? value.substring(value.indexOf("-") + 1) : null, root);
                }
                case object -> currentObj = getHashMapObject(value.contains("-") ? value.substring(0, value.indexOf("-")) : value, value.contains("-") ? value.substring(value.indexOf("-") +1) : null, root);
            }
        }

        return map2object(clazz, root, new LinkedHashMap<>());
    }

    private LinkedHashMap<String, Object> getHashMapObject(String get, String type, LinkedHashMap<String, Object> root) {
        if(get.equals("")) return root;

        LinkedHashMap<String, Object> last = root;

        String[] names = get.split("\\.");

        for(int i = 0;i < names.length;i++) {
            String name = names[i];

            if(name.contains("[")) {
                LinkedHashMap<String, Object> tempMap = (LinkedHashMap<String, Object>) getArrayIndex(name, i +1 == names.length ? type : null, last);
                if(tempMap == null) {
                    List<Object> arrayMap = new ArrayList<>();

                    LinkedHashMap<String, Object> map = new LinkedHashMap<>();
                    arrayMap.add(map);

                    last.put(name.substring(0, name.indexOf("[")), arrayMap);
                    last = map;
                } else {
                    last = tempMap;
                }
                continue;
            }

            if(last.containsKey(name)) {
                last = (LinkedHashMap<String, Object>) last.get(name);
            } else {

                last.put(name, new LinkedHashMap<>());
                last = (LinkedHashMap<String, Object>) last.get(name);

                if(i +1 == names.length && type != null) {
                    last.put(null, type);
                }
            }
        }
        return last;
    }

    private Object getArrayIndex(String get, String type, LinkedHashMap<String, Object> in) {
        List<Integer> index = getIndexes(get);
        get = get.substring(0, get.indexOf("["));

        List<Object> list = (List<Object>) in.get(get);
        if(list == null) {
            list = new ArrayList<>();

            in.put(get, list);
        }

        for (int i = 0; i < index.size(); i++) {
            int currentIndex = index.get(i);

            if(currentIndex == -1) {
                if(list.size() == 1) {

                    if(i +1 == index.size()) {
                        list.add(null);
                    } else {
                        LinkedHashMap<Integer, Object> newMap = new LinkedHashMap<>();

                        LinkedHashMap<String, String> properties = new LinkedHashMap<>();

                        if(type != null) {
                            properties.put("typeObject", type);
                        }

                        newMap.put(null, properties);

                        list.add(newMap);
                    }
                }

                currentIndex = list.size() -1;
            } else if(currentIndex == -2) {
                currentIndex = list.size();

                list.add(new LinkedHashMap<String, Object>());

                if(type != null) {
                    ((LinkedHashMap<String, Object>) list.get(list.size() -1)).put(null, type);
                }
            }

            if(i +1 == index.size()) {
                return list.get(currentIndex);
            }

            list = (List<Object>) list.get(currentIndex);
        }
        throw new IllegalStateException();
    }

    private List<Integer> getIndexes(String get) {
        List<Integer> index = new ArrayList<>();

        while(get.contains("[")) {
            String currentIndex = get.substring(get.indexOf("["), get.indexOf("]") +1);
            get = get.replaceFirst("\\\\".substring(1) + currentIndex, "");

            currentIndex = currentIndex.substring(1, currentIndex.length() -1);
            int lastIndex = currentIndex.equals("") || currentIndex.equals("new") ? 0 :Integer.parseInt(currentIndex);
            if(lastIndex < 0) {
                throw new IllegalArgumentException();
            }

            index.add(currentIndex.equals("") ? -1 : currentIndex.equals("new") ? -2 : lastIndex);
        }
        return index;
    }

    private Object string2object(String value, LinkedHashMap<String, Object> root) {
        if(value.startsWith("[")) {
            return parseArray(value, root);
        } else {
            return parseValue(value, root);
        }
    }

    private Object parseValue(String value, LinkedHashMap<String, Object> root) {

        if (value.equals("null")) {
            return null;
        } else if (value.startsWith("\"")) {
            return value.substring(1, value.length() - 1);
        } else if (value.startsWith("'")) {
            if(value.length() == 3) {
                return value.charAt(1);
            }
            return (char) Integer.parseInt(value.substring(3, value.length() - 1));
        } else if (value.equals("false")) {
            return false;
        } else if (value.equals("true")) {
            return true;
        } else if (value.endsWith(".class")) {
            value = value.substring(0, value.length() -6);
            Class<?> clazz = getClassByName(value);
            return clazz == null ? getPrimitiveClassByName(value) : clazz;
        }

        if(value.matches("(-?[0-9]+)([iI])?")) {
            return Integer.parseInt(value);
        } else if(value.matches("(-?[0-9]+\\.?[0-9]+|[0-9]+)([dD])?")) {
            return Double.parseDouble(value);
        } else if(value.matches("(-?[0-9]+\\.?[0-9]+|[0-9]+)([fF])")) {
            return Float.parseFloat(value.substring(0, value.length() -1));
        } else if(value.matches("(-?[0-9]+)([sS])")) {
            return Short.parseShort(value.substring(0, value.length() -1));
        } else if(value.matches("(-?[0-9]+)([lL])")) {
            return Long.parseLong(value.substring(0, value.length() -1));
        } else if(value.matches("(-?[0-9]+)([bB])")) {
            return Byte.parseByte(value.substring(0, value.length() -1));
        }

        if(value.equals(".")) return root;

        Object objValue;

        if(value.contains(".")) {
            objValue = getHashMapObject(value.substring(0, value.lastIndexOf(".")), null, root);
        } else if(value.contains("[")) {
            objValue = getArrayIndex(value, null, root);
        } else {
            if(root.containsKey(value)) throw new IllegalArgumentException();

            objValue = root.get(value);
        }

        if(objValue != null && objValue.getClass().isArray()) {
            objValue = ((Object[]) objValue)[0];
        }

        return objValue;
    }

    private List<Object> parseArray(String value, LinkedHashMap<String, Object> root) {
        List<Object> values = new ArrayList<>();

        if(value.equals("[]")) return values;

        boolean isEscaped = false;
        boolean inString = false;
        int depth = 0;

        String currentValue = "";

        for (String c : value.substring(1, value.length() -1).split("")) {

            if (!isEscaped && c.equals("\"")) {
                inString = !inString;
            }

            isEscaped = c.equals("\\");

            if(!inString && c.equals(",") && depth == 0) {
                values.add(string2object(currentValue.trim(), root));
                currentValue = "";
                continue;
            }

            if(!inString && c.equals("[")) depth++;
            if(!inString && c.equals("]")) depth--;

            currentValue += c;
        }
        values.add(string2object(currentValue.trim(), root));
        return values;
    }
}
