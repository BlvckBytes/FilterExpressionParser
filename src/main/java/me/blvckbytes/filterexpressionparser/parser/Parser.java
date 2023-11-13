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

import org.jetbrains.annotations.Nullable;
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
import java.util.stream.Collectors;

/**
 * This parser uses the compact and flexible algorithm called "precedence climbing" / "top down
 * recursive decent", which of course is not highly efficient. The main purpose of this project is
 * to parse expressions within configuration files once and then just evaluate the AST within the
 * desired evaluation context at runtime over and over again. Due to the ahead-of-time nature of
 * this intended use-case, efficiency at the level of the parser is sacrificed for understandability.
 */

public class Parser {

  private final Logger logger;
  private final FExpressionParser[] precedenceLadder;

  public Parser(Logger logger) {
    this.logger = logger;

    this.precedenceLadder = new FExpressionParser[] {
      this::parseDisjunctionExpression,
      this::parseConjunctionExpression,
      this::parseComparisonExpression,
      this::parseParenthesisExpression,
      (tk, s) -> this.parsePrimaryExpression(tk),
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
    return parseUnaryExpression(
      (input, h, t, op) -> input,
      tokenizer, precedenceSelf, true,
      new TokenType[] { TokenType.PARENTHESIS_OPEN }, new TokenType[] { TokenType.PARENTHESIS_CLOSE }
    );
  }

  /////////////////////// Binary Expressions ///////////////////////

  private AExpression parseComparisonExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> {

        ComparisonOperation operator;
        switch (op.getType()) {
          case VALUE_EQUALS:
            operator = ComparisonOperation.EQUAL;
            break;

          case VALUE_NOT_EQUALS:
            operator = ComparisonOperation.NOT_EQUAL;
            break;

          case REGEX_MATCHER:
            operator = ComparisonOperation.REGEX_MATCHER;
            break;

          case GREATER_THAN:
            operator = ComparisonOperation.GREATER_THAN;
            break;

          case GREATER_THAN_OR_EQUAL:
            operator = ComparisonOperation.GREATER_THAN_OR_EQUAL;
            break;

          case LESS_THAN:
            operator = ComparisonOperation.LESS_THAN;
            break;

          case LESS_THAN_OR_EQUAL:
            operator = ComparisonOperation.LESS_THAN_OR_EQUAL;
            break;

          default:
            throw new IllegalStateException();
        }

        return new ComparisonExpression(lhs, rhs, operator, h, t, tokenizer.getRawText());
      },
      tokenizer, PrecedenceMode.HIGHER, precedenceSelf,
      new TokenType[] { TokenType.GREATER_THAN, TokenType.GREATER_THAN_OR_EQUAL, TokenType.LESS_THAN, TokenType.LESS_THAN_OR_EQUAL }, null
    );
  }

  private AExpression parseDisjunctionExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new DisjunctionExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, PrecedenceMode.HIGHER, precedenceSelf,
      new TokenType[] { TokenType.BOOL_OR }, null
    );
  }

  private AExpression parseConjunctionExpression(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return parseBinaryExpression(
      (lhs, rhs, h, t, op) -> new ConjunctionExpression(lhs, rhs, h, t, tokenizer.getRawText()),
      tokenizer, PrecedenceMode.HIGHER, precedenceSelf,
      new TokenType[] { TokenType.BOOL_AND }, null
    );
  }

  //////////////////////// Primary Expression ////////////////////////

  private AExpression parsePrimaryExpression(ITokenizer tokenizer) throws AEvaluatorError {
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

  /**
   * Invokes the lowest expression parser within the sequence dictated by the precedence ladder
   * @param tokenizer Current parsing context's tokenizer reference
   * @return Result of invoking the lowest expression parser
   */
  private AExpression invokeLowestPrecedenceParser(ITokenizer tokenizer) throws AEvaluatorError {
    return precedenceLadder[0].apply(tokenizer, 0);
  }

  /**
   * Invokes the next expression parser within the sequence dictated by the precedence ladder
   * @param tokenizer Current parsing context's tokenizer reference
   * @param precedenceSelf Precedence of the current expression parser wanting to invoke the next
   * @return Result of invoking the next expression parser
   */
  private AExpression invokeNextPrecedenceParser(ITokenizer tokenizer, int precedenceSelf) throws AEvaluatorError {
    return precedenceLadder[precedenceSelf + 1].apply(tokenizer, precedenceSelf + 1);
  }

  /**
   * Searches the index within the token type array of an
   * element which has a type matching the input token
   * @param types Token types
   * @param tk Token to match the type of
   * @return Index if available, -1 otherwise
   */
  private int matchingTypeIndex(TokenType[] types, Token tk) {
    for (int i = 0; i < types.length; i++) {
      if (tk.getType() == types[i])
        return i;
    }
    return -1;
  }

  /**
   * Parses an expression with the unary expression pattern by first matching an operator, then parsing an
   * expression (with reset precedence levels) and checking for an optional following terminator
   * @param wrapper Expression wrapper which wraps the input expression and the operator into a matching expression type
   * @param tokenizer Current parsing context's tokenizer reference
   * @param precedenceSelf Precedence of the current expression parser wanting to invoke the next
   * @param resetPrecedence Whether or not to reset the precedence for the parsed input expression
   * @param operators Operators which represent this type of expression (match one)
   * @param terminators Optional terminator for each operator
   * @return Parsed expression after invoking the wrapper on it or the result of the next precedence parser if the operators didn't match
   */
  private AExpression parseUnaryExpression(
    FUnaryExpressionWrapper wrapper, ITokenizer tokenizer,
    int precedenceSelf, boolean resetPrecedence,
    TokenType[] operators, @Nullable TokenType[] terminators
  ) {
    String requiredOperatorsString = Arrays.stream(operators).map(Enum::name).collect(Collectors.joining("|"));
    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a unary expression for the operator " + requiredOperatorsString);

    Token tk = tokenizer.peekToken();
    int opInd;

    // There's no not operator as the next token, hand over to the next higher precedence parser
    if (tk == null || (opInd = matchingTypeIndex(operators, tk)) < 0) {
      logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Doesn't match any required operators of " + requiredOperatorsString);
      return invokeNextPrecedenceParser(tokenizer, precedenceSelf);
    }

    // Consume the operator
    Token operator = tokenizer.consumeToken();

    // Parse the following expression
    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse an input for this expression");

    AExpression input;

    if (resetPrecedence)
      input = invokeLowestPrecedenceParser(tokenizer);
    else
      input = invokeNextPrecedenceParser(tokenizer, precedenceSelf);

    // Terminator requested, expect and eat it, fail otherwise
    if (terminators != null) {
      if ((tk = tokenizer.consumeToken()) == null || tk.getType() != terminators[opInd])
        throw new UnexpectedTokenError(tokenizer, tk, terminators[opInd]);
    }

    return wrapper.apply(input, operator, input.getTail(), operator);
  }

  /**
   * Parses an expression with the binary expression pattern by first invoking the next precedence parser to
   * parse the left hand side, then matching an operator as well as a right hand side with the specified
   * right hand side precedence parser. This action of parsing an operator and a right hand side will happen
   * as often as there are matching operators available. The results will be chained together to the right. After
   * each right hand side, the optional terminator will be expected, if provided.
   * @param wrapper Expression wrapper which wraps the input expression and the operator into a matching expression type
   * @param tokenizer Current parsing context's tokenizer reference
   * @param rhsPrecedence Precedence mode to use when parsing right hand side expressions
   * @param precedenceSelf Precedence of the current expression parser wanting to invoke the next
   * @param operators Operators which represent this type of expression (match one)
   * @param terminators Optional terminator for each operator
   * @return Parsed expression after invoking the wrapper on it or the result of the next precedence parser if the operators didn't match
   */
  private AExpression parseBinaryExpression(
    FBinaryExpressionWrapper wrapper, ITokenizer tokenizer,
    PrecedenceMode rhsPrecedence, int precedenceSelf,
    TokenType[] operators, @Nullable TokenType[] terminators
  ) throws AEvaluatorError {
    logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a binary expression for the operator " + Arrays.stream(operators).map(Enum::name).collect(Collectors.joining("|")));

    AExpression lhs = invokeNextPrecedenceParser(tokenizer, precedenceSelf);

    Token tk, head = lhs.getHead();
    int opInd;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (opInd = matchingTypeIndex(operators, tk)) >= 0
    ) {
      // Consume the operator
      tokenizer.consumeToken();

      logger.log(Level.FINEST, () -> DebugLogSource.PARSER + "Trying to parse a rhs for this operation");

      AExpression rhs;

      if (rhsPrecedence == PrecedenceMode.HIGHER)
        rhs = invokeNextPrecedenceParser(tokenizer, precedenceSelf);
      else if (rhsPrecedence == PrecedenceMode.RESET)
        rhs = invokeLowestPrecedenceParser(tokenizer);
      else
        throw new IllegalStateException("Unimplemented precedence mode");

      Token operator = tk;

      // Terminator requested, expect and eat it, fail otherwise
      if (terminators != null) {
        if ((tk = tokenizer.consumeToken()) == null || tk.getType() != terminators[opInd])
          throw new UnexpectedTokenError(tokenizer, tk, terminators[opInd]);
      }

      lhs = wrapper.apply(lhs, rhs, head, rhs.getTail(), operator);
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
