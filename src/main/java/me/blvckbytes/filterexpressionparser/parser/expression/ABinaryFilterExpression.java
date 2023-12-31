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

import me.blvckbytes.filterexpressionparser.tokenizer.Token;
import me.blvckbytes.filterexpressionparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

public abstract class ABinaryFilterExpression<LHSType extends AExpression, RHSType extends AExpression> extends AExpression {

  protected LHSType lhs;
  protected RHSType rhs;

  public ABinaryFilterExpression(LHSType lhs, RHSType rhs, Token head, Token tail, String fullContainingExpression) {
    super(head, tail, fullContainingExpression);

    this.lhs = lhs;
    this.rhs = rhs;
  }

  public LHSType getLhs() {
    return lhs;
  }

  public RHSType getRhs() {
    return rhs;
  }

  @Override
  public String expressionify() {
    return (
      TokenType.PARENTHESIS_OPEN.getRepresentation() +
      lhs.expressionify() + " " + getInfixSymbol() + " " + rhs.expressionify() +
      TokenType.PARENTHESIS_CLOSE.getRepresentation()
    );
  }

  protected abstract @Nullable String getInfixSymbol();
}
