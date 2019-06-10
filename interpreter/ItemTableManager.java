package interpreter;

import parser.ast.IdNode;
import parser.ast.ValueNode;

import java.util.Stack;

class ItemTableManager {
    private static final Stack<ItemTable> stack = new Stack<>();

    static void push(ItemTable itemTable) {
        stack.push(itemTable);
    }

    static void pop() {
        stack.pop();
    }

    static void insertItem(IdNode key, ValueNode value) {
        if(stack.empty())
            System.out.println("ItemTableStack is empty");
        stack.peek().put(key,value);
    }

    static ValueNode getItem(IdNode id) {
        if (stack.empty())
            return null;
        ItemTable itemTable = stack.pop();
        ValueNode value = itemTable.get(id);
        if (value != null) {
            stack.push(itemTable);
            return value;
        }
        value = getItem(id);
        stack.push(itemTable);
        return value;
    }

}
