package com.waterflo;

enum TokenType {
  // Single-character tokens
  PLUS, EQUAL, SEMICOLON, LEFT_PAREN, RIGHT_PAREN,

  // Multi-character
  ARROW, // ->

  // Literals
  IDENTIFIER, NUMBER,

  // Keywords
  RIVER, DAM, LET, OUTPUT, RAIN, MM,

  // Other
  EOF
}
