package veml;

import java.lang.reflect.Modifier;
import java.util.*;

public class VemlParser {

    private boolean ignoreWrongNames = true;
    private int modifiers = Modifier.STATIC;

    public VemlParser ignoreWrongNames(boolean ignoreWrongNames) {
        this.ignoreWrongNames = ignoreWrongNames;
        return this;
    }

    public VemlParser ignoreFieldsWithModifiers(int ... modifiers) {
        this.modifiers = Arrays.stream(modifiers).sum();
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