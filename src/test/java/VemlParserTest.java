import org.junit.jupiter.api.Test;
import veml.VemlParser;

import java.util.List;

public class VemlParserTest {

    @Test
    public void test() {
        TestClass root = new TestClass();

        VemlParser parser = new VemlParser().ignoreFieldsWithModifiers();

        System.out.println();
        parser.stringify(root).forEach(System.out::println);
        System.out.println();

        parser.parse(parser.stringify(root));

        List<String> veml = parser.stringify(root);

        parser.stringify(DeepEquals.objToMap(root));

//        Object obj1 = DeepEquals.objToMap(root);
//        Object obj2 = parser.parse(veml);
//
//        assertTrue(DeepEquals.deepEquals(obj1, obj2));
    }
}
