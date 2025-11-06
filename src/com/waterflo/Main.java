package com.waterflo;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Usage: java com.waterflo.Main <source.wflo>");
      System.exit(64);
    }

    String source = Files.readString(Path.of(args[0]));
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    Parser parser = new Parser(tokens);
    List<Stmt> program = parser.parse();
    
    Interpreter interpreter = new Interpreter();
    interpreter.interpret(program);
  }
}
