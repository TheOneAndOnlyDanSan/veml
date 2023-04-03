import java.util.concurrent.atomic.AtomicBoolean;

abstract class TestClassSuperClass {
    int i = 11;
}

class TestClass2 {
    boolean[] boolArray = new boolean[]{true, true, false};
    double[] dblArray = new double[]{ 1, 2, 100000 };
    char[] charArray = new char[]{'a', 'f'};
    String name = "Rachel";
    AtomicBoolean bool = new AtomicBoolean(true);

    public TestClass2(String name) {
        this.name = name;
    }
}

class TestClass3 {
    AtomicBoolean bool;
    AtomicBoolean bool2 = null;
    AtomicBoolean bool3 = null;

    TestClass3(AtomicBoolean bool) {
        this.bool = bool;
    }

    TestClass3(AtomicBoolean bool, boolean a) {
        this.bool = bool;
        bool2 = bool;
        bool3 = bool;
    }
}

class TestClass4 {
    TestClass3 testClass3;

    TestClass4(TestClass3 testClass3) {
        this.testClass3 = testClass3;
    }
}

public class TestClass extends TestClassSuperClass {
    short s = 2;
    long l = 30;
    char c = 'e';
    float f = 3.3f;
    double d = 4.4;
    byte b = 5;

    int[] intArray = new int[]{1, 2, 3, 4};
    Object[] objArray = new Object[]{1, 2, 3, new TestClass2("violet")};
    TestClass t = this;
    TestClass2 z = new TestClass2("violet");
    TestClass2 x = (TestClass2) objArray[3];
    TestClass4 y = new TestClass4(new TestClass3(new AtomicBoolean(true), true));
}
