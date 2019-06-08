package parser.ast;

import java.util.HashMap;
import java.util.Map;

import lexer.TokenType;

// �������� ������ BinaryOpNode�� ���� ��ġ�Ѵ�.
// ���������� enum�� ������ �ٸ��ٴ� �Ͱ�, Ŭ���� �̸��� �ٸ� ���̴�.
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

        // TokenType�� Key��, FuctionType�� Value�� �ϴ� ���� class variable�� ����
        private static Map<TokenType, FunctionType> fromTokenType = new HashMap<TokenType, FunctionType>();

        // class initialization block�� FuctionType enum�� �޸𸮿� �ö� �� ȣ��ȴ�.
        // ���� ���� �ʱ�ȭ ��Ű�� ������ �Ѵ�.
        static {
            for (FunctionType fType : FunctionType.values())
                fromTokenType.put(fType.tokenType(), fType);
        }

        static FunctionType getFunctionType(TokenType tType) {
            return fromTokenType.get(tType);
        }

        // �߻� �޼ҵ带 ���� enum�� ���� TokenType�� ������ �� ����ϰ� �Ͽ���.
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
