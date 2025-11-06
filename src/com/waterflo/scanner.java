package com.waterflo;

import java.util.*;

class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("river", TokenType.RIVER);
    keywords.put("dam", TokenType.DAM);
    keywords.put("let", TokenType.LET);
    keywords.put("output", TokenType.OUTPUT);
    keywords.put("rain", TokenType.RAIN);
    keywords.put("mm", TokenType.MM);
  }

  private int start = 0;
  private int current = 0;
  private int line = 1;

  Scanner(String source) { this.source = source; }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      scanToken();
    }
    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '+': addToken(TokenType.PLUS); break;
      case '=': addToken(TokenType.EQUAL); break;
      case ';': addToken(TokenType.SEMICOLON); break;
      case '(': addToken(TokenType.LEFT_PAREN); break;
      case ')': addToken(TokenType.RIGHT_PAREN); break;
      case '-':
        if (match('>')) addToken(TokenType.ARROW);
        break;
      case '/':
        if (match('/')) {
          while (peek() != '\n' && !isAtEnd()) advance();
        } else if (match('*')) {
          while (!(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
          }
          advance(); advance(); // consume */
        }
        break;
      case ' ':
      case '\r':
      case '\t':
        break;
      case '\n': line++; break;
      default:
        if (isDigit(c)) number();
        else if (isAlpha(c)) identifier();
        else System.err.println("Unexpected character: " + c + " at line " + line);
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) advance();
    String text = source.substring(start, current);
    TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
    addToken(type);
  }

  private void number() {
    while (isDigit(peek())) advance();
    if (peek() == '.' && isDigit(peekNext())) {
      advance();
      while (isDigit(peek())) advance();
    }
    addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
  }

  private boolean isAtEnd() { return current >= source.length(); }
  private char advance() { return source.charAt(current++); }
  private void addToken(TokenType type) { addToken(type, null); }
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
  private boolean match(char expected) {
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;
    current++;
    return true;
  }
  private char peek() { return isAtEnd() ? '\0' : source.charAt(current); }
  private char peekNext() { return (current + 1 >= source.length()) ? '\0' : source.charAt(current + 1); }
  private boolean isDigit(char c) { return c >= '0' && c <= '9'; }
  private boolean isAlpha(char c) { return Character.isLetter(c) || c == '_'; }
  private boolean isAlphaNumeric(char c) { return isAlpha(c) || isDigit(c); }
}
