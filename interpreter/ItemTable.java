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
/*
        for (Map.Entry<IdNode, ValueNode> entry : storage.entrySet()) {
        IdNode innerKey = entry.getKey();
        System.out.println("innerKey.hashCode(): " + innerKey.hashCode());
        System.out.println("key.hashCode(): " + innerKey.hashCode());
        if (entry.getKey().equals(key))
        System.out.println(key + " yes");
        else
        System.out.println(key + " no");
        }
        boolean a = storage.containsKey(key);
*/
        ValueNode result = storage.get(key);
        return result;
    }
}
