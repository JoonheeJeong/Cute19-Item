package lexer;

class Char {
    private final char value;
    private final CharacterType type;

    enum CharacterType {
        LETTER, DIGIT, SPECIAL_CHAR, WS, END_OF_STREAM,
    }

    static Char of(char ch) {
        return new Char(ch, getType(ch));
    }

    static Char end() {
        return new Char(Character.MIN_VALUE, CharacterType.END_OF_STREAM);
    }

    private Char(char ch, CharacterType type) {
        this.value = ch;
        this.type = type;
    }

    char value() {
        return this.value;
    }

    CharacterType type() {
        return this.type;
    }

    private static CharacterType getType(char ch) {
        int code = (int) ch;
        // 문자(알파벳 포함)인지 여부를 확인하는 isLetter() 활용,
        // Token.ofName()에서 따로 처리를 해주기 때문에 일단 '?'도 LETTER에 포함시켜서 처리한다.
        if (Character.isLetter(code) || ch == '?') {
            return CharacterType.LETTER;
        }

        if (Character.isDigit(ch)) {
            return CharacterType.DIGIT;
        }

        switch (ch) {
            case '-': case '+': case '*': case '/':
            case '(': case ')':
            case '<': case '=': case '>':
            case '#': case '\'':
                return CharacterType.SPECIAL_CHAR;
        }

        if (Character.isWhitespace(ch)) {
            return CharacterType.WS;
        }

        throw new IllegalArgumentException("input=" + ch);
    }
}
