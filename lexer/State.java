package lexer;

import static lexer.TokenType.*;
import static lexer.TransitionOutput.*;

enum State {
    START {
        @Override
        public TransitionOutput transit(ScanContext context) {
            Char ch = context.getCharStream().nextChar();
            char v = ch.value();
            switch (ch.type()) { // 첫 스트림이 문자나 숫자이면 context에 붙이고 상태 전이.
                case LETTER:
                    context.append(v);
                    return GOTO_ACCEPT_ID;
                case DIGIT:
                    context.append(v);
                    return GOTO_ACCEPT_INT;
                case SPECIAL_CHAR: // 특수문자가 들어온 경우,
                    context.append(v); // 일단 context에 붙여준다.
                    if (v =='+' || v == '-') { // 부호인 경우 상태 전이.
                        return GOTO_SIGN;
                    } else if (v == '#') {  // #(boolean)인 경우 상태 전이.
                        return GOTO_SHARP;
                    } else { // 나머지 특수문자의 토큰 타입을 가져와서 최종 상태로 전이.
                        TokenType type = fromSpecialCharacter(v);
                        return GOTO_MATCHED(type, context.getLexeme());
                    }
                case WS: // 공백이면 계속 상태 유지. 붙이지 않기 때문에 공백으로 토큰 구분함.
                    return GOTO_START;
                case END_OF_STREAM: // 스트림 종료. 즉 토큰이 빈 경우.
                    return GOTO_EOS;
                default:
                    throw new AssertionError();
            }
        }
    },
    ACCEPT_ID { // ID 중간 상태.
        @Override
        public TransitionOutput transit(ScanContext context) {
            Char ch = context.getCharStream().nextChar();
            char v = ch.value();
            switch (ch.type()) {
                case LETTER:
                case DIGIT: // 다음 스트림이 문자나 숫자이면 컨텍스트에 붙이고 상태 유지.
                    context.append(v);
                    return GOTO_ACCEPT_ID;
                case SPECIAL_CHAR: // 다음 스트림이 특수문자이면 문법에 어긋남.
                    return GOTO_FAILED;
                case WS:
                case END_OF_STREAM: // 다음 스트림이 공백이나 EOS이면 최종상태로 전이.
                    return GOTO_MATCHED(Token.ofName(context.getLexeme()));
                default:
                    throw new AssertionError();
            }
        }
    },
    ACCEPT_INT { // INT 중간 상태. 숫자가 계속 나오면 상태 유지. 공백이 나오면 최종 상태로 전이. 나머지 에러.
        @Override
        public TransitionOutput transit(ScanContext context) {
            Char ch = context.getCharStream().nextChar();
            switch (ch.type()) {
                case LETTER:
                    return GOTO_FAILED;
                case DIGIT:
                    context.append(ch.value());
                    return GOTO_ACCEPT_INT;
                case SPECIAL_CHAR:
                    return GOTO_FAILED;
                case WS:
                case END_OF_STREAM:
                    return GOTO_MATCHED(INT, context.getLexeme());
                default:
                    throw new AssertionError();
            }
        }
    },
    SHARP { // 중간 상태. 이후에 T이면 TRUE, F이면 False 아니면 문법에 어긋남.
        @Override
        public TransitionOutput transit(ScanContext context) {
            Char ch = context.getCharStream().nextChar();
            char v = ch.value();
            switch (ch.type()) {
                case LETTER:
                    switch (v) {
                        case 'T': case 't':
                            context.append(v);
                            return GOTO_MATCHED(TRUE, context.getLexeme());
                        case 'F': case 'f':
                            context.append(v);
                            return GOTO_MATCHED(FALSE, context.getLexeme());
                        default:
                            return GOTO_FAILED;
                    }
                default:
                    return GOTO_FAILED;
            }
        }
    },
    SIGN { // 중간 상태. 이후에 문자, 특수문자, EOS일 경우 문법에 어긋남.
        @Override
        public TransitionOutput transit(ScanContext context) {
            Char ch = context.getCharStream().nextChar();
            char v = ch.value();
            switch (ch.type()) {
                case LETTER:
                    return GOTO_FAILED;
                case DIGIT: // 이후에 숫자이면 INT 상태로 전이
                    context.append(v);
                    return GOTO_ACCEPT_INT;
                case SPECIAL_CHAR:
                    return GOTO_FAILED;
                case WS: // 이후에 공백이면 연산자이므로 최종 상태로 전이.
                    String lexeme = context.getLexeme();
                    switch (lexeme) {
                        case "+":
                            return GOTO_MATCHED(PLUS, lexeme);
                        case "-":
                            return GOTO_MATCHED(MINUS, lexeme);
                        default: // 에러 처리
                            throw new AssertionError();
                    }
                case END_OF_STREAM:
                    return GOTO_FAILED;
                default:
                    throw new AssertionError();
            }
        }
    },
    MATCHED { // 최종 상태. 문법에 맞는 토큰.
        @Override
        public TransitionOutput transit(ScanContext context) {
            throw new IllegalStateException("at final state");
        }
    },
    FAILED { // 최종 상태. 문법에 어긋나는 토큰.
        @Override
        public TransitionOutput transit(ScanContext context) {
            throw new IllegalStateException("at final state");
        }
    },
    EOS { // 최종 상태. 스트림 끝.
        @Override
        public TransitionOutput transit(ScanContext context) {
            return GOTO_EOS;
        }
    };

    abstract TransitionOutput transit(ScanContext context);
}
