package veml;

public class VemlSyntaxException extends RuntimeException {

    VemlSyntaxException(String s) {
        super(s);
    }

    VemlSyntaxException(RuntimeException e, String s) {
        super(s, e);
        this.setStackTrace(e.getStackTrace());
    }
}
