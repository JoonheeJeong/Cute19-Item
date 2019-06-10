package interpreter;

import parser.ast.*;
import parser.parse.*;

import static parser.ast.FunctionNode.FunctionType.LAMBDA;

import java.util.Scanner;

public class CuteInterpreter {
    private static final Node PASS = new Node() {};

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ItemTableManager.push(new ItemTable());
        System.out.println("Starting Cute19. Enter \"q!\" to exit.");
        String expr;
        do {
            System.out.print("> ");
            expr = sc.nextLine();
            if (expr.equals("q!"))
                break;
            Node parseTree = new CuteParser(expr).parseExpr();
            Node resultNode = new CuteInterpreter().runExpr(parseTree);
            if (resultNode == PASS)
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
            Node value = ItemTableManager.getItem((IdNode) rootExpr);
            if (value == null) {
                errorLog("[ERROR] \"" + rootExpr + "\" is undefined.");
                return null;
            }
            if (value instanceof QuoteNode) {
                Node quoteInside = ((QuoteNode) value).nodeInside();
                value = (isLambdaExpr(quoteInside))
                        ? quoteInside : ListNode.cons(value, ListNode.EMPTY_LIST);
            }
            return value;
        }
        if (rootExpr instanceof ValueNode)
            return rootExpr;
        if (rootExpr instanceof ListNode)
            return runList((ListNode) rootExpr);
        errorLog("run Expr error");
        return null;
    }

    private Node runList(ListNode list) {
        if (list == ListNode.EMPTY_LIST)
            return list;
        Node car = list.car();
        if (car == ListNode.EMPTY_LIST)
            return list;
        if (car instanceof FunctionNode) {
            if (((FunctionNode) car).funcType == LAMBDA)
                return list;
            return runFunction((FunctionNode) car, (ListNode) stripList(list.cdr()));
        }
        if (car instanceof BinaryOpNode)
            return runBinary(list);
        if (car instanceof IdNode)
            return runList(ListNode.cons(runExpr(car), list.cdr()));
        if (car instanceof ListNode) {
            if (isLambdaExpr(car))
                return runLambda(list);
            return runList(ListNode.cons(runList((ListNode) car), list.cdr()));
        }
        return list;
    }

    private Node runFunction(FunctionNode operator, ListNode operand) {
        switch (operator.funcType) {
            case CAR: // 기본 피연산자는 QuotedList(List > Quote > List)
                Node car = operand.car();
                if (car instanceof QuoteNode) {
                    Node temp = runQuote(operand);
                    if (!(temp instanceof ListNode)) {
                        errorLog("[ERROR] CAR can evaluate for Quoted List");
                        return null;
                    }
                    ListNode quoteInside = (ListNode) temp;
                    if (quoteInside == ListNode.EMPTY_LIST) {
                        errorLog("[ERROR] CAR can evaluate for Quoted List");
                        return null;
                    }
                    Node caar = quoteInside.car();
                    if (caar instanceof IntNode || caar instanceof BooleanNode)
                        return caar;
                    return ListNode.cons(new QuoteNode(caar), ListNode.EMPTY_LIST);
                }
                if (car instanceof IdNode)
                    return runFunction(operator, (ListNode) runExpr(car));     // casting error => wrong input
                return runFunction(operator, (ListNode) runList(operand)); // casting error => wrong input
            case CDR: // 기본 피연산자는 QuotedList(List > Quote > List)
                car = operand.car();
                if (car instanceof QuoteNode) {
                    Node temp = runQuote(operand);
                    if (!(temp instanceof ListNode)) {
                        errorLog("[ERROR] CDR can evaluate for Quoted List");
                        return null;
                    }
                    ListNode quoteInside = (ListNode) temp;
                    if (quoteInside == ListNode.EMPTY_LIST) {
                        errorLog("[ERROR] CDR can evaluate for Quoted List");
                        return null;
                    }
                    return ListNode.cons(new QuoteNode(quoteInside.cdr()), ListNode.EMPTY_LIST);
                }
                if (car instanceof IdNode)
                    return runFunction(operator, (ListNode) runExpr(car));     // casting error => wrong input
                return runFunction(operator, (ListNode) runList(operand)); // casting error => wrong input
            case CONS: // second operand의 결과값은 항상 QuotedList로 가정
                Node temp = runExpr(operand.car());
                Node head = stripQuotedList(temp);
                temp = runExpr(operand.cdr().car());
                ListNode tail = (ListNode) stripQuotedList(temp); // casting error => wrong input
                return ListNode.cons(new QuoteNode(ListNode.cons(head, tail)), ListNode.EMPTY_LIST);
            case NULL_Q: // operand가 List로 들어와야 함.
                car = operand.car();
                if (car instanceof IntNode || car instanceof BooleanNode)
                    return BooleanNode.FALSE_NODE;
                if (car instanceof QuoteNode) {
                    Node quote = runQuote(operand); // List가 아닐 수도 있다. ( null? ' c ) => #F
                    if (quote == ListNode.EMPTY_LIST)
                        return BooleanNode.TRUE_NODE;
                    return BooleanNode.FALSE_NODE;
                }
                if (car instanceof FunctionNode || car instanceof BinaryOpNode) {
                    if (operand.cdr() == ListNode.EMPTY_LIST) // ex) ( null? func ), ( null? op )
                        return BooleanNode.FALSE_NODE;
                }
                if (car instanceof IdNode) {
                    if (operand.cdr() == ListNode.EMPTY_LIST) // ex) ( null? id )
                        return runFunction(operator, getIdOperand((IdNode) car));
                }
                Node subResult = runList(operand); // operand 내용이 List
                return runFunction(operator, ListNode.cons(subResult, ListNode.EMPTY_LIST));
            case ATOM_Q: // only one operand
                car = operand.car();
                if (car instanceof IntNode || car instanceof BooleanNode)
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
                    if (operand.cdr() == ListNode.EMPTY_LIST) // ex) ( atom? func ), ( atom? op )
                        return BooleanNode.TRUE_NODE;
                }
                if (car instanceof IdNode) {
                    if (operand.cdr() == ListNode.EMPTY_LIST) // ex) ( atom? id )
                        return runFunction(operator, getIdOperand((IdNode) car));
                }
                subResult = runList(operand); // operand 내용이 List
                return runFunction(operator, ListNode.cons(subResult, ListNode.EMPTY_LIST));
            case EQ_Q:
                Node first = runExpr(operand.car());
                Node second = runExpr(operand.cdr().car());
                return (first.equals(second)) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
            case NOT: // operand는 List이고, 그 내용은 Boolean 또는 관계, 논리 연산
                car = operand.car();
                if (car instanceof BooleanNode) // Boolean이 List에 감싸져 있으면 벗겨서 함수 호출
                    return revertBooleanNode((BooleanNode) car);
                if (car instanceof IntNode || car instanceof QuoteNode)
                    return BooleanNode.FALSE_NODE;
                if (car instanceof FunctionNode || car instanceof BinaryOpNode) {
                    if (operand.cdr() == ListNode.EMPTY_LIST) // ex) ( not func ), ( not op )
                        return BooleanNode.FALSE_NODE;
                }
                if (car instanceof IdNode)
                    return runFunction(operator, getIdOperand((IdNode) car));
                subResult = runList(operand);
                return runFunction(operator, ListNode.cons(subResult, ListNode.EMPTY_LIST));
            case COND:
                temp = runExpr(operand.car());
                if (temp instanceof ListNode) // 조건-실행 List 2개 이상
                    return cond(operand);
                if (temp == BooleanNode.FALSE_NODE)
                    return PASS;
                ListNode cdr = operand.cdr();
                if (cdr == ListNode.EMPTY_LIST)
                    return temp;
                return condOneList(cdr); // 조건-실행 List 1개
            case DEFINE:
                first = operand.car();
                second = operand.cdr().car();
                if (!(first instanceof IdNode)) // error
                    return null;
                second = runExpr(second);
                if (second == null) // error
                    return null;
                if (isLambdaExpr(second))
                    second = new QuoteNode(second);
                else if (second instanceof ListNode) {
                    car = ((ListNode) second).car();
                    if (!(car instanceof QuoteNode)) // eval의 결과인 List가 quote가 아니면 error
                        return null;
                    second = car;
                }
                ItemTableManager.insertItem((IdNode) first, (ValueNode) second);
                return PASS;
            default:
                break;
        }
        return null;
    }

    private boolean isLambdaExpr(Node node) {
        if (!(node instanceof ListNode))
            return false;
        Node car = ((ListNode) node).car();
        if (!(car instanceof FunctionNode))
            return false;
        return((FunctionNode) car).funcType == LAMBDA;
    }

    private Node testLambdaFormalParam(Node car) {
        if (!(car instanceof ListNode))
            return null;
        Node formalParam = ((ListNode) car).car();
        if (!(formalParam instanceof IdNode) || ((ListNode) car).cdr() != ListNode.EMPTY_LIST) {
            errorLog("[ERROR] lambda formal parameter must be an Item");
            return null;
        }
        return formalParam;
    }

    private ListNode testLambdaBody(ListNode cdr) {
        Node body = stripList(cdr);
        if (!(body instanceof ListNode) || cdr.cdr() != ListNode.EMPTY_LIST) {
            errorLog("[ERROR] lambda body must be a List");
            return null;
        }
        return (ListNode) body;
    }

    private Node testLambdaActualParam(ListNode back) {
        Node actualParam = back.car();
        if (actualParam instanceof IdNode)
            actualParam = runExpr(actualParam);
        if (!(actualParam instanceof IntNode)) {
            errorLog("[ERROR] Value of lambda actual parameter must be an Integer");
            return null;
        }
        if (back.cdr() != ListNode.EMPTY_LIST) {
            errorLog("[ERROR] lambda actual parameter must be an Number or Item");
            return null;
        }
        return actualParam;
    }

    private Node runLambda(ListNode list) {
        Node car = list.car();
        ListNode cdr = ((ListNode) car).cdr();
        Node formalParam = testLambdaFormalParam(cdr.car());
        if (formalParam == null)
            return null;
        ListNode body = testLambdaBody(cdr.cdr());
        if (body == null)
            return null;
        Node actualParam = testLambdaActualParam(list.cdr());
        if (actualParam == null)
            return null;
        ItemTableManager.push(new ItemTable());
        ItemTableManager.insertItem((IdNode) formalParam, (ValueNode) actualParam);
        Node result = runList(body);
        ItemTableManager.pop();
        return result;
    }

    private ListNode getIdOperand(IdNode idNode) {
        Node value = runExpr(idNode);
        return (value instanceof ListNode)
                ? (ListNode) value : ListNode.cons(value, ListNode.EMPTY_LIST);
    }

    private BooleanNode revertBooleanNode(BooleanNode arg) {
        if (arg == BooleanNode.TRUE_NODE)
            return BooleanNode.FALSE_NODE;
        return BooleanNode.TRUE_NODE;
    }

    // return Last Node
    private Node lastNode(ListNode listNode) {
        if (listNode.cdr() == ListNode.EMPTY_LIST)
            return runExpr(listNode.car());
        return lastNode(listNode.cdr());
    }

    private Node condOneList(ListNode listNode) {
        Node evalResult = runExpr(listNode.car());
        if (evalResult == BooleanNode.FALSE_NODE)  // 단일 리스트에서 첫 노드의 결과값이 false이면
            return PASS;                           // 이 리스트에 해당하는 cond 결과값 없음
        if (listNode.cdr() == ListNode.EMPTY_LIST) // 첫노드의 결과값이 true이거나 다른 노드일 때,
            return evalResult;                     // 리스트의 원소가 하나가 남으면 바로 리턴
        return lastNode(listNode.cdr());           // 리스트의 원소가 둘 이상이면 나머지 부분으로 재귀 호출
    }

    private Node cond(ListNode lists) {
        if (lists == ListNode.EMPTY_LIST) // 모든 리스트에 대해 false일 경우 PASS 리턴
            return PASS;

        ListNode cdr = lists.cdr();
        ListNode listNode = (ListNode) lists.car(); // casting error => error input

        Node evalList = condOneList(listNode);
        return (evalList == PASS) ? cond(cdr) : evalList;
    }

    // ListNode의 tail은 항상 List이므로 이를 벗기는 역할을 한다.
    private Node stripList(ListNode node) {
        if (node.car() instanceof ListNode && node.cdr() == ListNode.EMPTY_LIST)
            return node.car();
        return node;
    }

    private IntNode getIntOperand(Node operand) {
        if (operand == null) // error input
            return null;

        if (operand instanceof IntNode)        // 정지조건
            return (IntNode) operand;          // casting error => input error
        if (operand instanceof IdNode)
            return (IntNode) runExpr(operand); // casting error => input error
        if (!(operand instanceof ListNode)) {
            errorLog("[ERROR] NaN");
            return null;
        }
        ListNode listOperand = (ListNode) operand;
        Node car = listOperand.car();
        if (car instanceof BinaryOpNode)
            return (IntNode) runBinary(listOperand); // casting error => input error
        if (car instanceof FunctionNode || car instanceof ListNode || car instanceof IdNode)
            return (IntNode) runList(listOperand);   // casting error => input error
        errorLog("[ERROR] NaN");
        return null;
    }

    private Node runBinary(ListNode list) {
        BinaryOpNode operator = (BinaryOpNode) list.car();

        ListNode cdr = list.cdr();

        IntNode firstOperand = getIntOperand(cdr.car());
        IntNode secondOperand = getIntOperand(cdr.cdr().car());

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
