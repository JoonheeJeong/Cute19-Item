package parser.parse;

import java.util.Iterator;

import parser.ast.*;
import lexer.Scanner;
import lexer.Token;
import lexer.TokenType;

public class CuteParser {
    private Iterator<Token> tokens;
    private static final Node END_OF_LIST = new Node(){};

    public CuteParser(String expr) {
        tokens = Scanner.scan(expr);
    }

    private Token getNextToken() {
        if (!tokens.hasNext())
            return null;
        return tokens.next();
    }

    public Node parseExpr() {
        Token t = getNextToken();
        if (t == null) { // 토큰이 더 이상 없으면 null
            System.out.println("No more token");
            return null;
        }
        TokenType tType = t.type();
        String tLexeme = t.lexeme();

        switch (tType) {
            // BinaryOpNode에 대하여 작성
            // +, -, /, *가 해당
            case DIV:
            case EQ:
            case MINUS:
            case GT:
            case PLUS:
            case TIMES:
            case LT:
                // BinaryOpNode 인스턴스 생성 후 주어진 해당 tokenType을 매개변수로 하여
                // value 설정 후 인스턴스 반환.
                BinaryOpNode binaryOpNode = new BinaryOpNode();
                binaryOpNode.setValue(tType);
                return binaryOpNode;

            // FunctionNode에 대하여 작성
            // 키워드가 FunctionNode에 해당
            case ATOM_Q:
            case CAR:
            case CDR:
            case COND:
            case CONS:
            case DEFINE:
            case EQ_Q:
            case LAMBDA:
            case NOT:
            case NULL_Q:
            // FunctionNode 인스턴스 생성 후 주어진 해당 tokenType을 매개변수로 하여
            // value 설정 후 인스턴스 반환.
                FunctionNode functionNode = new FunctionNode();
                functionNode.setValue(tType);
                return functionNode;

            case ID: // idNode 생성하여 값 설정하고 리턴
                return new IdNode(tLexeme);
            case INT: // intNode 생성하여 값 설정하고 리턴
                if (tLexeme == null)
                    System.out.println("???");
                return new IntNode(tLexeme);

            case FALSE: // false node
                return BooleanNode.FALSE_NODE;
            case TRUE: // true node
                return BooleanNode.TRUE_NODE;

            // ListNode의 구조를 형성하는 시작과 끝점
            // L_PAREN일 경우 parseExprList()를 호출하여 처리
            // 닫힌 괄호 이전의 모든 토큰을 노드화시켜서 List로 리턴
            case L_PAREN:
			    return parseExprList();
            case R_PAREN:
                return END_OF_LIST;

            case APOSTROPHE:
                QuoteNode quoteNode = new QuoteNode(parseExpr());
                return ListNode.cons(quoteNode, ListNode.EMPTY_LIST);

            case QUOTE:
                return new QuoteNode(parseExpr());

            default: // error
                System.out.println("Parsing Error!");
                return null;
        }

    }

    // 왼쪽 괄호 토큰이 호출하여 List를 생성하는 메소드
    private ListNode parseExprList() {
        // 다음 토큰을 가져와서 head로 설정
        Node head = parseExpr();
        if (head == null) // 토큰이 더 이상 없음
            return null;
        if (head == END_OF_LIST) // 리스트의 끝
            return ListNode.EMPTY_LIST;

        // 이후 나머지 부분은 END_OF_LIST가 리턴될 때까지 재귀 호출
        // 만약 tail이 null이 된다면 토큰이 없거나 parsing error
        ListNode tail = parseExprList();
        if (tail == null)
            return null;

        // cons 메소드를 통해 생성되어 리턴되는 List는 head tail의 반복 구조
        // 즉 head tail을 가진 부분 List가 더 큰 List의 tail이 되는 구조
        return ListNode.cons(head, tail);
    }
}
