package lexer;

public enum TokenType {
	INT,
	ID, 
	TRUE, FALSE, NOT,
	PLUS, MINUS, TIMES, DIV,   		//special character
	LT, GT, EQ, APOSTROPHE,    		//special character
	L_PAREN, R_PAREN, QUESTION,		//special character
	DEFINE, LAMBDA, COND, QUOTE,
	CAR, CDR, CONS,
	ATOM_Q, NULL_Q, EQ_Q; 
	
	static TokenType fromSpecialCharacter(char ch) {
		switch ( ch ) {
			case '+':
				return PLUS;
			case '-':
				return MINUS;
			case '*':
				return TIMES;
			case '/':
				return DIV;
			case '<':
				return LT;
			case '>':
				return GT;
			case '=':
				return EQ;
			case '\'':
				return APOSTROPHE;
			case '(':
				return L_PAREN;
			case ')':
				return R_PAREN;
			/* 이거 사실 없어도 돌아감. Char에서 타입을 LETTER로 받기 때문.
			case '?':
				return QUESTION;
			*/
			default:
				throw new IllegalArgumentException("unregistered char: " + ch);
		}
	}
}
