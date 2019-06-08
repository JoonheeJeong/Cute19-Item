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
        if (rootExpr instanceof ValueNode)
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
                    ListNode quote = (ListNode) runQuote(operand); // if casting error, error input
                    Node caar = quote.car();
                    if (caar instanceof ListNode)
                        return ListNode.cons(new QuoteNode(caar), ListNode.EMPTY_LIST);
                    if (caar instanceof IdNode)
                        return runExpr(caar);
                    return caar;
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
                    ListNode quote = (ListNode) runQuote(operand); // if casting error, wrong input
                    ListNode cdar = quote.cdr();
                    return ListNode.cons(new QuoteNode(cdar), ListNode.EMPTY_LIST);
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

                Node head = stripQuotedList(eval(operand.car()));
                return (head == null || tail == null) ? null
                        : ListNode.cons(new QuoteNode(ListNode.cons(head, tail)), ListNode.EMPTY_LIST);
            case NULL_Q: // operand가 List로 들어와야 함.
                car = operand.car();
                cdr = operand.cdr();
                if (car instanceof IntNode)
                    return BooleanNode.FALSE_NODE;
                if (car instanceof BooleanNode)
                    return BooleanNode.FALSE_NODE;
                if (car instanceof QuoteNode) {
                    Node quote = runQuote(operand); // List가 아닐 수도 있다. ( null? ' c ) => #F
                    if (quote == ListNode.EMPTY_LIST)
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
                    return runFunction(operator, ListNode.cons(runExpr(car), ListNode.EMPTY_LIST));
                return runFunction(operator, ListNode.cons(stripQuotedList(eval(operand)), ListNode.EMPTY_LIST));
            case ATOM_Q: // only one operand
                car = operand.car();
                cdr = operand.cdr();
                if (car instanceof IntNode)
                    return BooleanNode.TRUE_NODE;
                if (car instanceof BooleanNode)
                    return BooleanNode.TRUE_NODE;
                if (car instanceof QuoteNode) {
                    Node quote = runQuote(operand); // List가 아닐 수도 있다. ( atom? ' c ) => #T
                    if (quote == ListNode.EMPTY_LIST)
                        return BooleanNode.TRUE_NODE;
                    if (quote instanceof ListNode)
                        return BooleanNode.FALSE_NODE;
                    return BooleanNode.TRUE_NODE;
                }
                if (car instanceof FunctionNode || car instanceof BinaryOpNode) {
                    if (cdr == ListNode.EMPTY_LIST)
                        return BooleanNode.TRUE_NODE;
                    Node evalResult = runExpr(operand);
                    if (evalResult instanceof ListNode)
                        return runFunction(operator, (ListNode) evalResult); // item3 수정 필요
                    return runFunction(operator, ListNode.cons(evalResult, ListNode.EMPTY_LIST)); // item3 수정 필요
                }
                if (car instanceof IdNode)
                    return runFunction(operator, ListNode.cons(runExpr(car), ListNode.EMPTY_LIST));
                return runFunction(operator, ListNode.cons(stripQuotedList(eval(operand)), ListNode.EMPTY_LIST));
            case EQ_Q:
                Node first = eval(operand.car());
                Node second = eval(operand.cdr().car());
                return (first.equals(second)) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
            case NOT: // operand는 List이고, 그 내용은 Boolean 또는 관계, 논리 연산
                car = operand.car();
                if (car instanceof BooleanNode) // Boolean이 List에 감싸져 있으면 벗겨서 함수 호출
                    return revertBooleanNode((BooleanNode) car);
                return revertBooleanNode((BooleanNode) eval(operand)); // casting error => input error
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
                second = eval(second);
                if (second == null)
                    return null;
                if (second instanceof ListNode) {
                    car = ((ListNode) second).car();
                    if (!(car instanceof QuoteNode)) // eval의 결과인 List가 quote가 아니면 error
                        return null;
                    second = car;
                }
                ItemTable.put((IdNode) first, (ValueNode) second);
                return define;
            default:
                break;
        }
        return null;
    }

    private Node eval(Node operand) {
        if (operand == null) // error
            return null;
        if (!(operand instanceof ListNode)) {
            if (!(operand instanceof IdNode))
                return operand;
            return runExpr(operand);
        }
        Node car = ((ListNode) operand).car();
        if (car == null) // EMPTY_LIST
            return operand;
        if (car instanceof IntNode || car instanceof BooleanNode) // error input
            return null;
        if (car instanceof FunctionNode || car instanceof BinaryOpNode || car instanceof ListNode)
            return runExpr(operand);
        if (car instanceof IdNode) {
            car = runExpr(car);
            if (car == null) // error input
                return null;
            ListNode cdr = ((ListNode) operand).cdr();
            if (cdr == ListNode.EMPTY_LIST)
                return car;
            return runList(ListNode.cons(car, cdr)); // 리스트 뒷부분 eval 후에 List로 합쳐서 다시 eval
        }
        if (car instanceof QuoteNode)
            return operand;
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

    private Node stripQuotedList(Node node) {
        if (node == null)
            return null;
        if (!(node instanceof ListNode))
            return node;
        ListNode listNode = (ListNode) node;
        if (listNode.car() instanceof QuoteNode)
            return runQuote(listNode);
        return node; // 이 부분 eval(node) 안 해도 되는지 의문.
    }

    private Node runQuote(ListNode node) {
        return ((QuoteNode) node.car()).nodeInside();
    }
}
