package lexer;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

class TokenIterator implements Iterator<Token> {
    private final ScanContext context;
    private Optional<Token> nextToken;

    TokenIterator(ScanContext context) {
        this.context = context;
        nextToken = readToNextToken(context);
    }

    @Override
    public boolean hasNext() {
        return nextToken.isPresent();
    }

    @Override
    public Token next() { // iterator forEach에서 호출할 메소드. 다음 토큰 리턴.
        if (!nextToken.isPresent()) {
            throw new NoSuchElementException();
        }

        Token token = nextToken.get(); // 토큰 리턴
        nextToken = readToNextToken(context); // 새로운 다음 토큰 읽어와서 멤버에 저장.

        return token;
    }

    private Optional<Token> readToNextToken(ScanContext context) {
        State current = State.START;
        while (true) {
            TransitionOutput output = current.transit(context);
            if (output.nextState() == State.MATCHED) {
                return output.token(); // 토큰 반환
            } else if (output.nextState() == State.FAILED) {
                throw new ScannerException(); // 상태 실패이면 예외
            } else if (output.nextState() == State.EOS) {
                return Optional.empty(); // 스트림 종료.
            }

            current = output.nextState();
        }
    }
}
