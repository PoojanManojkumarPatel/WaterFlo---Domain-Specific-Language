package com.waterflo;

abstract class Stmt {
  interface Visitor<R> {
    R visitRiverStmt(River stmt);
    R visitDamStmt(Dam stmt);
    R visitLetStmt(Let stmt);
    R visitDrainStmt(Drain stmt);
    R visitOutputStmt(Output stmt);
    R visitLevelStmt(Level stmt);

  }

    static class DamAlg {
    final String kind;                 // "number" | "reduce" | "cap" | "boost" | "threshold"
    final java.util.List<Expr> params; // expressions for parameters
    DamAlg(String kind, java.util.List<Expr> params) {
      this.kind = kind;
      this.params = params;
    }
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
    final DamAlg alg; // nullable (means default reduce(1.0))

    Dam(Token name, DamAlg alg) {
      this.name = name;
      this.alg = alg;
    }

    <R> R accept(Visitor<R> visitor) { return visitor.visitDamStmt(this); }
  }

  static class Level extends Stmt {
    final Token name;
    final Expr value;

    Level(Token name, Expr value) {
      this.name = name;
      this.value = value;
    }

    <R> R accept(Visitor<R> visitor) { return visitor.visitLevelStmt(this); }
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

