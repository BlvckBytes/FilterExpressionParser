package me.blvckbytes.filterexpressionparser;

import me.blvckbytes.filterexpressionparser.parser.ComparisonOperator;
import me.blvckbytes.filterexpressionparser.parser.LiteralType;
import me.blvckbytes.filterexpressionparser.parser.Parser;
import me.blvckbytes.filterexpressionparser.parser.expression.*;
import me.blvckbytes.filterexpressionparser.tokenizer.Tokenizer;

import java.util.logging.Logger;

public abstract class TestsBase {

  private static final Logger LOGGER = Logger.getGlobal();
  private static final Parser PARSER = new Parser(LOGGER);

  public void validate(String input, ABinaryFilterExpression<?, ?> expectedExpression) {
    compareExpressions(PARSER.parse(new Tokenizer(LOGGER, input)), expectedExpression);
  }

  private void compareExpressions(AExpression actual, AExpression expected) {
    if (!expected.getClass().isInstance(actual))
      throw new ValidationError(actual, "Expected expression-type " + expected.getClass().getSimpleName() + " but found " + actual.getClass().getSimpleName());

    if (expected instanceof ComparisonExpression) {
      ComparisonExpression expectedComparison = (ComparisonExpression) expected;
      ComparisonExpression actualComparison = (ComparisonExpression) actual;

      ComparisonOperator expectedOperator = expectedComparison.getOperator();
      ComparisonOperator actualOperator = actualComparison.getOperator();

      if (expectedOperator != actualOperator)
        throw new ValidationError(actual, "Expected operator " + expectedOperator + " but found " + actualOperator);

      IdentifierExpression expectedIdentifier = expectedComparison.getLhs();
      IdentifierExpression actualIdentifier = actualComparison.getLhs();

      if (!expectedIdentifier.getValue().equals(actualIdentifier.getValue()))
        throw new ValidationError(actualIdentifier, "Expected identifier " + expectedIdentifier.getValue() + " but found " + actualIdentifier.getValue());

      TerminalExpression<?> expectedTerminal = expectedComparison.getRhs();
      TerminalExpression<?> actualTerminal = actualComparison.getRhs();

      if (!expectedTerminal.getClass().isInstance(actualTerminal))
        throw new ValidationError(actualTerminal, "Expected terminal-type " + expectedTerminal.getClass().getSimpleName() + " but found " + actualTerminal.getClass().getSimpleName());

      if (!expectedTerminal.getValue().equals(actualTerminal.getValue()))
        throw new ValidationError(actualTerminal, "Expected terminal-value " + expectedTerminal.getValue() + " but found " + actualTerminal.getValue());

      return;
    }

    if (expected instanceof ABinaryFilterExpression<?, ?>) {
      compareExpressions(((ABinaryFilterExpression<?, ?>) actual).getLhs(), ((ABinaryFilterExpression<?, ?>) expected).getLhs());
      compareExpressions(((ABinaryFilterExpression<?, ?>) actual).getRhs(), ((ABinaryFilterExpression<?, ?>) expected).getRhs());
    }
  }

  protected ComparisonExpression comparison(String identifier, ComparisonOperator operator, TerminalExpression<?> value) {
    return new ComparisonExpression(
      identifier(identifier),
      value, operator,
      null, null, null
    );
  }

  protected IdentifierExpression identifier(String identifier) {
    return new IdentifierExpression(identifier, null, null, null);
  }

  protected ConjunctionExpression conjunction(ABinaryFilterExpression<?, ?> lhs, ABinaryFilterExpression<?, ?> rhs) {
    return new ConjunctionExpression(lhs, rhs, null, null, null);
  }

  protected DisjunctionExpression disjunction(ABinaryFilterExpression<?, ?> lhs, ABinaryFilterExpression<?, ?> rhs) {
    return new DisjunctionExpression(lhs, rhs, null, null, null);
  }

  protected DoubleExpression doubleValue(double value) {
    return new DoubleExpression(value, null, null, null);
  }

  protected LongExpression longValue(long value) {
    return new LongExpression(value, null, null, null);
  }

  protected StringExpression stringValue(String value) {
    return new StringExpression(value, null, null, null);
  }

  protected LiteralExpression nullLiteral() {
    return new LiteralExpression(LiteralType.NULL, null, null, null);
  }

  protected LiteralExpression trueLiteral() {
    return new LiteralExpression(LiteralType.TRUE, null, null, null);
  }

  protected LiteralExpression falseLiteral() {
    return new LiteralExpression(LiteralType.FALSE, null, null, null);
  }
}
