package interpreter;

import parser.ast.*;
import parser.parse.*;

import java.util.Scanner;

public class CuteInterpreter {
    private static final Node define = new Node() {
    };

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Starting Cute19. Enter \"q!\" to exit.");
        String expr;
        do {
            System.out.print("> ");
            expr = sc.nextLine();
            if (expr.equals("q!"))
                break;
            Node parseTree = new CuteParser(expr).parseExpr();
            Node resultNode = new CuteInterpreter().runExpr(parseTree);
            if (resultNode == define)
                continue;
            NodePrinter nodePrinter = new NodePrinter(resultNode);
            nodePrinter.prettyPrint();
        } while (true);
        sc.close();
        System.out.println("Exit Cute19.");
    }

    private void errorLog(String err) {
        System.out.println(err);
    }

    private Node runExpr(Node rootExpr) {
        if (rootExpr == null)
            return null;
        if (rootExpr instanceof IdNode) {
            Node value = ItemTable.get((IdNode) rootExpr);
            if (value instanceof QuoteNode)
                value = ListNode.cons(value, ListNode.EMPTY_LIST);
            return runExpr(value);
        }
        if (rootExpr instanceof IntNode)
            return rootExpr;
        if (rootExpr instanceof BooleanNode)
            return rootExpr;
        if (rootExpr instanceof ListNode)
            return runList((ListNode) rootExpr);
        errorLog("run Expr error");
        return null;
    }

    private Node runList(ListNode list) {
        Node car = list.car();
        if (list.equals(ListNode.EMPTY_LIST))
            return list;
        if (car instanceof FunctionNode)
            return runFunction((FunctionNode) car, (ListNode) stripList(list.cdr()));
        if (car instanceof BinaryOpNode)
            return runBinary(list);
        return list;
    }

    private Node runFunction(FunctionNode operator, ListNode operand) {
        switch (operator.funcType) {
            case CAR:
                if (operand.cdr() != ListNode.EMPTY_LIST)
                    return null;
                Node car = operand.car();
                if (car instanceof QuoteNode) {
                    Node result = ((ListNode) runQuote(operand)).car(); // if casting error, wrong input
                    return ListNode.cons(new QuoteNode(result), ListNode.EMPTY_LIST);
                }
                Node newOperand;
                if (car instanceof IdNode) {
                    newOperand = runExpr(car);
                    return runFunction(operator, (ListNode) newOperand); // if casting error, wrong input
                }
                return null; // operand가 중첩 리스트일 때, 일단 null.
            case CDR:
                if (operand.cdr() != ListNode.EMPTY_LIST)
                    return null;
                car = operand.car();
                if (car instanceof QuoteNode) {
                    ListNode result = ((ListNode) runQuote(operand)).cdr(); // if casting error, wrong input
                    return ListNode.cons(new QuoteNode(result), ListNode.EMPTY_LIST);
                }
                if (car instanceof IdNode) {
                    newOperand = runExpr(car);
                    return runFunction(operator, (ListNode) newOperand); // if casting error, wrong input
                }
                return null; // operand가 중첩 리스트일 때, 일단 null.
            case CONS:
                ListNode cdr = operand.cdr();
                ListNode quotedList = (ListNode) stripList(cdr); // error나면 input error
                ListNode tail = (ListNode) runQuote(quotedList); // tail은 항상 quoted

                car = operand.car();
                Node head;
                if (car instanceof IntNode || car instanceof BooleanNode)
                    head = car;
                else if (car instanceof ListNode) {
                    Node ccar = ((ListNode) car).car();
                    if (ccar instanceof QuoteNode)
                        head = runQuote((ListNode) car);
                    else
                        head = runList((ListNode) car); // 중첩 함수 대응 불가. item3에서 수정 필요.
                } else if (car instanceof IdNode) {
                    head = runExpr(car);
                    return runFunction(operator, ListNode.cons(head, cdr));
                } else { // FunctionNode, BinaryOpNode => error
                    return null;
                }
                return new QuoteNode(ListNode.cons(head, tail));
            case NULL_Q: // operand가 List로 들어와야 함.
                car = operand.car();
                cdr = operand.cdr();
                if (car instanceof IntNode)
                    return BooleanNode.FALSE_NODE;
                if (car instanceof BooleanNode)
                    return BooleanNode.FALSE_NODE;
                if (car instanceof QuoteNode) {
                    operand = (ListNode) runQuote(operand);
                    if (operand == ListNode.EMPTY_LIST)
                        return BooleanNode.TRUE_NODE;
                    return BooleanNode.FALSE_NODE;
                }
                if (car instanceof FunctionNode || car instanceof BinaryOpNode) {
                    if (cdr == ListNode.EMPTY_LIST)
                        return BooleanNode.FALSE_NODE;
                    Node evalResult = runExpr(operand);
                    if (evalResult instanceof ListNode)
                        return runFunction(operator, (ListNode) evalResult); // item3 수정 필요
                    return runFunction(operator, ListNode.cons(evalResult, ListNode.EMPTY_LIST)); // item3 수정 필요
                }
                if (car instanceof IdNode)
                    return runFunction(operator, (ListNode) runExpr(car)); // item3 수정 필요
                return runList((ListNode) car); // item3 수정 필요
            case ATOM_Q:
                head = runQuote(operand); // 단일 Quote로 입력 한정
                if (head == ListNode.EMPTY_LIST)
                    return BooleanNode.TRUE_NODE;
                if (head instanceof ListNode) // Empty가 아닌 List이면 false
                    return BooleanNode.FALSE_NODE;
                return BooleanNode.TRUE_NODE;
            case EQ_Q:
                car = operand.car();
                Node first;
                if (!(car instanceof ListNode))
                    first = car; // IntNode 또는 IdNode
                else
                    first = runQuote((ListNode) car);
                quotedList = (ListNode) stripList(operand.cdr());
                Node second = runQuote(quotedList);

                if (!second.getClass().equals(first.getClass())) // type이 다르면 false
                    return BooleanNode.FALSE_NODE;
                if (first instanceof ListNode) // 하나라도 List이면 false
                    return BooleanNode.FALSE_NODE;
                if (second instanceof IdNode || second instanceof IntNode) // Id이거나 Int일 경우 값이 같으면 true 아니면 false
                    return second.equals(first) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
                return null;
            case NOT: // operand는 List이고, 그 내용은 Boolean 또는 관계, 논리 연산
                car = operand.car();
                if (car instanceof BooleanNode) // Boolean이 List에 감싸져 있으면 벗겨서 함수 호출
                    return revertBooleanNode((BooleanNode) car);
                return revertBooleanNode(evalTest(operand)); // test 결과 역전시켜서 리턴
            case COND:
                Node temp = runExpr(operand.car());
                if (temp instanceof ListNode) // 조건-실행 List 2개 이상
                    return cond(operand);
                if (temp == BooleanNode.TRUE_NODE) // 조건-실행 List 1개
                    return runExpr(operand.cdr().car());
                return null; // error input 또는 단일 조건이 false라서 결과 없음.
            case DEFINE:
                first = operand.car();
                second = operand.cdr().car();
                if (!(first instanceof IdNode))
                    return null;
                if (!(second instanceof ValueNode)) {
                    if (!(second instanceof ListNode))
                        return null;
                    car = ((ListNode) second).car();
                    if (car instanceof QuoteNode)
                        second = car;
                    else
                        second = runExpr(second);
                    if (!(second instanceof ValueNode))
                        return null;
                }
                ItemTable.put((IdNode) first, (ValueNode) second);
                return define;
            default:
                break;
        }
        return null;
    }

    private BooleanNode revertBooleanNode(BooleanNode arg) {
        if (arg == BooleanNode.TRUE_NODE)
            return BooleanNode.FALSE_NODE;
        return BooleanNode.TRUE_NODE;
    }

    private Node cond(ListNode node) {
        if (node == ListNode.EMPTY_LIST) // 정지 조건
            return null;

        ListNode cdr = node.cdr();
        ListNode list = (ListNode) node.car();
        BooleanNode testResult = evalTest(list.car()); // test의 결과를 Boolean으로 한정

        if (testResult == BooleanNode.FALSE_NODE) // 조건 결과가 false이면 다음 리스트로 진행
            return cond(cdr);

        Node then = list.cdr().car();
        return runExpr(then); // then 수행
    }

    private BooleanNode evalTest(Node test) {
        if (test instanceof BooleanNode)
            return (BooleanNode) test;

        if (!(test instanceof ListNode)) // error input
            return null;
        ListNode listTest = (ListNode) test;

        // 밑의 결과에서 casting error가 발생하면 input error
        if (listTest.car() instanceof BinaryOpNode) // 관계 또는 논리 연산 수행
            return (BooleanNode) runBinary(listTest);
        return (BooleanNode) runList(listTest); // test가 중첩 리스트이면 runList 호출
    }

    // ListNode의 tail은 항상 List이므로 이를 벗기는 역할을 한다.
    private Node stripList(ListNode node) {
        if (node.car() instanceof ListNode && node.cdr() == ListNode.EMPTY_LIST)
            return node.car();
        return node;
    }

    private IntNode getIntNodeOperand(Node subRoot) {
        if (subRoot == null) // error input
            return null;

        if (!(subRoot instanceof IntNode || subRoot instanceof ListNode)) // error input
            return null;

        if (subRoot instanceof IntNode) // 정지조건
            return (IntNode) subRoot;

        ListNode listSubRoot = (ListNode) subRoot;
        Node car = listSubRoot.car();
        // 각각 IntNode가 아닐 경우 error input.
        // 올바른 input이면 위의 정지조건에 따라 호출 메소드 runBinary()에서 연산이 이루어진다.
        if (car instanceof BinaryOpNode)
            return (IntNode) runBinary(listSubRoot);
        if (car instanceof ListNode)
            return (IntNode) runList(listSubRoot);
        return null; // error input
    }

    private Node runBinary(ListNode list) {
        BinaryOpNode operator = (BinaryOpNode) list.car();

        ListNode cdr = list.cdr();

        IntNode firstOperand = getIntNodeOperand(cdr.car());
        IntNode secondOperand = getIntNodeOperand(cdr.cdr().car());

        if (firstOperand == null || secondOperand == null) // error input
            return null;

        int firstValue = firstOperand.value;
        int secondValue = secondOperand.value;

        switch (operator.binType) {
            case PLUS:
                return new IntNode(firstValue + secondValue + "");
            case MINUS:
                return new IntNode(firstValue - secondValue + "");
            case TIMES:
                return new IntNode(firstValue * secondValue + "");
            case DIV:
                return new IntNode(firstValue / secondValue + "");
            case LT:
                return (firstValue < secondValue) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
            case GT:
                return (firstValue > secondValue) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
            case EQ:
                return (firstValue == secondValue) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
            default:
                break;
        }
        return null;
    }

    private Node runQuote(ListNode node) {
        return ((QuoteNode) node.car()).nodeInside();
    }
}
