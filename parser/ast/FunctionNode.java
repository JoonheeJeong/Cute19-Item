package parser.ast;

import java.util.HashMap;
import java.util.Map;

import lexer.TokenType;

// 전반적인 형식은 BinaryOpNode와 완전 일치한다.
// 내부적으로 enum의 종류가 다르다는 것과, 클래스 이름만 다를 뿐이다.
public class FunctionNode implements Node {
	public enum FunctionType {
		DEFINE { TokenType tokenType() { return TokenType.DEFINE; } },
		LAMBDA { TokenType tokenType() { return TokenType.LAMBDA; } },
		COND { TokenType tokenType() { return TokenType.COND; } },
		QUOTE { TokenType tokenType() { return TokenType.QUOTE; } },
		NOT { TokenType tokenType() { return TokenType.NOT; } },
		CDR { TokenType tokenType() { return TokenType.CDR; } },
		CAR { TokenType tokenType() { return TokenType.CAR; } },
		CONS { TokenType tokenType() { return TokenType.CONS; } },
		EQ_Q { TokenType tokenType() { return TokenType.EQ_Q; } },
		NULL_Q { TokenType tokenType() { return TokenType.NULL_Q; } },
		ATOM_Q { TokenType tokenType() { return TokenType.ATOM_Q; } };

        // TokenType을 Key로, FuctionType을 Value로 하는 맵을 class variable로 선언
        private static Map<TokenType, FunctionType> fromTokenType = new HashMap<TokenType, FunctionType>();

        // class initialization block은 FuctionType enum이 메모리에 올라갈 때 호출된다.
        // 위의 맵을 초기화 시키는 역할을 한다.
        static {
            for (FunctionType fType : FunctionType.values())
                fromTokenType.put(fType.tokenType(), fType);
        }

        static FunctionType getFunctionType(TokenType tType) {
            return fromTokenType.get(tType);
        }

        // 추상 메소드를 만들어서 enum을 통해 TokenType을 가져올 때 사용하게 하였다.
        abstract TokenType tokenType();

    }

    public FunctionType funcType;

    public void setValue(TokenType tType) {
        FunctionType fType = FunctionType.getFunctionType(tType);
        funcType = fType;
    }

    @Override
    public String toString() {
        return funcType.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FunctionNode)) return false;
        FunctionNode param = (FunctionNode) o;
        return (param.funcType == param.funcType) ? true : false;
    }
}
