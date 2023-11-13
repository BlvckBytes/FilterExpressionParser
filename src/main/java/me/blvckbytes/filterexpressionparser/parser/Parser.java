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

package me.blvckbytes.filterexpressionparser.parser;

import me.blvckbytes.filterexpressionparser.logging.DebugLogSource;
import me.blvckbytes.filterexpressionparser.error.AEvaluatorError;
import me.blvckbytes.filterexpressionparser.error.UnexpectedTokenError;
import me.blvckbytes.filterexpressionparser.parser.expression.*;
import me.blvckbytes.filterexpressionparser.tokenizer.ITokenizer;
import me.blvckbytes.filterexpressionparser.tokenizer.Token;
import me.blvckbytes.filterexpressionparser.tokenizer.TokenCategory;
import me.blvckbytes.filterexpressionparser.tokenizer.TokenType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Parser {

  private final Logger logger;
  private final FExpressionParser[] precedenceLadder;

  public Parser(Logger logger) {
    this.logger = logger;

    this.precedenceLadder = new FExpressionParser[] {
      this::parseDisjunctionExpression,
      this::parseConjunctionExpression,
      this::parseParenthesisExpression,
      this::parseComparisonExpression,
    };
  }

  public AExpression parse(ITokenizer tokenizer) throws AEvaluatorError {
    return invokeLowestPrecedenceParser(tokenizer);
  }

  //=========================================================================//
  //                            Expression Parsers                           //
  //=========================================================================//

  /////////////////////// Unary Expressions ///////////////////////

  private AExpression parseParenthesisExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a parenthesis expression");

    Token tk = tokenizer.peekToken();

    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {
      logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Not a parenthesis expression");
      return invokeNextPrecedenceParser(tokenizer, precedenceSelf);
    }

    // Consume the parenthesis
    tokenizer.consumeToken();

    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse the content of the parenthesis expression");

    AExpression content = invokeLowestPrecedenceParser(tokenizer);

    // Parenthesis has to be closed again
    if ((tk = tokenizer.consumeToken()) == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    return content;
  }

  /////////////////////// Binary Expressions ///////////////////////

  private AExpression parseDisjunctionExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new DisjunctionExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, precedenceSelf, TokenType.BOOL_OR
    );
  }

  private AExpression parseConjunctionExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new ConjunctionExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, precedenceSelf, TokenType.BOOL_AND
    );
  }

  //////////////////////// Primary Expression ////////////////////////

  private ComparisonExpression parseComparisonExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    Token identifierToken = tokenizer.consumeToken();

    if (identifierToken == null || identifierToken.getType() != TokenType.IDENTIFIER)
      throw new UnexpectedTokenError(tokenizer, identifierToken, TokenType.IDENTIFIER);

    IdentifierExpression identifierExpression = new IdentifierExpression(
      identifierToken.getValue(), identifierToken, identifierToken, tokenizer.getRawText()
    );

    Token operatorToken = tokenizer.consumeToken();

    if (operatorToken == null || operatorToken.getType().getCategory() != TokenCategory.OPERATOR)
      throw new UnexpectedTokenError(tokenizer, identifierToken, TokenType.operatorTypes);

    ComparisonOperator operator;
    switch (operatorToken.getType()) {
      case VALUE_EQUALS:
        operator = ComparisonOperator.EQUAL;
        break;

      case VALUE_NOT_EQUALS:
        operator = ComparisonOperator.NOT_EQUAL;
        break;

      case REGEX_MATCHER:
        operator = ComparisonOperator.REGEX_MATCHER;
        break;

      case GREATER_THAN:
        operator = ComparisonOperator.GREATER_THAN;
        break;

      case GREATER_THAN_OR_EQUAL:
        operator = ComparisonOperator.GREATER_THAN_OR_EQUAL;
        break;

      case LESS_THAN:
        operator = ComparisonOperator.LESS_THAN;
        break;

      case LESS_THAN_OR_EQUAL:
        operator = ComparisonOperator.LESS_THAN_OR_EQUAL;
        break;

      default:
        throw new IllegalStateException();
    }

    AExpression valueExpression = parseTerminalExpression(tokenizer);

    return new ComparisonExpression(
      identifierExpression,
      valueExpression,
      operator,
      identifierToken,
      valueExpression.getTail(),
      tokenizer.getRawText()
    );
  }

  private AExpression parseTerminalExpression(ITokenizer tokenizer) throws AEvaluatorError {
    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a primary expression");

    Token tk = tokenizer.consumeToken();

    if (tk == null)
      throw new UnexpectedTokenError(tokenizer, null, TokenType.valueTypes);

    switch (tk.getType()) {
      case LONG:
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found an integer");
        return new LongExpression(parseIntegerWithPossibleExponent(tk), tk, tk, tokenizer.getRawText());

      case DOUBLE:
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found a double");
        return new DoubleExpression(Double.parseDouble(tk.getValue()), tk, tk, tokenizer.getRawText());

      case STRING:
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found a string");
        return new StringExpression(tk.getValue(), tk, tk, tokenizer.getRawText());

      case IDENTIFIER: {
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found an identifier");
        return new IdentifierExpression(tk.getValue(), tk, tk, tokenizer.getRawText());
      }

      case TRUE:
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found the true literal");
        return new LiteralExpression(LiteralType.TRUE, tk, tk, tokenizer.getRawText());

      case FALSE:
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found the false literal");
      return new LiteralExpression(LiteralType.FALSE, tk, tk, tokenizer.getRawText());

      case NULL:
        logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Found the null literal");
      return new LiteralExpression(LiteralType.NULL, tk, tk, tokenizer.getRawText());

      default:
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.valueTypes);
    }
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private AExpression invokeLowestPrecedenceParser(ITokenizer tokenizer) throws AEvaluatorError {
    return precedenceLadder[0].apply(tokenizer, 0);
  }

  private AExpression invokeNextPrecedenceParser(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return precedenceLadder[precedenceSelf + 1].apply(tokenizer, precedenceSelf + 1);
  }

  private AExpression parseBinaryExpression(
    FBinaryExpressionWrapper wrapper, ITokenizer tokenizer,
    int precedenceSelf, TokenType operator
  ) throws AEvaluatorError {
    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a binary expression for the operator " + operator);

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceSelf);
    Token tk, head = lhs.getHead();

    while ((tk = tokenizer.peekToken()) != null && tk.getType() == operator) {
      // Consume the operator
      tokenizer.consumeToken();

      logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a rhs for this operation");

      AExpression rhs = invokeNextPrecedenceParser(tokenizer, precedenceSelf);

      lhs = wrapper.apply(lhs, rhs, head, rhs.getTail(), tk);
    }

    return lhs;
  }

  /**
   * Parses an integer token which supports exponent notation, which java does not (only on doubles).
   * @param tk Token of integer type
   * @return Value represented by the token
   */
  private int parseIntegerWithPossibleExponent(Token tk) {
    String tokenValue = tk.getValue();
    int exponentIndicatorIndex = tokenValue.indexOf('e');

    int number;

    if (exponentIndicatorIndex < 0)
      number = Integer.parseInt(tokenValue);
    else {
      int numberValue = Integer.parseInt(tokenValue.substring(0, exponentIndicatorIndex));
      int exponentValue = Integer.parseInt(tokenValue.substring(exponentIndicatorIndex + 1));
      number = numberValue * (int) Math.pow(10, exponentValue);
    }

    return number;
  }
}
