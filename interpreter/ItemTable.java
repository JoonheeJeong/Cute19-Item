package interpreter;

import parser.ast.IdNode;;
import parser.ast.ValueNode;

import java.util.HashMap;
import java.util.Map;

public class ItemTable {
    private static Map<IdNode, ValueNode> storage = new HashMap<>();

    public static void put(IdNode key, ValueNode value) {
        storage.put(key, value);
    }

    public static ValueNode get(IdNode key) {
        ValueNode result = storage.get(key);
        return result;
    }
}
