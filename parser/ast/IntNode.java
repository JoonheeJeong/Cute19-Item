package parser.ast;

public class IntNode implements ValueNode {
    public int value;

    public IntNode(String value) {
        this.value = new Integer(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IntNode)) return false;
        IntNode intNode = (IntNode) o;
        return (value == intNode.value) ? true : false;
    }
}
