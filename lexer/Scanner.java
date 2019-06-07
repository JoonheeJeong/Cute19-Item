package lexer;

import java.util.Iterator;

public class Scanner { // TokenIterator를 Stream으로 처리해서 리턴하기 위한 클래스
    // return tokens as an Iterator
    public static Iterator<Token> scan(String expr) {
        ScanContext context = new ScanContext(expr);
        return new TokenIterator(context);
    }
}