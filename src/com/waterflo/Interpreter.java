package com.waterflo;

import java.util.*;

import com.waterflo.Expr.Binary;
import com.waterflo.Expr.Grouping;
import com.waterflo.Expr.Literal;
import com.waterflo.Expr.RainLit;
import com.waterflo.Expr.Variable;
import com.waterflo.Stmt.Dam;
import com.waterflo.Stmt.Drain;
import com.waterflo.Stmt.Let;
import com.waterflo.Stmt.Output;
import com.waterflo.Stmt.River;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment environment = new Environment();

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
    
    
    // --- Stmt visitor methods will go here ---
    
    @Override
	public Void visitRiverStmt(River stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitDamStmt(Dam stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitLetStmt(Let stmt) {
		 Object value = evaluate(stmt.value);                    // triggers visitBinaryExpr, RainLit, etc.
		 environment.define(stmt.name.lexeme, value);
		 return null;
	}

	@Override
	public Void visitDrainStmt(Drain stmt) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Void visitOutputStmt(Output stmt) {
		Object value = environment.get(stmt.name.lexeme);
	    System.out.println(stmt.name.lexeme + " = " + stringify(value));
	    return null;
	}
	// helper (nice formatting)
	private String stringify(Object v) {
	    if (v == null) return "nil";
	    if (v instanceof Double d) {
	        if (d == Math.rint(d)) return String.valueOf(d.longValue()); // show 30 not 30.0
	    }
	    return v.toString();
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
