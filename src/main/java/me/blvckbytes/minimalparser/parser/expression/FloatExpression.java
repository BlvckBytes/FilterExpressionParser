package me.blvckbytes.minimalparser.parser.expression;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.blvckbytes.minimalparser.IEvaluationContext;
import me.blvckbytes.minimalparser.IValueInterpreter;
import me.blvckbytes.minimalparser.error.AParserError;

@Getter
@AllArgsConstructor
public class FloatExpression extends AExpression {

  private final Float value;

  @Override
  public Object evaluate(IEvaluationContext context, IValueInterpreter valueInterpreter) throws AParserError {
    return value;
  }

  @Override
  public String expressionify() {
    return String.valueOf(value);
  }
}
