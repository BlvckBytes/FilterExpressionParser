package me.blvckbytes.filterexpressionparser.parser.expression;

import me.blvckbytes.filterexpressionparser.tokenizer.Token;

public abstract class TerminalExpression<T> extends AExpression {

  public TerminalExpression(Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);
  }

  public abstract T getValue();

}
