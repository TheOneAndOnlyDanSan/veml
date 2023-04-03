package testclasses;

public class TestClass1 {
    int i;
    double d;
    long l;
    float f;
    byte b;
    short s;
    boolean bool;
    char c;
    Class<?> clazz;
    Number[][] arr;
    TestClass2 testClass2;

    public TestClass1(int i, double d, long l, float f, byte b, short s, boolean bool, char c, Class<?> clazz, Number[][] arr) {
        this.i = i;
        this.d = d;
        this.l = l;
        this.f = f;
        this.b = b;
        this.s = s;
        this.bool = bool;
        this.c = c;
        this.clazz = clazz;
        this.arr = arr;
    }

    public void setTestClass2(TestClass2 testClass2) {
        this.testClass2 = testClass2;
    }
}
