package com.waterflo;

import java.util.*;


class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment environment = new Environment();
    private int days = 1; // default: single-day simulation

    
    // --- Node graph ---
    private enum Kind { RIVER, DAM }
    private enum DamType { REDUCE, CAP, BOOST, THRESHOLD }

    private static class Node {
    Kind kind = Kind.RIVER;
    // Base source value (from let name = expr;)
    Double base = null;

    // Dam config
    DamType damType = DamType.REDUCE;
    double factor = 1.0;          // used for reduce; default
    double[] damParams = new double[0];
    Double level = null;          // set by 'level d = value;'
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
    // optional: you may ignore stmt.length at runtime
    return null;
    }

    @Override
    public Void visitDamStmt(Stmt.Dam stmt) {
    Node n = ensureNode(stmt.name.lexeme);
    n.kind = Kind.DAM;

    if (stmt.alg == null) {
        // 'dam d;' → default reduce(1.0)
        n.damType = DamType.REDUCE;
        n.factor  = 1.0;
        n.damParams = new double[]{1.0};
        return null;
    }

    // Evaluate params
    java.util.List<Expr> ps = stmt.alg.params;
    double[] vals = new double[ps.size()];
    for (int i = 0; i < ps.size(); i++) {
        Object v = evaluate(ps.get(i));
        vals[i] = (v instanceof Double d) ? d : 0.0;
    }
    n.damParams = vals;

    // Resolve kind
    String k = stmt.alg.kind;
    switch (k) {
        case "number":   // implicit reduce(NUMBER)
        case "reduce":   n.damType = DamType.REDUCE; n.factor = vals.length>0? vals[0]:1.0; break;
        case "cap":      n.damType = DamType.CAP;                                      break;
        case "boost":    n.damType = DamType.BOOST;                                    break;
        case "threshold":n.damType = DamType.THRESHOLD;                                break;
        default:         n.damType = DamType.REDUCE; n.factor = 1.0;                   break;
    }
    return null;
    }

    @Override
    public Void visitLevelStmt(Stmt.Level stmt) {
    Node n = ensureNode(stmt.name.lexeme);
    Object v = evaluate(stmt.value);
    n.level = (v instanceof Double d) ? d : 0.0;
    return null;
    }

    @Override
    public Void visitLetStmt(Stmt.Let stmt) {
    Object value = evaluate(stmt.value);
    environment.define(stmt.name.lexeme, value);

    Node n = ensureNode(stmt.name.lexeme);
    n.kind = Kind.RIVER;                 // treat lets as sources/pass-through nodes
    n.base = (value instanceof Double d) ? d : 0.0;
    return null;
    }

    @Override
    public Void visitDaysStmt(Stmt.Days stmt) {
        Object v = evaluate(stmt.value);
        if (v instanceof Double d) {
            int n = (int) Math.round(d);
            if (n < 1) n = 1;
            days = n;
        } else {
            days = 1;
        }
        return null;
    }


    @Override
    public Void visitDrainStmt(Stmt.Drain stmt) {
    addEdge(stmt.from.lexeme, stmt.to.lexeme);
    return null;
    }

    @Override
    public Void visitOutputStmt(Stmt.Output stmt) {
        String outName = stmt.name.lexeme;

        // 1. Compute all flows (per day)
        Map<String, Double> memo = new HashMap<>();
        double mainOut = computeFlow(outName, memo);

        System.out.println("=== Flow Paths to " + outName + " (per day) ===");

        // 2. Collect all source→output paths
        List<List<String>> paths = new ArrayList<>();
        Deque<String> current = new ArrayDeque<>();
        collectPaths(outName, current, paths);

        if (paths.isEmpty()) {
            System.out.println("(no upstream paths; " + outName + " is isolated)");
        } else {
            // 3. Print each path like a linked list: a(flow) -> b(flow) -> c(flow)
            for (List<String> path : paths) {
                // path is already from source → output
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < path.size(); i++) {
                    String name = path.get(i);
                    Double val = memo.get(name);
                    if (val == null) val = 0.0;
                    if (i > 0) sb.append(" -> ");
                    sb.append(name).append("(").append(stringify(val)).append(")");
                }
                System.out.println(sb.toString());
            }
        }

        System.out.println("----------------------------");
        System.out.println("System Output (" + outName + ") per day = " + stringify(mainOut));
        if (days > 1) {
            double total = mainOut * days;
            System.out.println("For " + days + " days total = " + stringify(total));
        }
        System.out.println("============================\n");

        return null;
    }

    


    //---------- helper methods ----------

    private double computeFlow(String name, Map<String, Double> memo) {
        if (memo.containsKey(name)) return memo.get(name);
        Node n = ensureNode(name);

        double base = (n.base != null) ? n.base : 0.0;

        double inflow = 0.0;
        for (String p : preds.getOrDefault(name, java.util.Collections.emptyList())) {
            inflow += computeOutflow(p, memo);
        }
        double totalIn = base + inflow;

        double out;
        if (n.kind == Kind.DAM) {
            switch (n.damType) {
            case REDUCE: {
                double f = (n.damParams.length > 0) ? n.damParams[0] : n.factor;
                out = totalIn * f;
                break;
            }
            case CAP: {
                double max = (n.damParams.length > 0) ? n.damParams[0] : Double.POSITIVE_INFINITY;
                out = Math.min(totalIn, max);
                break;
            }
            case BOOST: {
                double k = (n.damParams.length > 0) ? n.damParams[0] : 0.0;
                double rainToday = currentRain();
                out = totalIn + k * rainToday;
                break;
            }
            case THRESHOLD: {
                double th   = (n.damParams.length > 0) ? n.damParams[0] : 0.0;
                double low  = (n.damParams.length > 1) ? n.damParams[1] : 0.0;
                double high = (n.damParams.length > 2) ? n.damParams[2] : 1.0;
                double lvl  = (n.level != null) ? n.level : 0.0;
                out = (lvl < th) ? totalIn * low : totalIn * high;
                break;
            }
            default:
                out = totalIn;
            }
        } else {
            out = totalIn; // rivers/lets pass through
        }

        memo.put(name, out);
        return out;
    }

    private double computeOutflow(String name, Map<String, Double> memo) {
        return computeFlow(name, memo);
    }

    private double currentRain() {
        try {
            Object v = environment.get("rain_today");
            if (v instanceof Double d) return d;
        } catch (RuntimeException ignored) {}
        return 1.0; // default if user didn't define rain_today
    }

    private String stringify(Object v) {
        if (v == null) return "nil";
        if (v instanceof Double d) {
            if (d == Math.rint(d)) return String.valueOf(d.longValue());
            return d.toString();
        }
        return v.toString();
    }

    // Collect all paths from sources to 'node'.
    // We walk backward (via preds), then reverse the path so it prints source -> ... -> node.
    private void collectPaths(String node,
                            Deque<String> current,
                            List<List<String>> paths) {
        current.addLast(node);
        List<String> ps = preds.getOrDefault(node, java.util.Collections.emptyList());

        if (ps.isEmpty()) {
            // reached a source (no predecessors)
            List<String> path = new ArrayList<>(current);
            java.util.Collections.reverse(path);   // now source → ... → output
            paths.add(path);
        } else {
            for (String p : ps) {
                collectPaths(p, current, paths);
            }
        }

        current.removeLast();
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
