import org.junit.jupiter.api.Test;
import testclasses.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static veml.VemlParser.parse;

public class ParseTest {

    @Test
    public void Test1() {
        TestClass1 obj = new TestClass1(1, 2.0, 3L, 4F, (byte) 5, (short) 6, false, '\u0000', int.class, new Number[][]{new Integer[]{1,2,3}, new Long[]{4L,5L,6L}});
        obj.setTestClass2(new TestClass2(obj, new TestClass[]{new TestClass(1), new TestClass4(1, 2, 3), new TestClass3(1, 2)}));

        List<String> veml = List.of(
                "i = 1",
                "d = 2.0",
                "l = 3l",
                "f = 4f",
                "b = 5b",
                "s = 6s",
                "bool = false",
                "c = '\u0000'",
                "clazz = int.class",
                "|arr| java.lang.Number[]",
                "[arr] = [1,2,3] java.lang.Integer",
                "[arr[]] = 4l",
                "[arr[1]] = 5l",
                "[arr[1]] = 6l",
                "|arr[1]| java.lang.Long",
                "{testClass2} testclasses.TestClass2",
                "testClass = super",
                "|this.objArr| testclasses.TestClass",
                "[testClass2.objArr]",
                "i = 1",
                "[testClass2.objArr] testclasses.TestClass4",
                "extends.extends.i = 3",
                "extends.i = 2",
                "i = 1",
                "[testClass2.objArr] testclasses.TestClass3",
                "extends.i = 2",
                "i = 1"
        );

        assertTrue(DeepEquals.deepEquals(parse(veml, TestClass1.class), obj));
    }
}
