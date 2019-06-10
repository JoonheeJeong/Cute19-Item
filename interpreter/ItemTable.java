package interpreter;

import parser.ast.IdNode;
import parser.ast.ValueNode;

import java.util.HashMap;
import java.util.Map;

class ItemTable {
    private Map<IdNode, ValueNode> storage = new HashMap<>();

    void put(IdNode key, ValueNode value) {
        storage.put(key, value);
    }

    ValueNode get(IdNode key) {
        return storage.get(key);
    }
}
