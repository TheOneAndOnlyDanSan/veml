package veml;

import reflection.ClassReflection;

import java.util.Arrays;
import java.util.LinkedHashMap;

class VemlObject extends LinkedHashMap<String, VemlElement> implements VemlElement {
    private Class<?> type;

    VemlObject(String type) {
        if(type == null) {
            this.type = null;
        } else {
            this.type = ClassReflection.getClassByName(type);
            if(this.type == null) throw new IllegalArgumentException();
        }
    }

    VemlObject() {

    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public VemlElement get(Object key) {
        return super.get(key);
    }

    @Override
    public VemlElement put(String key, VemlElement value) {
        if(isVariableName(key)) throw new IllegalArgumentException("" + key);

        if(super.containsKey(key)) throw new IllegalArgumentException(key);

        return super.put(key, value);
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
}
