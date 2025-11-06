package com.waterflo;

import java.util.*;

class Parser {
  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  // Entry point
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }
    return statements;
  }

  // --------------------
  // Declarations
  // --------------------
  private Stmt declaration() {
    if (match(TokenType.RIVER)) return riverDecl();
    if (match(TokenType.DAM)) return damDecl();
    if (match(TokenType.LET)) return letDecl();
    if (match(TokenType.OUTPUT)) return outputStmt();
    if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ARROW)) return drainStmt();

    throw error(peek(), "Unexpected statement.");
  }

  private Stmt riverDecl() {
    Token name = consume(TokenType.IDENTIFIER, "Expect river name.");
    Expr length = null;
    if (match(TokenType.EQUAL)) {
      Token number = consume(TokenType.NUMBER, "Expect number after '='.");
      length = new Expr.Literal(number.literal);
    }
    consume(TokenType.SEMICOLON, "Expect ';' after river declaration.");
    return new Stmt.River(name, length);
  }

  private Stmt damDecl() {
    Token name = consume(TokenType.IDENTIFIER, "Expect dam name.");
    consume(TokenType.SEMICOLON, "Expect ';' after dam declaration.");
    return new Stmt.Dam(name);
  }

  private Stmt letDecl() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
    consume(TokenType.EQUAL, "Expect '=' after name.");
    Expr value = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after let declaration.");
    return new Stmt.Let(name, value);
  }

  private Stmt drainStmt() {
    Token from = consume(TokenType.IDENTIFIER, "Expect source river/dam.");
    consume(TokenType.ARROW, "Expect '->'.");
    Token to = consume(TokenType.IDENTIFIER, "Expect destination river/dam.");
    consume(TokenType.SEMICOLON, "Expect ';' after drain statement.");
    return new Stmt.Drain(from, to);
  }

  private Stmt outputStmt() {
    Token name = consume(TokenType.IDENTIFIER, "Expect river/dam name after 'output'.");
    consume(TokenType.SEMICOLON, "Expect ';' after output statement.");
    return new Stmt.Output(name);
  }

  // --------------------
  // Expressions
  // --------------------
  private Expr expression() {
    return addition();
  }

  private Expr addition() {
    Expr expr = primary();
    while (match(TokenType.PLUS)) {
      Token operator = previous();
      Expr right = primary();
      expr = new Expr.Binary(expr, operator, right);
    }
    return expr;
  }

  private Expr primary() {
    if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());
    if (match(TokenType.NUMBER)) return new Expr.Literal(previous().literal);

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    if (match(TokenType.RAIN)) return rainLit();

    throw error(peek(), "Expect expression.");
  }

  private Expr rainLit() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'rain'.");
    Token number = consume(TokenType.NUMBER, "Expect rainfall number.");
    consume(TokenType.MM, "Expect 'mm' after rainfall number.");
    consume(TokenType.RIGHT_PAREN, "Expect ')' after rainfall literal.");
    return new Expr.RainLit(number.literal);
  }

  // --------------------
  // Helpers
  // --------------------
  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }

  private boolean check(TokenType type) {
    if (isAtEnd()) return false;
    return peek().type == type;
  }

  private boolean checkNext(TokenType type) {
    if (current + 1 >= tokens.size()) return false;
    return tokens.get(current + 1).type == type;
  }

  private Token advance() {
    if (!isAtEnd()) current++;
    return previous();
  }

  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }

  private Token peek() {
    return tokens.get(current);
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    System.err.println("[line " + token.line + "] Error at '" + token.lexeme + "': " + message);
    return new ParseError();
  }

  private static class ParseError extends RuntimeException {}
}
