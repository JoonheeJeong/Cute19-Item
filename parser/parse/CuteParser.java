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
        if (t == null) { // ��ū�� �� �̻� ������ null
            System.out.println("No more token");
            return null;
        }
        TokenType tType = t.type();
        String tLexeme = t.lexeme();

        switch (tType) {
            // BinaryOpNode�� ���Ͽ� �ۼ�
            // +, -, /, *�� �ش�
            case DIV:
            case EQ:
            case MINUS:
            case GT:
            case PLUS:
            case TIMES:
            case LT:
                // BinaryOpNode �ν��Ͻ� ���� �� �־��� �ش� tokenType�� �Ű������� �Ͽ�
                // value ���� �� �ν��Ͻ� ��ȯ.
                BinaryOpNode binaryOpNode = new BinaryOpNode();
                binaryOpNode.setValue(tType);
                return binaryOpNode;

            // FunctionNode�� ���Ͽ� �ۼ�
            // Ű���尡 FunctionNode�� �ش�
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
            // FunctionNode �ν��Ͻ� ���� �� �־��� �ش� tokenType�� �Ű������� �Ͽ�
            // value ���� �� �ν��Ͻ� ��ȯ.
                FunctionNode functionNode = new FunctionNode();
                functionNode.setValue(tType);
                return functionNode;

            case ID: // idNode �����Ͽ� �� �����ϰ� ����
                return new IdNode(tLexeme);
            case INT: // intNode �����Ͽ� �� �����ϰ� ����
                if (tLexeme == null)
                    System.out.println("???");
                return new IntNode(tLexeme);

            case FALSE: // false node
                return BooleanNode.FALSE_NODE;
            case TRUE: // true node
                return BooleanNode.TRUE_NODE;

            // ListNode�� ������ �����ϴ� ���۰� ����
            // L_PAREN�� ��� parseExprList()�� ȣ���Ͽ� ó��
            // ���� ��ȣ ������ ��� ��ū�� ���ȭ���Ѽ� List�� ����
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

    // ���� ��ȣ ��ū�� ȣ���Ͽ� List�� �����ϴ� �޼ҵ�
    private ListNode parseExprList() {
        // ���� ��ū�� �����ͼ� head�� ����
        Node head = parseExpr();
        if (head == null) // ��ū�� �� �̻� ����
            return null;
        if (head == END_OF_LIST) // ����Ʈ�� ��
            return ListNode.EMPTY_LIST;

        // ���� ������ �κ��� END_OF_LIST�� ���ϵ� ������ ��� ȣ��
        // ���� tail�� null�� �ȴٸ� ��ū�� ���ų� parsing error
        ListNode tail = parseExprList();
        if (tail == null)
            return null;

        // cons �޼ҵ带 ���� �����Ǿ� ���ϵǴ� List�� head tail�� �ݺ� ����
        // �� head tail�� ���� �κ� List�� �� ū List�� tail�� �Ǵ� ����
        return ListNode.cons(head, tail);
    }
}
