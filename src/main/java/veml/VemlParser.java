package veml;

import java.util.*;

import static reflection.FieldReflection.getField;
import static reflection.FieldReflection.setFieldValue;

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
        static final int SYNTHETIC = 4096;
    }

    private boolean ignoreWrongNames = true;
    private int[] modifiers = new int[]{Modifier.STATIC, Modifier.SYNTHETIC};

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
            setFieldValue(getField(Exception.class, "detailMessage", true), e, "invalid veml");
            throw e;
        }
    }

    public List<String> stringify(Object instance) {
        return new ObjectToVeml(modifiers).parse(instance, "");
    }
}