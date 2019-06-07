package lexer;

public class ScannerException extends RuntimeException { // 예외 클래스.
    private static final long serialVersionUID = -5564986423129197718L;

    public ScannerException() {
        super();
    }

    public ScannerException(String details) {
        super(details);
    }
}
