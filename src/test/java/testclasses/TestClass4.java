package testclasses;

public class TestClass4 extends TestClass3 {
    public int i;

    public TestClass4(int i, int superI, int superSuperI) {
        super(superI, superSuperI);
        this.i = i;
    }
}
