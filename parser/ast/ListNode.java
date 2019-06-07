package parser.ast;

public interface ListNode extends Node {
    ListNode EMPTY_LIST = new ListNode() {
        @Override
        public Node car() {
            return null;
        }
        @Override
        public ListNode cdr() {
            return null;
        }
    };

    static ListNode cons(Node head, ListNode tail) {
        return new ListNode() {
            @Override
            public Node car() { return head; }
            @Override
            public ListNode cdr() { return tail; }
        };
    }

    Node car(); // 리스트 첫 원소
    ListNode cdr(); // 리스트 첫 원소 제외 나머지 부분
}
