package interpreter;

import parser.ast.*;
import parser.parse.*;

import static parser.ast.FunctionNode.FunctionType.*;

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
        if (rootExpr instanceof IdNode)
            return getIdValue((IdNode) rootExpr);
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
            return null;
        if (car instanceof FunctionNode) {
            FunctionNode.FunctionType funcType = ((FunctionNode) car).funcType;
            if (funcType == LAMBDA)
                return list;
            if (funcType == COND)
                return runCond(list.cdr());
            return runFunction((FunctionNode) car, list.cdr());
        }
        if (car instanceof BinaryOpNode)
            return runBinary(list);
        if (car instanceof IdNode) {
            Node value = getIdValue((IdNode) car);
            if (value == null)
                return null;
            return runList(ListNode.cons(value, list.cdr()));
        }
        if (car instanceof ListNode)
            return runNestedList(list, (ListNode) car);
        if (car instanceof QuoteNode) {
            Node nodeInside = ((QuoteNode) car).nodeInside();
            if (nodeInside instanceof IntNode || nodeInside instanceof BooleanNode)
                return nodeInside;
        }
        return list;
    }

    private Node runFunction(FunctionNode operator, ListNode operand) {
        Node first = null, second = null;
        if (operator.funcType == DEFINE || operator.funcType == EQ_Q || operator.funcType == CONS) {
            ListNode cdr = operand.cdr();
            if (cdr.cdr() != ListNode.EMPTY_LIST) {
                errorLog("[ERROR] " + operator + " must have only two argument");
                return null;
            }
            if (!(operator.funcType == DEFINE))
                first = runExpr(operand.car());
            else {
                first = operand.car();
                if (!(first instanceof IdNode)) {
                    errorLog("[ERROR] define first argument must be item");
                    return null;
                }
            }
            if (first == null)
                return null;
            second = runExpr(cdr.car());
            if (second == null)
                return null;
        }
        if (operator.funcType == DEFINE) {
            if (isLambdaExpr(second)) // runExpr의 결과가 순수 Non-Quoted List인 경우는 Lambda뿐.
                second = new QuoteNode(second);
            else if (second instanceof ListNode) {
                Node car = ((ListNode) second).car();
                if (!(car instanceof QuoteNode)) { // lambda 아니면 무조건 Quoted List
                    errorLog("[ERROR] input second operand: type of List not to be allocated to item as value");
                    return null;
                }
                second = car;
            }
            ItemTableManager.insertItem((IdNode) first, (ValueNode) second);
            return PASS;
        }

        Node head = first;
        Node tail = second;
        if (operator.funcType == CONS || operator.funcType == EQ_Q) {
            if (first instanceof ListNode) {
                Node car = ((ListNode) first).car();
                if (!isLambdaExpr(first) && !(car instanceof QuoteNode)) {
                    errorLog("[ERROR] input first operand: type of List not to be evaluated");
                    return null;
                }
                if (car instanceof QuoteNode)
                    head = ((QuoteNode) car).nodeInside();
            }
        }
        if (operator.funcType == CONS) {
            if (!(second instanceof ListNode)) {
                errorLog("[ERROR] input second operand: Not Quoted List");
                return null;
            }
            Node car = ((ListNode) second).car();
            if (!(car instanceof QuoteNode)) {
                errorLog("[ERROR] input second operand: Not Quoted List");
                return null;
            }
            tail = ((QuoteNode) car).nodeInside();
            return ListNode.cons(new QuoteNode(ListNode.cons(head, (ListNode) tail)), ListNode.EMPTY_LIST);
        }
        if (operator.funcType == EQ_Q) {
            if (second instanceof ListNode) {
                Node car = ((ListNode) second).car();
                if (!isLambdaExpr(second) && !(car instanceof QuoteNode)) {
                    errorLog("[ERROR] input second operand: type of List not to be evaluated");
                    return null;
                }
            }
            return (head.equals(tail)) ? BooleanNode.TRUE_NODE : BooleanNode.FALSE_NODE;
        }

        if (!(operand.cdr() == ListNode.EMPTY_LIST)) {
            errorLog("[ERROR] " + operator + " has only one argument");
            return null;
        }
        Node realOperand = operand.car();
        if (realOperand instanceof IdNode) {
            Node value = getIdValue((IdNode) realOperand);
            if (value == null)
                return null;
            ListNode listOperand = ListNode.cons(value, ListNode.EMPTY_LIST);
            return runFunction(operator, listOperand);
        }
        Node innerNode = null, car = null;
        if (realOperand instanceof ListNode) {
            Node subResult = runList((ListNode) realOperand);
            if (subResult == null)
                return null;
            if (!(subResult instanceof ListNode))
                return runFunction(operator, ListNode.cons(subResult, ListNode.EMPTY_LIST));
            car = ((ListNode) subResult).car();
            if (!(car instanceof QuoteNode)) {
                errorLog("[ERROR] input operand: type of List not to be evaluated");
                return null;
            }
            innerNode = ((QuoteNode) car).nodeInside();
        }

        if (operator.funcType == NOT) {
            if (!(realOperand instanceof ListNode)) {
                if (realOperand instanceof BooleanNode) // Boolean이 List에 감싸져 있으면 벗겨서 함수 호출
                    return revertBooleanNode((BooleanNode) realOperand);
                return BooleanNode.FALSE_NODE;
            }
            return BooleanNode.FALSE_NODE;
        }
        if (operator.funcType == QUOTE) {
            Node target = (innerNode != null) ? car : realOperand;
            return ListNode.cons(new QuoteNode(target), ListNode.EMPTY_LIST);
        }
        if (operator.funcType == NULL_Q || operator.funcType == ATOM_Q) {
            if (!(realOperand instanceof ListNode)) {
                return (operator.funcType == NULL_Q) ? BooleanNode.FALSE_NODE
                        : BooleanNode.TRUE_NODE;
            }

            if (innerNode == ListNode.EMPTY_LIST)
                return BooleanNode.TRUE_NODE;

            if (operator.funcType == NULL_Q)
                return BooleanNode.FALSE_NODE;

            if (innerNode instanceof ListNode)
                return BooleanNode.FALSE_NODE;
            return BooleanNode.TRUE_NODE;
        }

        // CAR & CDR
        if (!(realOperand instanceof ListNode)) {
            errorLog("[ERROR] " + operator + " can't evaluate Not List");
            return null;
        }

        if (!(innerNode instanceof ListNode)) {
            errorLog("[ERROR] " + operator + " can't evaluate Not List");
            return null;
        }
        ListNode innerList = (ListNode) innerNode;
        if (innerList == ListNode.EMPTY_LIST) {
            errorLog("[ERROR] " + operator + " can't evaluate Empty List");
            return null;
        }

        if (operator.funcType == CDR) // CDR
            return ListNode.cons(new QuoteNode(innerList.cdr()), ListNode.EMPTY_LIST);

        // CAR
        Node target = innerList.car();
        if (target instanceof IntNode || target instanceof BooleanNode)
            return target;
        return ListNode.cons(new QuoteNode(target), ListNode.EMPTY_LIST);
    }

    private Node runNestedList(ListNode list, ListNode subList) {
        if (isLambdaExpr(subList))
            return runLambda(list);
        Node subResult = runList(subList);
        if (subResult == null)
            return null;
        if (subResult instanceof ListNode) {
            errorLog("[ERROR] type of nested list not to be allowed");
            return null;
        }
        return runList(ListNode.cons(subResult, list.cdr()));
    }

    private Node runLambda(ListNode list) {
        Node car = list.car();
        ListNode cdr = ((ListNode) car).cdr();
        Node formalParam = testLambdaFormalParam(cdr.car());
        if (formalParam == null)
            return null;
        Node actualParam = testLambdaActualParam(list.cdr());
        if (actualParam == null)
            return null;
        ItemTableManager.push(new ItemTable());
        ItemTableManager.insertItem((IdNode) formalParam, (ValueNode) actualParam);
        Node result = runBody(cdr.cdr());
        ItemTableManager.pop();
        return result;
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
        if (!(car instanceof ListNode)) // ( x ) list 형식으로 받는다.
            return null;
        Node formalParam = ((ListNode) car).car();
        if (!(formalParam instanceof IdNode) || ((ListNode) car).cdr() != ListNode.EMPTY_LIST) {
            errorLog("[ERROR] lambda formal parameter must be \"ONE\" Item");
            return null;
        }
        return formalParam;
    }

    private Node testLambdaActualParam(ListNode back) {
        if (back.cdr() != ListNode.EMPTY_LIST) {
            errorLog("[ERROR] lambda actual parameter must be \"ONE\" Integer, Item or Quote");
            return null;
        }
        Node actualParam = runExpr(back.car());
        if (actualParam == null)
            return null;
        if (actualParam instanceof ListNode) {
            ListNode listParam = (ListNode) actualParam;
            if (listParam.cdr() != ListNode.EMPTY_LIST) {
                errorLog("[ERROR] input lambda actual parameter: two or more List");
                return null;
            }
            Node quoteParam = listParam.car();
            if (quoteParam instanceof QuoteNode) // List를 벗겨서 Quote 형태로 저장된다.
                return quoteParam;
            errorLog("[ERROR] input lambda actual parameter: type of List not to be evaluated");
            return null;
        }
        if (actualParam instanceof IntNode)
            return actualParam;
        errorLog("[ERROR] lambda actual parameter must be \"ONE\" Integer, Item or Quote");
        return null;
    }

    private Node runCond(ListNode lists) {
        if (lists == ListNode.EMPTY_LIST) // 모든 리스트에 대해 false일 경우 PASS 리턴
            return PASS;
        ListNode cdr = lists.cdr();
        Node car = lists.car();
        if (!(car instanceof ListNode)) { // cond는 List의 집합만을 Operand로 받는다.
            errorLog("[ERROR] cond: bad syntax (clause is not a test-value pair)");
            return null;
        }
        ListNode list = (ListNode) car;
        if (list == ListNode.EMPTY_LIST) {
            errorLog("[ERROR] cond: bad syntax (clause is not a test-value pair)");
            return null;
        }
        if (list.car() instanceof QuoteNode) {
            errorLog("[ERROR] quote: bad syntax");
            return null;
        }
        Node evalList = runCondOneList((ListNode) car);
        return (evalList == PASS) ? runCond(cdr) : evalList;
    }

    private Node runBody(ListNode cdr) {
        Node result = runExpr(cdr.car());
        if (result == null)
            return null;
        ListNode back = cdr.cdr();
        if (back == ListNode.EMPTY_LIST)
            return result;
        return runBody(back);
    }

    private Node runCondOneList(ListNode list) {
        Node target = list.car();
        Node result = runExpr(target);
        if (result == PASS) {
            errorLog("[ERROR] define: not allowed in an expression context");
            return null;
        }
        if (result == BooleanNode.FALSE_NODE) // 단일 리스트에서 첫 노드의 결과값이 false이면
            return PASS;                      // 이 리스트에 해당하는 runCond 결과값 없음
        ListNode back = list.cdr();
        if (back == ListNode.EMPTY_LIST)
            return result;                    // 나머지 부분 없으면 리턴
        return runBody(back);                 // 나머지 부분 재귀 함수 리턴
    }

    private BooleanNode revertBooleanNode(BooleanNode arg) {
        if (arg == BooleanNode.TRUE_NODE)
            return BooleanNode.FALSE_NODE;
        return BooleanNode.TRUE_NODE;
    }

    private Node getIdValue(IdNode id) {
        Node value = ItemTableManager.getItem(id);
        if (value == null) {
            errorLog("[ERROR] \"" + id + "\" is undefined.");
            return null;
        }
        if (value instanceof QuoteNode) {
            Node quoteInside = ((QuoteNode) value).nodeInside();
            value = (isLambdaExpr(quoteInside)) ? quoteInside
                    : ListNode.cons(value, ListNode.EMPTY_LIST);
        }
        return value;
    }

    private IntNode getIntOperand(Node operand) {
        if (operand == null) {
            errorLog("[ERROR] operand is empty");
            return null;
        }

        if (operand instanceof IntNode)        // 정지조건
            return (IntNode) operand;
        if (operand instanceof IdNode) {
            Node value = getIdValue((IdNode) operand);
            if (value instanceof IntNode) {
                return (IntNode) value;
            }
            errorLog("[ERROR] NaN");
            return null;
        }
        if (!(operand instanceof ListNode)) {
            errorLog("[ERROR] NaN");
            return null;
        }
        ListNode listOperand = (ListNode) operand;
        Node car = listOperand.car();
        if (car instanceof IntNode || car instanceof BooleanNode || car instanceof QuoteNode) {
            errorLog("[ERROR] NaN");
            return null;
        }
        Node subResult = runList((ListNode) operand);
        if (!(subResult instanceof IntNode)) {
            errorLog("[ERROR] NaN");
            return null;
        }
        return (IntNode) subResult;
    }

    private Node runBinary(ListNode list) {
        BinaryOpNode operator = (BinaryOpNode) list.car();

        ListNode cdr = list.cdr();
        if (cdr ==ListNode.EMPTY_LIST) {
            errorLog("[ERROR] operand is empty");
            return null;
        }

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
}
