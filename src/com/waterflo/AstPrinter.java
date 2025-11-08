package com.waterflo;

import com.waterflo.Stmt.Level;
import java.util.List;

public class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<Void> {

  // ---------- Public entry points ----------
  public void print(List<Stmt> statements) {
    int i = 1;
    for (Stmt s : statements) {
      System.out.println("Stmt " + (i++) + ":");
      s.accept(this);
      System.out.println();
    }
  }

  public String print(Expr expr) {
    return expr == null ? "nil" : expr.accept(this);
  }

  // ---------- Stmt visitors ----------
  @Override
  public Void visitRiverStmt(Stmt.River stmt) {
    System.out.println(indent(1) + "River");
    System.out.println(indent(2) + "name: " + stmt.name.lexeme);
    String len = (stmt.length == null) ? "none" : print(stmt.length);
    System.out.println(indent(2) + "length: " + len);
    return null;
  }

  @Override
  public Void visitDamStmt(Stmt.Dam stmt) {
    System.out.println(indent(1) + "Dam");
    System.out.println(indent(2) + "name: " + stmt.name.lexeme);
    return null;
  }

  @Override
  public Void visitLetStmt(Stmt.Let stmt) {
    System.out.println(indent(1) + "Let");
    System.out.println(indent(2) + "name: " + stmt.name.lexeme);
    System.out.println(indent(2) + "value: " + print(stmt.value));
    return null;
  }

  @Override
  public Void visitDrainStmt(Stmt.Drain stmt) {
    System.out.println(indent(1) + "Drain");
    System.out.println(indent(2) + "from: " + stmt.from.lexeme);
    System.out.println(indent(2) + "to: " + stmt.to.lexeme);
    return null;
  }

  @Override
  public Void visitOutputStmt(Stmt.Output stmt) {
    System.out.println(indent(1) + "Output");
    System.out.println(indent(2) + "name: " + stmt.name.lexeme);
    return null;
  }

  // ---------- Expr visitors ----------
  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    if (expr.value instanceof Double) {
      double d = (Double) expr.value;
      if (d == Math.rint(d)) return String.valueOf((long) d); // pretty int if whole
    }
    return expr.value.toString();
  }

  @Override
  public String visitRainLitExpr(Expr.RainLit expr) {
    // expr.value should be the numeric amount (e.g., 25)
    return "(rain " + visitLiteralExpr(new Expr.Literal(expr.value)) + "mm)";
  }

  // ---------- Helpers ----------
  private String parenthesize(String name, Expr... parts) {
    StringBuilder sb = new StringBuilder();
    sb.append('(').append(name);
    for (Expr p : parts) sb.append(' ').append(print(p));
    sb.append(')');
    return sb.toString();
  }

  private String indent(int n) { return "  ".repeat(Math.max(0, n)); }

  @Override
  public Void visitLevelStmt(Level stmt) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitLevelStmt'");
  }
}
