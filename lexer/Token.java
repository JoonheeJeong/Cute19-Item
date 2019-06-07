package lexer;

import java.util.HashMap;
import java.util.Map;

public class Token {
    private final TokenType type;
    private final String lexeme;

    static Token ofName(String lexeme) {
        TokenType type = KEYWORDS.get(lexeme);
        if (type != null) { // 키워드이면 그대로 리턴
            return new Token(type, lexeme);
        } else if (lexeme.endsWith("?")) { // 키워드가 아닌데 ?로 끝나는 경우
            if (lexeme.substring(0, lexeme.length() - 1).contains("?")) { // 끝 부분 제외하고 ?가 있는 경우 예외 던짐
                throw new ScannerException("invalid ID=" + lexeme);
            }

            return new Token(TokenType.QUESTION, lexeme); // 타입을 QUESTION으로 주고 리턴
        } else if (lexeme.contains("?")) { // 중간에 물음표가 있는 경우는 예외만 던지고 끝
            throw new ScannerException("invalid ID=" + lexeme);
        } else { // 위의 경우가 아니면 모두 ID 타입으로 처리해서 리턴
            return new Token(TokenType.ID, lexeme);
        }
    }

    Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    public TokenType type() {
        return this.type;
    }

    public String lexeme() {
        return this.lexeme;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", type, lexeme);
    }

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("define", TokenType.DEFINE);
        KEYWORDS.put("lambda", TokenType.LAMBDA);
        KEYWORDS.put("cond", TokenType.COND);
        KEYWORDS.put("quote", TokenType.QUOTE);
        KEYWORDS.put("not", TokenType.NOT);
        KEYWORDS.put("cdr", TokenType.CDR);
        KEYWORDS.put("car", TokenType.CAR);
        KEYWORDS.put("cons", TokenType.CONS);
        KEYWORDS.put("eq?", TokenType.EQ_Q);
        KEYWORDS.put("null?", TokenType.NULL_Q);
        KEYWORDS.put("atom?", TokenType.ATOM_Q);
    }
}
