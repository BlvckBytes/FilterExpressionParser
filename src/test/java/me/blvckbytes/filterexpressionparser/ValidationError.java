package me.blvckbytes.filterexpressionparser;

import me.blvckbytes.filterexpressionparser.error.AParserError;
import me.blvckbytes.filterexpressionparser.parser.expression.AExpression;

public class ValidationError extends AParserError {

  public ValidationError(AExpression expression, String text) {
    super(
      // This only points at the location that caused the error in very few cases... meh.
      expression.getHead().getRow(),
      expression.getHead().getCol(),
      expression.getFullContainingExpression(),
      text
    );
  }
}
