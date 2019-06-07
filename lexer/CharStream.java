package lexer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

class CharStream {
    private final Reader reader;
    private Character cache;

    static CharStream from(String expr) { // static 선언으로 CharStream 유일성 확보
        return new CharStream(new StringReader(expr));
    }

    CharStream(Reader reader) {
        this.reader = reader;
        this.cache = null;
    }

    Char nextChar() {
        if (cache != null) { // cache가 있으면 Char로 만들어 리턴하고 cache null로 초기화
            char ch = cache;
            cache = null;

            return Char.of(ch);
        } else {
            try {
                int ch = reader.read(); // char 하나 읽어온다.
                if (ch == -1) { // eos
                    return Char.end();
                } else {
                    return Char.of((char) ch); // Char로 만들어 리턴
                }
            } catch (IOException e) {
                throw new ScannerException("" + e);
            }
        }
    }

    //void pushBack(char ch) { cache = ch; }
    // 캐쉬 설정할 때 쓰는데 지금 당장은 안 쓰이는 듯하므로 일단 주석처리
}
