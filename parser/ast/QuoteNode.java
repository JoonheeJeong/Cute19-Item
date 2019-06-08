package parser.ast;

public class QuoteNode implements ValueNode {
    Node quoted;

    public QuoteNode(Node quoted) {
        this.quoted = quoted;
    }

    @Override
    public String toString() {
        return quoted.toString();
    }

    public Node nodeInside() { return quoted; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuoteNode)) return false;
        QuoteNode param = (QuoteNode) o;
        Node paramQuoted = param.nodeInside();
        return (quoted.equals(paramQuoted)) ? true : false;
    }
}
