package parser.ast;

public class IdNode implements ValueNode {
    public String id;

    public IdNode(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdNode)) return false;
        IdNode idNode = (IdNode) o;
        return (id.equals(idNode.id)) ? true : false;
    }
}
