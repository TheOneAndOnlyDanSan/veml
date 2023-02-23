import veml.VemlElement;

import java.util.concurrent.atomic.AtomicBoolean;

public class TestClass {
    int i = 1;
    short s = 1;
    long l = 1;
    char c = '1';
    float f = 1;
    double d = 1;
    byte b = 1;
    AtomicBoolean bool = new AtomicBoolean(true);
    Class<?> clazz = int.class;
    Object[] objects = new Object[]{};
    Object[] objects2 = new Index[] {
            new Index0(),
            null,
            new Index1()
    };
    Object object = objects2[1];

    TestClass() {
        objects2[1] = objects2[0];
        ((Index1) objects2[2]).obj = objects2;
    }

    public interface Index{

    };

    public static class Index0 implements Index {
        @VemlElement(comment = "comment") int i = 1;
        short s = 1;
        long l = 1;
        char c = '1';
        float f = 1;
        double d = 1;
        byte b = 1;
        boolean bool = true;
        Class<?> clazz = int.class;
        Object obj = null;
    }

    public static class Index1 implements Index {
        int i = 1;
        short s = 1;
        long l = 1;
        char c = '1';
        float f = 1;
        double d = 1;
        byte b = 1;
        boolean bool = true;
        Class<?> clazz = int.class;
        Object obj = null;
    }
}
