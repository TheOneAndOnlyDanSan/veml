package veml;

import java.util.*;

public class VemlParser {

    public class Modifier {
        public static final int PACKAGE_PRIVATE = 0;
        public static final int PUBLIC = 1;
        public static final int PRIVATE = 2;
        public static final int PROTECTED = 4;
        public static final int STATIC = 8;
        public static final int FINAL = 16;
        public static final int VOLATILE  = 64;
        public static final int TRANSIENT = 128;
    }

    private boolean ignoreWrongNames = true;
    private int[] modifiers = new int[]{Modifier.STATIC};

    public VemlParser ignoreWrongNames(boolean ignoreWrongNames) {
        this.ignoreWrongNames = ignoreWrongNames;
        return this;
    }

    public VemlParser ignoreFieldsWithModifiers(int ... modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public <T> T parse(Class<T> clazz, List<String> veml) {
        try {
            return new VemlToObject(ignoreWrongNames, modifiers).parse(clazz, veml);
        } catch(Exception e) {
            String message = e.getMessage();

            if(message.equals("") || e.getClass() != IllegalArgumentException.class) throw new IllegalArgumentException("invalid veml");
            else throw e;
        }
    }

    public List<String> stringify(Object instance) {
        return new ObjectToVeml().parse(instance);
    }
}