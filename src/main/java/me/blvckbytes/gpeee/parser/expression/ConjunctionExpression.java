package me.blvckbytes.gpeee.parser.expression;

import lombok.Getter;
import me.blvckbytes.gpeee.tokenizer.Token;
import me.blvckbytes.gpeee.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

@Getter
public class ConjunctionExpression extends BinaryExpression {

  public ConjunctionExpression(AExpression lhs, AExpression rhs, Token head, Token tail, String fullContainingExpression) {
    super(lhs, rhs, head, tail, fullContainingExpression);
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    return TokenType.BOOL_AND.getRepresentation();
  }
}
