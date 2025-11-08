package com.waterflo;

abstract class Stmt {
  interface Visitor<R> {
    R visitRiverStmt(River stmt);
    R visitDamStmt(Dam stmt);
    R visitLetStmt(Let stmt);
    R visitDrainStmt(Drain stmt);
    R visitOutputStmt(Output stmt);
  }

  static class River extends Stmt {
    final Token name;
    final Expr length; // optional

    River(Token name, Expr length) {
      this.name = name;
      this.length = length;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitRiverStmt(this);
    }
  }

  static class Dam extends Stmt {
    final Token name;
    final Expr factor; // optional: pass-through factor (e.g., 0.8)

    Dam(Token name, Expr factor) {
      this.name = name;
      this.factor = factor;
    }

    <R> R accept(Visitor<R> visitor) { return visitor.visitDamStmt(this); }
  }

  static class Let extends Stmt {
    final Token name;
    final Expr value;

    Let(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitLetStmt(this);
    }
  }

  static class Drain extends Stmt {
    final Token from;
    final Token to;

    Drain(Token from, Token to) {
      this.from = from;
      this.to = to;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitDrainStmt(this);
    }
  }

  static class Output extends Stmt {
    final Token name;

    Output(Token name) {
      this.name = name;
    }

    <R> R accept(Visitor<R> visitor) {
      return visitor.visitOutputStmt(this);
    }
  }

  abstract <R> R accept(Visitor<R> visitor);
}
