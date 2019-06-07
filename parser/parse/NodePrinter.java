package parser.parse;

import parser.ast.*;

public class NodePrinter {
    private StringBuffer sb = new StringBuffer();
    private Node root;

    public NodePrinter(Node root) {
        this.root = root;
    }

    private void printNode(ListNode listNode) {
        if (listNode == ListNode.EMPTY_LIST) {
            return;
        }
        printNode(listNode.car()); // head(Node)를 인자로 호출
        printNode(listNode.cdr()); // tail(ListNode)을 인자로 호출
    }

    private void printNode(QuoteNode quoteNode) {
        Node node = quoteNode.nodeInside();
        if (node == null)
            return;
        if (!(node instanceof IntNode)) // IntNode일 경우 붙이지 않음
            sb.append("\'"); // apostrophe는 띄어쓰기 없이 붙임
        printNode(node); // 내부 노드를 인자로 호출
    }

    private void printNode(Node node) {
        if (node == null)
            return;

        // ListNode의 head가 QuoteNode이면 괄호 제거.
        // 아니면 이전 과제와 동일하게 괄호 추가.
        // 오버로딩 메소드 호출.
        if (node instanceof ListNode) {
            ListNode listNode = (ListNode) node;
            if (listNode.car() instanceof QuoteNode) {
                printNode(listNode);
                return;
            }
            sb.append("( ");
            printNode(listNode);
            sb.append(") ");
            return;
        }

        // QuoteNode이면 종류에 맞는 오버로딩 메소드 호출.
        if (node instanceof QuoteNode) {
            printNode((QuoteNode) node);
            return;
        }

        // 둘 다 아닐 경우(즉 ValueNode의 하나일 경우) 노드 내용 출력
        sb.append(node + " "); // 대괄호 제거
    }

    public void prettyPrint() {
        printNode(root); // root node 호출
        System.out.println(sb);
    }
}
