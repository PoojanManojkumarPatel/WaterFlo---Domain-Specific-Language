package com.waterflo;

import java.util.*;


class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment environment = new Environment();

    
    // --- Node graph ---
    private enum Kind { RIVER, DAM }

    private static class Node {
    Kind kind = Kind.RIVER;
    double factor = 1.0;           // only used for dams
    Double base = null;            // from let name = <value>; (null means 0)
    }

    private final Map<String, Node> nodes = new HashMap<>();
    private final Map<String, List<String>> edges = new HashMap<>(); // from -> [to...]
    private final Map<String, List<String>> preds = new HashMap<>(); // to   -> [from...]

    private Node ensureNode(String name) {
    return nodes.computeIfAbsent(name, n -> new Node());
    }
    private void addEdge(String from, String to) {
    edges.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    preds.computeIfAbsent(to,   k -> new ArrayList<>()).add(from);
    ensureNode(from);
    ensureNode(to);
    }


    void interpret(List<Stmt> statements) {
    	System.out.println("Interpreter running...");
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError e) {
            System.err.println("Runtime error: " + e.getMessage());
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    
 // ---------- Expr visitor methods ----------

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        // literal nodes already store a Java number (Double)
        return expr.value;
    }

    @Override
    public Object visitRainLitExpr(Expr.RainLit expr) {
        // rainfall literal; just return its numeric amount
        return expr.value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        
        System.out.println("Evaluated: " + left + " + " + right + " = " + ((Double) left + (Double) right));

        // right now, the only binary operator is '+'
        if (expr.operator.type == TokenType.PLUS) {
            if (left instanceof Double && right instanceof Double) {
                return (Double) left + (Double) right;
            }
            throw new RuntimeError("Operands to '+' must be numbers.");
        }

        throw new RuntimeError("Unknown operator: " + expr.operator.lexeme);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
    	return environment.get(expr.name.lexeme);
    }
    
    
    // ---------- Stmt visitor methods ----------
    
    @Override
    public Void visitRiverStmt(Stmt.River stmt) {
    Node n = ensureNode(stmt.name.lexeme);
    n.kind = Kind.RIVER;
    // optional: you can store length if you want (stmt.length), but it doesn't affect flow
    return null;
    }

    @Override
    public Void visitDamStmt(Stmt.Dam stmt) {
    Node n = ensureNode(stmt.name.lexeme);
    n.kind = Kind.DAM;
    // parse factor if provided, else default 1.0
    if (stmt.factor != null) {
        Object v = evaluate(stmt.factor);
        if (v instanceof Double d) n.factor = d;
    }
    return null;
    }

    @Override
    public Void visitLetStmt(Stmt.Let stmt) {
    Object value = evaluate(stmt.value);
    environment.define(stmt.name.lexeme, value);
    // treat let-name as a source node too
    Node n = ensureNode(stmt.name.lexeme);
    n.kind = Kind.RIVER; // generic source/pass-through
    n.base = (value instanceof Double d) ? d : 0.0;
    return null;
    }

    @Override
    public Void visitDrainStmt(Stmt.Drain stmt) {
    String from = stmt.from.lexeme;
    String to   = stmt.to.lexeme;
    addEdge(from, to);
    return null;
    }

    @Override
    public Void visitOutputStmt(Stmt.Output stmt) {
    String name = stmt.name.lexeme;
    double flow = computeFlow(name, new HashMap<>()); // memo map
    System.out.println(name + " = " + stringify(flow));
    return null;
    }

    //---------- helper methods ----------

	// nice formatting
	private String stringify(Object v) {
	    if (v == null) return "nil";
	    if (v instanceof Double d) {
	        if (d == Math.rint(d)) return String.valueOf(d.longValue()); // show 30 not 30.0
	    }
	    return v.toString();
	}

    private double computeFlow(String name, Map<String, Double> memo) {
    if (memo.containsKey(name)) return memo.get(name);

    Node n = ensureNode(name);

    // base value (from let) or 0
    double base = (n.base != null) ? n.base : 0.0;

    // sum of outflows from predecessors
    double inflow = 0.0;
    List<String> ps = preds.getOrDefault(name, Collections.emptyList());
    for (String p : ps) {
        inflow += computeOutflow(p, memo); // compute predecessor's outflow
    }

    double totalIn = base + inflow;
    double out = (n.kind == Kind.DAM) ? totalIn * n.factor : totalIn;

    // NOTE: for computeFlow(name) we want the value *at* this node.
    // For printing the "flow at node", use out (its outflow). That’s our convention here.
    memo.put(name, out);
    return out;
    }

    private double computeOutflow(String name, Map<String, Double> memo) {
    // Here outflow of a node is the memoised computeFlow(node)
    return computeFlow(name, memo);
    }
}



class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(String name) {
        if (values.containsKey(name)) return values.get(name);
        throw new RuntimeException("Undefined variable '" + name + "'");
    }

    Map<String, Object> getValues() { return values; }
}

class RuntimeError extends RuntimeException {
    RuntimeError(String message) { super(message); }
}
