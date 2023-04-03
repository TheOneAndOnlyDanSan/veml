package veml;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import static reflection.ClassReflection.getClassByName;
import static reflection.ClassReflection.getPrimitiveClassByName;
import static reflection.ConstructorReflection.createInstanceWithoutConstructor;
import static reflection.FieldReflection.getField;
import static reflection.FieldReflection.setFieldValue;

class VemlToObject {

    VemlObject Super = new VemlObject();
    VemlObject This = Super;
    IdentityHashMap<VemlElement, Object> identityHashMap = new IdentityHashMap<>();

    private <T> T super2clazzType(VemlObject object, Class<T> clazz) {
        T instance = (T) identityHashMap.get(object);

        if(instance == null) instance = createInstanceWithoutConstructor(clazz);
        if(!identityHashMap.containsKey(Super)) identityHashMap.put(Super, instance);

        for(String key : object.keySet()) {
            Field f = getField(clazz, key);

            if(key.equals("extends")) {
                identityHashMap.put(object.get("extends"), instance);
                super2clazzType((VemlObject) object.get("extends"), clazz.getSuperclass());
                continue;
            }

            if(f == null) continue;
            VemlElement value = object.get(key);

            Object objValue = null;
            if(value.getClass().equals(VemlPrimitive.class)) {
                objValue = ((VemlPrimitive) value).value;
                setFieldValue(f, instance, objValue);
            } else if(value.getClass().equals(VemlObject.class)) {
                Class<?> type = ((VemlObject) value).getType();
                type = type == null ? f.getType() : type;

                objValue = super2clazzType((VemlObject) value, type);
                setFieldValue(f, instance, objValue);
            } else if(value.getClass().equals(VemlList.class)) {
                Class<?> type = ((VemlList) value).getType();
                type = type == null ? f.getType().getComponentType() : type;

                objValue = list2array((VemlList) value, type);
                setFieldValue(f, instance, objValue);
            } else if(value.getClass().equals(VemlReference.class)) {
                objValue = identityHashMap.get(((VemlReference) value).value);
                setFieldValue(f, instance, objValue);
            }

            identityHashMap.put(value, objValue);
        }

        return instance;
    }

    private <T> Object list2array(VemlList list, Class<T> clazz) {
        Object array = Array.newInstance(clazz, list.size());

        for (int i = 0;i < list.size();i++) {
            VemlElement value = list.get(i);

            Object objValue = null;
            if(value.getClass().equals(VemlPrimitive.class)) {
                objValue = ((VemlPrimitive) value).value;
                Array.set(array, i, objValue);
            } else if (value.getClass().equals(VemlObject.class)) {
                VemlObject objectValue = (VemlObject) value;
                Class<?> type = clazz;

                if(objectValue.getType() != null) {
                    type = objectValue.getType();
                }

                objValue = super2clazzType((VemlObject) value, type);
                Array.set(array, i, objValue);
            } else if(value.getClass().equals(VemlList.class))  {
                VemlList listValue = (VemlList) value;
                Class<?> type = clazz.getComponentType();

                if(listValue.getType() != null) {
                    type = listValue.getType();
                }

                objValue = list2array(listValue, type);
                Array.set(array, i, objValue);
            } else if(value.getClass().equals(VemlReference.class)) {
                objValue = identityHashMap.get(((VemlReference) value).value);
                Array.set(array, i, objValue);
            }

            identityHashMap.put(value, objValue);
        }
        return array;
    }

    public <T> T parse(List<String> veml, Class<T> clazz) {

        for(String line : veml) {
            line = removeComments(line).trim();
            if(line.equals("")) continue;

            String key = line.split("=")[0];
            String value;

            if(!key.equals(line)) {
                value = line.replace(key + "=", "");
                key = key.trim();

                if(key.startsWith("{")) {
                    if(key.contains(".")) {
                        String[] splitPath = key.split("\\.");
                        String get = key.replace("." + splitPath[splitPath.length - 1], "");
                        String last = splitPath[splitPath.length - 1];

                        key = key.substring(1, key.length() - 1);
                        getObject(get, false).put(last, string2value(value.trim()));
                    } else {
                        Super.put(key, string2value(value.trim()));
                    }
                    This = Super;
                } else if(key.startsWith("[")) {
                    key = key.substring(1, key.length() - 1);
                    getList(key, false).add(string2value(value.trim()));
                    This = Super;
                } else {
                    if(key.contains(".")) {
                        String[] splitPath = key.split("\\.");
                        String get = key.replace("." + splitPath[splitPath.length - 1], "");
                        String last = splitPath[splitPath.length - 1];

                        getObject(get, false).put(last, string2value(value.trim()));
                    } else {
                        This.put(key, string2value(value.trim()));
                    }
                }
            } else {
                changeRootObject(key);
            }
        }
        return super2clazzType(Super, clazz);
    }

    private void changeRootObject(String newRoot) {
        if(newRoot.equals("")) {
            This = Super;
            return;
        }

        String clazz = newRoot.substring(newRoot.lastIndexOf(newRoot.startsWith("[") ? "]" : newRoot.startsWith("{") ? "}" : "|") +1);
        newRoot = newRoot.substring(0, newRoot.length() - clazz.length()).trim();
        clazz = clazz.trim();
        if(clazz.equals("")) clazz = null;
        String type = newRoot.substring(0, 1);
        newRoot = newRoot.substring(1, newRoot.length() -1);

        if(type.equals("[")) {
            VemlList object = getList(newRoot, false);
            object.add(new VemlObject(clazz));

            This = (VemlObject) object.get(object.size() -1);
        } else if(newRoot.contains(".")) {
            String[] splitPath = newRoot.split("\\.");
            String get = newRoot.replace("." + splitPath[splitPath.length -1], "");
            String last = splitPath[splitPath.length -1];

            if(type.equals("{")) {
                VemlObject object = getObject(get, false);
                object.put(last, new VemlObject(clazz));

                This = (VemlObject) object.get(last);
            } else if(type.equals("|")) {
                VemlList list = getList(newRoot, false);
                list.setType(clazz);
            }
        } else {

            if(type.equals("{")) {
                Super.put(newRoot, new VemlObject(clazz));
                This = (VemlObject) Super.get(newRoot);
            } else if(type.equals("|")) {
                VemlList list = getList(newRoot, false);
                list.setType(clazz);
            }
        }
    }

    private VemlList getList(String path, boolean parsingValue) {
        String[] splitPath = path.split("\\.");
        VemlObject current = Super;
        if(path.startsWith("this.") || path.startsWith("extends")) current = This;

        if(splitPath.length != 1) {
            current = getObject(path.replace("." + splitPath[splitPath.length -1], ""), parsingValue);
        }

        String key = splitPath[splitPath.length -1];
        String realKey = key;

        if(key.contains("[")) {
            key = key.substring(0, key.indexOf("["));
        }

        if(current.containsKey(key)) {
            VemlList list = (VemlList) current.get(key);

            if(realKey.contains("[")) {
                return (VemlList) getFromIndex(list, realKey, parsingValue, true);
            } else {
                return list;
            }

        } else {
            if(!parsingValue) {
                VemlList list = new VemlList();
                current.put(key, list);
                return list;
            }
        }
        throw new IllegalStateException();
    }

    private VemlObject getObject(String path, boolean parsingValue) {
        if(path.equals("super")) return Super;
        if(path.equals("this")) return This;

        VemlObject current = Super;

        if(path.startsWith("this.") || path.startsWith("extends")) current = This;

        String[] splitPath = path.split("\\.");

        for(String key : splitPath) {
            if(current.containsKey(key)) {
                VemlElement value = current.get(key);

                if(value.getClass().equals(VemlObject.class)) {
                    current = (VemlObject) value;
                } else if(value.getClass().equals(VemlList.class)) {
                    current = (VemlObject) getFromIndex((VemlList) value, key, parsingValue, false);
                } else if(value.getClass().equals(VemlReference.class)) {
                    value = ((VemlReference) value).value;
                } else {
                    throw new IllegalStateException();
                }
            } else {
                if(key.contains("[")) {
                    VemlList nextList = new VemlList();
                    current.put(key, nextList);

                    current = (VemlObject) getFromIndex(nextList, key, parsingValue, false);
                } else {
                    VemlObject nextObject = new VemlObject();
                    current.put(key, nextObject);
                    current = nextObject;
                }
            }
        }
        return current;
    }

    private VemlElement getFromIndex(VemlList list, String key, boolean parsingValue, boolean isLastIndexList) {
        List<String> indexes = getIndexes(key);
        if(indexes.size() == 0) return list;

        VemlElement value = list;
        for(int i = 0;i < indexes.size();i++) {
            String index = indexes.get(i);

            if(index.equals("")) {
                if(parsingValue) {
                    value = list.get(list.size() -1);
                } else {
                    if(i +1 == indexes.size()) {
                        VemlElement next = null;
                        if(isLastIndexList) {
                            next = new VemlList();
                        } else {
                            next = new VemlObject();
                        }

                        list.add(next);
                        value = next;
                    } else {
                        VemlList nextList = new VemlList();
                        list.add(nextList);
                        value = nextList;
                    }
                }
            } else {
                int indexNum = Integer.parseInt(index);
                if(list.size() > indexNum) {
                    value = list.get(indexNum);
                }
            }

            if(value.getClass().equals(VemlList.class)) {
                list = (VemlList) value;
            } else if(value.getClass().equals(VemlReference.class)) {
                list = (VemlList) ((VemlReference) value).value;
            } else {
                throw new IllegalStateException();
            }
        }

        return value;
    }

    private List<String> getIndexes(String key) {
        List<String> indexes = new ArrayList<>();

        while(key.contains("[")) {
            String currentIndex = key.substring(key.indexOf("["), key.indexOf("]") +1);
            key = key.replaceFirst("\\\\".substring(1) + currentIndex, "");

            currentIndex = currentIndex.substring(1, currentIndex.length() -1);
            indexes.add(currentIndex);
        }
        return indexes;
    }

    private String removeComments(String line) {
        boolean inString = false;
        boolean commentStart = false;

        for(int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if(c == '"') inString = !inString;

            if(commentStart) {
                if(c == '\\') return line.substring(0, i-1);
                else commentStart = false;
            }

            if(!inString && c == '\\') {
                commentStart = true;
            }
        }
        return line;
    }

    private VemlElement string2value(String value) {
        if(value.startsWith("[")) return parseArray(value);
        else return parseValue(value);
    }

    private VemlElement parseValue(String value) {

        if(value.equals("null")) {
            return new VemlPrimitive(null);
        } else if (value.startsWith("\"")) {
            return new VemlPrimitive(value.substring(1, value.length() - 1));
        } else if (value.startsWith("'")) {
            if(value.length() == 3) {
                return new VemlPrimitive(value.charAt(1));
            }
            return new VemlPrimitive((char) Integer.parseInt(value.substring(3, value.length() - 1)));
        } else if (value.equals("false")) {
            return new VemlPrimitive(false);
        } else if (value.equals("true")) {
            return new VemlPrimitive(true);
        } else if (value.endsWith(".class")) {
            value = value.substring(0, value.length() -6);
            Class<?> clazz = getClassByName(value);
            return new VemlPrimitive(clazz == null ? getPrimitiveClassByName(value) : clazz);
        }

        if(value.matches("(-?[0-9]+)([iI])?")) {
            return new VemlPrimitive(Integer.parseInt(value.toLowerCase().contains("i") ? value.substring(0, value.length() -1) : value));
        } else if(value.matches("(-?[0-9]+\\.?[0-9]+|[0-9]+)([dD])?")) {
            return new VemlPrimitive(Double.parseDouble(value.toLowerCase().contains("d") ? value.substring(0, value.length() -1) : value));
        } else if(value.matches("(-?[0-9]+\\.?[0-9]+|[0-9]+)([fF])")) {
            return new VemlPrimitive(Float.parseFloat(value.substring(0, value.length() -1)));
        } else if(value.matches("(-?[0-9]+)([sS])")) {
            return new VemlPrimitive(Short.parseShort(value.substring(0, value.length() -1)));
        } else if(value.matches("(-?[0-9]+)([lL])")) {
            return new VemlPrimitive(Long.parseLong(value.substring(0, value.length() -1)));
        } else if(value.matches("(-?[0-9]+)([bB])")) {
            return new VemlPrimitive(Byte.parseByte(value.substring(0, value.length() -1)));
        }

        if(value.contains(".")) {
            String[] splitPath = value.split("\\.");
            String get = value.replace("." + splitPath[splitPath.length -1], "");
            String last = splitPath[splitPath.length -1];

            try {
                VemlObject obj = getObject(get, true);
                return new VemlReference(obj.get(last));
            } catch(Exception e) {
                return new VemlReference( getFromIndex(getList(get, true), last, true, false));
            }

        } else {
            try {
                return new VemlReference(getObject(value, true));
            } catch(Exception e) {
                return new VemlReference(getList(value, true));
            }
        }
    }

    private VemlElement parseArray(String value) {
        VemlList list = new VemlList();
        value = value.substring(1);

        String index = "";
        boolean inString = false;
        int depth = 0;

        for(int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if(c == '"') inString = !inString;

            if(!inString) {
                if(c == ',' && depth == 0) {
                    list.add(string2value(index.trim()));
                    index = "";
                    continue;
                } else if(c == '[') {
                    depth++;
                } else if(c == ']') {
                    depth--;
                    if(depth == -1) {
                        list.setType(value.substring(i +1).trim());
                        list.add(string2value(index.trim()));
                        break;
                    }
                }
            }

            index += c;
        }

        return list;
    }
}
