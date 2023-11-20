/*
 * MIT License
 *
 * Copyright (c) 2022 BlvckBytes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.blvckbytes.filterexpressionparser.parser.expression;

import me.blvckbytes.filterexpressionparser.parser.ComparisonOperator;
import me.blvckbytes.filterexpressionparser.tokenizer.Token;
import me.blvckbytes.filterexpressionparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public class ComparisonExpression extends ABinaryFilterExpression<IdentifierExpression, TerminalExpression<?>> {

  private final ComparisonOperator operation;

  public ComparisonExpression(IdentifierExpression lhs, TerminalExpression<?> rhs, ComparisonOperator operation, Token head, Token tail, String fullContainingExpression) {
    super(lhs, rhs, head, tail, fullContainingExpression);
    this.operation = operation;
  }

  public ComparisonOperator getOperator() {
    return operation;
  }

  @Override
  protected @Nullable String getInfixSymbol() {
    switch (operation) {
      case EQUAL:
        return TokenType.VALUE_EQUALS.getRepresentation();
      case EQUAL_SENSITIVE:
        return TokenType.VALUE_EQUALS_SENSITIVE.getRepresentation();
      case NOT_EQUAL:
        return TokenType.VALUE_NOT_EQUALS.getRepresentation();
      case NOT_EQUAL_SENSITIVE:
        return TokenType.VALUE_NOT_EQUALS_SENSITIVE.getRepresentation();
      case REGEX_MATCHER_SENSITIVE:
        return TokenType.REGEX_MATCHER_SENSITIVE.getRepresentation();
      case REGEX_MATCHER:
        return TokenType.REGEX_MATCHER.getRepresentation();
      case STARTS_WITH:
        return TokenType.STARTS_WITH.getRepresentation();
      case ENDS_WITH:
        return TokenType.ENDS_WITH.getRepresentation();
      case CONTAINS:
        return TokenType.CONTAINS.getRepresentation();
      case CONTAINS_FUZZY:
        return TokenType.CONTAINS_FUZZY.getRepresentation();
      case LESS_THAN:
        return TokenType.LESS_THAN.getRepresentation();
      case GREATER_THAN:
        return TokenType.GREATER_THAN.getRepresentation();
      case LESS_THAN_OR_EQUAL:
        return TokenType.LESS_THAN_OR_EQUAL.getRepresentation();
      case GREATER_THAN_OR_EQUAL:
        return TokenType.GREATER_THAN_OR_EQUAL.getRepresentation();
      default:
        return null;
    }
  }
}
