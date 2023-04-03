package veml;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static reflection.ClassReflection.getClassByName;
import static reflection.ClassReflection.getPrimitiveClassByName;
import static reflection.FieldReflection.*;
import static reflection.ConstructorReflection.createInstanceWithoutConstructor;
import static reflection.FieldReflection.setFieldValue;

class VemlToObject {

    LinkedHashMap<String, Object> root = new LinkedHashMap<>();

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

    private Object getAsObject(LinkedHashMap<String, Object> root) {
        root.remove(null);
        for(Map.Entry<String, Object> e : root.entrySet()) {
            Object o = e.getValue();
            String k = e.getKey();
            if(o instanceof LinkedHashMap<?,?>) {
                root.put(k, getAsObject((LinkedHashMap<String, Object>) o));
            } else if(o.getClass().isArray()) {
                root.put(k, ((Object[]) o)[0]);
            } else if(o instanceof ArrayList<?>) {
                root.put(k, removeFirstIndex((ArrayList<Object>) o));
            }
        }
        return root;
    }

    private List<Object> removeFirstIndex(ArrayList<Object> list) {
        list.remove(0);
        return list.stream().map(v -> {
            if(v.getClass() == ArrayList.class) return removeFirstIndex((ArrayList<Object>) v);
            if(v.getClass() == LinkedHashMap.class) return getAsObject((LinkedHashMap<String, Object>) v);
            if(v.getClass().isArray()) return ((Object[]) v)[0];
            return v;
        }).collect(Collectors.toList());
    }

    private <T> T map2object(Class<T> clazz, LinkedHashMap<String, Object> hashMap, IdentityHashMap<Object, Object> hashmap2existingObjects) {
        if(clazz == null || clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) throw new IllegalArgumentException("invalid class");

        T instance = createInstanceWithoutConstructor(clazz);
        if(!hashmap2existingObjects.containsKey(root)) hashmap2existingObjects.put(root, instance);

        List<String> keys = hashMap.keySet().stream().filter(Objects::nonNull).toList();
        HashMap<String, Field> fields = new HashMap<>();

        List<Field> nonFinalFields = Arrays.stream(getFields(clazz, true)).filter(f -> !(Boolean) getFieldValue(getField(Field.class, "trustedFinal"), f)).toList();

        for(Field field : nonFinalFields) {
            VemlElement element = field.getAnnotation(VemlElement.class);
            if(element != null) {
                if(fields.containsKey(element.name()) || fields.containsValue(field)) {
                    throw new IllegalArgumentException();
                }
                if(!element.name().equals("")) {
                    fields.put(element.name(), field);
                }
            }
        }

        for(Field field : nonFinalFields) {
            String name = field.getName();

            if((fields.containsKey(name) || fields.containsValue(field)) && !field.isAnnotationPresent(VemlElement.class)) {
                throw new IllegalArgumentException();
            }

            if(!fields.containsValue(field)) fields.put(name, field);
        }

        for(String key : keys) {
            Object value = hashMap.get(key);
            Class<?> valueType = value == null ? null : value.getClass();

            Field f = fields.get(key);

            if(f == null || Arrays.stream(modifiers).anyMatch(modifier -> modifier == 0 && (f.getModifiers()&(Modifier.PUBLIC|Modifier.PRIVATE|Modifier.PROTECTED)) == 0 || (modifier&f.getModifiers()) != 0)) {
                if(ignoreWrongNames) continue;
                else throw new IllegalArgumentException();
            }

            VemlElement element = f.getAnnotation(VemlElement.class);
            if(element != null && element.ignore()) {
                continue;
            }

            Class<?> fieldType = f.getType();

            if(isPrimitive(fieldType, value)) {
                setFieldValue(f, instance, value);
                hashmap2existingObjects.put(value, value);
            } else if(valueType != null && valueType.isArray()) {
                setFieldValue(f, instance, hashmap2existingObjects.get(((Object[]) value)[0]));
            } else if(valueType.equals(ArrayList.class)) {

                Class<?> type = getClassByName((String) ((ArrayList<?>) value).get(0));

                Object objValue = list2array(type == null ? fieldType.getComponentType() : type, (List<?>) value, hashmap2existingObjects);

                setFieldValue(f, instance, objValue);
                hashmap2existingObjects.put(value, objValue);
            } else if(valueType.equals(LinkedHashMap.class)) {
                LinkedHashMap<String, Object> map = ((LinkedHashMap<String, Object>) value);
                String objectType = (String) map.get(null);

                Class<?> type = fieldType;
                if(objectType != null) {
                    type = getClassByName(objectType.substring(0, objectType.length() -6));
                }

                Object objValue = map2object(type, map, hashmap2existingObjects);

                setFieldValue(f, instance, objValue);
                hashmap2existingObjects.put(value, objValue);
            }
        }
        return instance;
    }

    private Object list2array(Class<?> type, List<?> list, IdentityHashMap<Object, Object> hashmap2existingObjects) {
        int size = list.size() -1;
        Object array = Array.newInstance(type, size);
        hashmap2existingObjects.put(list, array);
        for (int i = 1; i < size +1; i++) {
            Object value = list.get(i);

            if (value instanceof List) {
                Class<?> newType = getClassByName((String) ((ArrayList<?>) value).get(0));

                Object objValue = list2array(newType == null ? type : newType, (List<?>) value, hashmap2existingObjects);

                Array.set(array, i -1, objValue);
                hashmap2existingObjects.put(value, objValue);
            } else if(value instanceof HashMap<?,?>) {
                LinkedHashMap<String, Object> map = ((LinkedHashMap<String, Object>) value);
                String objectType = (String) map.get(null);

                if(objectType != null) {
                    type = getClassByName(objectType);
                }

                Object objValue = map2object(type, map, hashmap2existingObjects);

                Array.set(array, i -1, objValue);
                hashmap2existingObjects.put(value, objValue);
            } else if(value != null && value.getClass().isArray()) {
                Array.set(array, i -1, hashmap2existingObjects.get(((Object[]) value)[0]));
            } else {
                Array.set(array, i -1, value);
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

    private List<Token> getTokens(Collection<String> veml) {

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
                            if(!finishedKey) {
                                finishedKey = true;
                                continue;
                            }
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
                            } else if(objDepth == 0 && key.equals("")) {
                                objDepth++;
                                inObjectArray = true;
                                finishedObjectArray = false;
                            } else if(!finishedObjectArray) {
                                objDepth++;
                            }
                        }
                        case "]" -> {
                            if(finishedKey) {
                                depth--;
                            } else if(!finishedObjectArray && !inObject) {
                                objDepth--;
                                if(objDepth == 0) {
                                    finishedKey = true;
                                    finishedObjectArray = true;
                                    continue;
                                }
                            }
                        }
                        case "|" -> {
                            if(finishedObjectArray) {
                                objDepth++;
                                inObjectArray = true;
                                finishedObjectArray = false;
                            } else {
                                objDepth--;
                                if(objDepth == 0) {
                                    finishedKey = true;
                                    finishedObjectArray = true;
                                    key += "|";
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
                    tokens.add(new Token(Token.TYPE.objectArray, key.substring(1) + (value.equals("") ? "" : "-" + value)));
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

    Object parse(Class<?> clazz, Collection<String> veml) {
        try {
            Iterator<Token> tokens = getTokens(veml).iterator();

            LinkedHashMap<String, Object> currentObj = root;

            while(tokens.hasNext()) {
                Token token = tokens.next();

                String value = token.value;

                switch(token.type) {
                    case keyValuePair -> {
                        String key = value.substring(0, value.indexOf("="));
                        value = value.substring(value.indexOf("=") + 1);

                        if(key.contains("[l]")) throw new IllegalArgumentException();

                        if(value.contains("[]")) value = value.replaceAll("\\[]", "[l]");

                        LinkedHashMap<String, Object> getFrom = currentObj;

                        if(key.contains(".")) {
                            getFrom = (LinkedHashMap<String, Object>) getHashMapObject(key.substring(0, key.lastIndexOf(".")), null, currentObj, true);
                            key = key.substring(key.lastIndexOf(".") + 1);
                        }

                        LinkedHashMap<String, Object> addTo = getFrom;

                        if(key.endsWith("[]")) {
                            addTo = (LinkedHashMap<String, Object>) getArrayIndex(key, null, getFrom);
                            ArrayList<Object> list = (ArrayList<Object>) getHashMapObject(key.replaceAll("\\[]", ""), null, currentObj, true);
                            list.set(list.size() -1, string2object(value, value.contains("this") ? currentObj : root));
                        } else {
                            addTo.put(key, string2object(value, value.contains("this") ? currentObj : root));
                        }
                    }
                    case objectArray -> {
                        if(value.contains("[l]")) throw new IllegalArgumentException();

                        String type = value.contains("-") ? value.substring(value.indexOf("-") + 1).trim() : null;

                        if(value.contains("|")) {
                            String key = value.substring(0, value.indexOf("|"));

                            LinkedHashMap<String, Object> getFrom = root;

                            if(key.contains(".")) {
                                getFrom = (LinkedHashMap<String, Object>) getHashMapObject(key.substring(0, key.lastIndexOf(".")), null, currentObj, true);
                                key = key.substring(key.lastIndexOf(".") + 1);
                            }

                            List<Object> addTo;

                            if(key.contains("[]")) {
                                addTo = (ArrayList<Object>) getArrayIndex(key, null, getFrom);
                                ArrayList<Object> list = (ArrayList<Object>) getHashMapObject(key.replaceAll("\\[]", ""), null, currentObj, true);
                                list.set(list.size() - 1, string2object(value, value.contains("this") ? currentObj : root));
                            } else {
                                if(!getFrom.containsKey(key)) {
                                    getFrom.put(key, new ArrayList<>());
                                }

                                addTo = (ArrayList<Object>) getFrom.get(key);

                                if(addTo.size() > 0 && addTo.get(0) != null) throw new IllegalArgumentException();

                                if(addTo.size() == 0) addTo.add(null);

                                addTo.set(0, type);
                            }
                            continue;
                        }

                        currentObj = root;

                        if(type == null) {
                            currentObj = (LinkedHashMap<String, Object>) getHashMapObject(value + "[]", null, currentObj, true);
                        } else if(type.contains("=")) {
                            type = type.substring(1).trim();
                            String key = value.substring(0, value.indexOf("-"));

                            LinkedHashMap<String, Object> getFrom = root;

                            if(key.contains(".")) {
                                getFrom = (LinkedHashMap<String, Object>) getHashMapObject(key.substring(0, key.lastIndexOf(".")), null, currentObj, true);
                                key = key.substring(key.lastIndexOf(".") + 1);
                            }

                            List<Object> addTo;

                            if(!getFrom.containsKey(key)) {
                                getFrom.put(key, new ArrayList<>());
                                ((ArrayList<Object>) getFrom.get(key)).add(type);
                            }

                            addTo = (ArrayList<Object>) getFrom.get(key);

                            addTo.add(string2object(type, type.contains("this") ? currentObj : root));

                        } else {
                            currentObj = (LinkedHashMap<String, Object>) getHashMapObject(value.substring(0, value.indexOf("-")) + "[]", type, currentObj, true);
                        }
                    }
                    case object -> {
                        if(value.contains("[l]")) throw new IllegalArgumentException();
                        String type = value.contains("-") ? value.substring(value.indexOf("-") + 1).trim() : null;

                        if(type == null) {
                            currentObj = (LinkedHashMap<String, Object>) getHashMapObject(value, null, currentObj, true);
                        } else if(type.contains("=")) {
                            String key = value.substring(0, value.indexOf("=") -1);
                            type = type.substring(1).trim();

                            LinkedHashMap<String, Object> getFrom = root;

                            String[] keys = key.split("\\.");

                            if(key.contains(".")) {

                                key = "";
                                for(int i = 0; i < keys.length -2; i++) {
                                    key += "." + keys[i];
                                }
                                key = key.substring(1);

                                getFrom = (LinkedHashMap<String, Object>) getHashMapObject(key, null, currentObj, true);

                                key = keys[keys.length -2];
                            }

                            LinkedHashMap<String, Object> addTo;

                            if(key.contains("[]")) {
                                addTo = (LinkedHashMap<String, Object>) getArrayIndex(key, null, getFrom);
                                ArrayList<Object> list = (ArrayList<Object>) getHashMapObject(key.replaceAll("\\[]", ""), null, currentObj, true);
                                list.set(list.size() - 1, string2object(type, type.contains("this") ? currentObj : root));
                            } else {
                                if(!getFrom.containsKey(key)) {
                                    getFrom.put(key, new LinkedHashMap<>());
                                }

                                addTo = (LinkedHashMap<String, Object>) getFrom.get(key);

                                addTo.put(keys.length == 1 ? key : keys[keys.length - 1], string2object(type, type.contains("this") ? currentObj : root));
                            }
                        } else {
                            currentObj = (LinkedHashMap<String, Object>) getHashMapObject(value.substring(0, value.indexOf("-")), type, currentObj, true);
                        }
                    }
                }
            }
        } catch(RuntimeException e) {

            String message = (String) getFieldValue(getField(Exception.class, "detailMessage", true), e);

            if(message != null) {
                message = message.substring(message.indexOf("") +1);
            }

            VemlSyntaxException v = new VemlSyntaxException(e, "invalid veml" + (message == null || !message.equals("") ? "" : ": " + message));

            throw v;
        }

        if(clazz == null) return getAsObject(root);
        return map2object(clazz, root, new IdentityHashMap<>());
    }

    private Object getHashMapObject(String get, String type, LinkedHashMap<String, Object> current, boolean create) {
        if(get.equals("") || get.equals("[]")) return root;

        LinkedHashMap<String, Object> last = root;

        String[] names = get.split("\\.");
        int start = 0;

        if(names[0].equals("this")) {
            last = current;
            start++;
        }

        for(int i = start;i < names.length;i++) {
            String name = names[i];

            if(name.contains("[")) {

                LinkedHashMap<String, Object> tempMap = (LinkedHashMap<String, Object>) getArrayIndex(name, i +1 == names.length ? type : null, last);
                if(tempMap == null) {
                    if(!create) throw new IllegalArgumentException();

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
                Object tmp = last.get(name);
                if(tmp.getClass().isArray()) {
                    tmp = ((Object[]) tmp)[0];
                }

                if(tmp instanceof ArrayList<?>) {
                    if(i +1 != names.length) throw new IllegalStateException();
                    return tmp;
                }

                last = (LinkedHashMap<String, Object>) tmp;
            } else {
                if(!create) throw new IllegalArgumentException();

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
            if(index.get(0) == -2) {
                list = new ArrayList<>();
                list.add(type == null ? "null" : type);

                in.put(get, list);
            } else if(index.get(0) != -2) {
                throw new IllegalArgumentException();
            }
        }

        for (int i = 0; i < index.size(); i++) {
            int currentIndex = index.get(i);

            if(currentIndex == -1) {
                currentIndex = list.size() -1;
            } else if(currentIndex == -2) {
                currentIndex = list.size();

                if(i +1 == index.size()) {
                    list.add(new LinkedHashMap<String, Object>());

                    if(type != null) {
                        ((LinkedHashMap<String, Object>) list.get(list.size() -1)).put(null, type);
                    }
                } else {
                    list.add(new ArrayList<>());
                }
            }

            if(i +1 == index.size()) {
                if(currentIndex == list.size()) {
                    list.add(new LinkedHashMap<String, Object>());
                }

                return list.get(currentIndex);
            }

            if(currentIndex == list.size()) {
                list.add(new ArrayList<>());
                ((ArrayList<Object>) list.get(list.size() -1)).add("null");
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
            int lastIndex = currentIndex.equals("l") || currentIndex.equals("") ? 0 :Integer.parseInt(currentIndex);
            if(lastIndex < 0) {
                throw new IllegalArgumentException();
            }

            lastIndex = currentIndex.equals("l") ? -1 : lastIndex;
            lastIndex = currentIndex.equals("") ? -2 : lastIndex;

            if(lastIndex >= 0) {
                lastIndex++;
            }

            index.add(lastIndex);
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

    private Object parseValue(String value, LinkedHashMap<String, Object> currentObj) {

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
            return Integer.parseInt(value.toLowerCase().contains("i") ? value.substring(0, value.length() -1) : value);
        } else if(value.matches("(-?[0-9]+\\.?[0-9]+|[0-9]+)([dD])?")) {
            return Double.parseDouble(value.toLowerCase().contains("d") ? value.substring(0, value.length() -1) : value);
        } else if(value.matches("(-?[0-9]+\\.?[0-9]+|[0-9]+)([fF])")) {
            return Float.parseFloat(value.substring(0, value.length() -1));
        } else if(value.matches("(-?[0-9]+)([sS])")) {
            return Short.parseShort(value.substring(0, value.length() -1));
        } else if(value.matches("(-?[0-9]+)([lL])")) {
            return Long.parseLong(value.substring(0, value.length() -1));
        } else if(value.matches("(-?[0-9]+)([bB])")) {
            return Byte.parseByte(value.substring(0, value.length() -1));
        }

        if(value.equals("super")) return new Object[]{root};
        if(value.equals("this")) return new Object[]{currentObj};

        Object objValue;

        if(value.contains(".")) {
            objValue = getHashMapObject(value, null, currentObj, false);
        } else if(value.contains("[")) {
            objValue = getArrayIndex(value, null, currentObj);
        } else {
            if(!currentObj.containsKey(value)) throw new IllegalArgumentException();

            objValue = currentObj.get(value);
        }

        if(objValue != null && objValue.getClass().isArray()) {
            return objValue;
        } else {
            objValue = new Object[]{objValue};
        }

        return objValue;
    }

    private List<Object> parseArray(String value, LinkedHashMap<String, Object> root) {
        List<Object> values = new ArrayList<>();

        if(value.matches("\\[.*?].+")) {
            values.add(value.substring(value.indexOf("]") +1).trim());
            value = value.replace(value.substring(value.indexOf("]") +1), "");
        } else {
            values.add(null);
        }

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