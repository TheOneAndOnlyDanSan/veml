package veml;

import java.util.List;

public class VemlParser {
    public static <T> T parse(List<String> veml, Class<T> clazz) {
        return new VemlToObject().parse(veml, clazz);
    }
}
