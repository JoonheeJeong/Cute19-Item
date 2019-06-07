package lexer;

class ScanContext { //charstream을 빌드해서 최종적으로 lexeme을 얻어내기 위한 클래스
    private final CharStream input;
    private StringBuilder builder;

    ScanContext(String expr) {
        this.input = CharStream.from(expr);
        this.builder = new StringBuilder();
    }

    CharStream getCharStream() {
        return input;
    }

    String getLexeme() { // 한 번 쓰면 초기화됨
        String str = builder.toString();
        builder.setLength(0);
        return str;
    }

    void append(char ch) { // 빌더에 갖다 붙이기
        builder.append(ch);
    }
}
