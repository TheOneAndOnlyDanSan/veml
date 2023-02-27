package veml;

import java.util.*;

public class VemlParser {

    /**
     * @see java.lang.reflect.Modifier
     */
    public static abstract class Modifier {
        public static final int PACKAGE_PRIVATE = 0;
        public static final int PUBLIC = 1;
        public static final int PRIVATE = 2;
        public static final int PROTECTED = 4;
        public static final int STATIC = 8;
        public static final int FINAL = 16;
        public static final int VOLATILE  = 64;
        public static final int TRANSIENT = 128;
        public static final int SYNTHETIC = 4096;
    }

    private boolean ignoreWrongNames = true;
    private int[] modifiers = new int[]{Modifier.STATIC, Modifier.TRANSIENT, Modifier.SYNTHETIC};

    public VemlParser ignoreWrongNames(boolean ignoreWrongNames) {
        this.ignoreWrongNames = ignoreWrongNames;
        return this;
    }

    /**
     * always ignores trusted final fields
     */
    public VemlParser ignoreFieldsWithModifiers(int... modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public LinkedHashMap<String, Object> parse(List<String> veml) {
        return (LinkedHashMap<String, Object>) new VemlToObject(ignoreWrongNames, modifiers).parse(null, veml);
    }

    public <T> T parse(Class<T> clazz, List<String> veml) {
        return (T) new VemlToObject(ignoreWrongNames, modifiers).parse(clazz, veml);
    }

    public List<String> stringify(Object instance) {
        return new ObjectToVeml(modifiers).parse(instance, "");
    }
}