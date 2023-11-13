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

package me.blvckbytes.filterexpressionparser.tokenizer;

import me.blvckbytes.filterexpressionparser.logging.DebugLogSource;
import me.blvckbytes.filterexpressionparser.error.AParserError;
import me.blvckbytes.filterexpressionparser.error.UnknownTokenError;
import org.jetbrains.annotations.Nullable;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tokenizer implements ITokenizer {

  private final String rawText;
  private final Logger logger;
  private final char[] text;
  private final Stack<TokenizerState> saveStates;
  private TokenizerState state;

  public Tokenizer(Logger logger, String text) {
    this.rawText = text;
    this.logger = logger;
    this.text = text.toCharArray();
    this.state = new TokenizerState();
    this.saveStates = new Stack<>();
  }

  //=========================================================================//
  //                                ITokenizer                               //
  //=========================================================================//

  @Override
  public String getRawText() {
    return rawText;
  }

  @Override
  public boolean hasNextChar() {
    return state.charIndex < this.text.length;
  }

  @Override
  public boolean isConsideredWhitespace(char c) {
    return c == ' ' || c == '\t';
  }

  @Override
  public void saveState(boolean debugLog) {
    this.saveStates.push(this.state.copy());

    if (debugLog)
      logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Saved state " + this.saveStates.size() + " (charIndex=" + state.charIndex + ")");
  }

  @Override
  public void restoreState(boolean debugLog) {
    int sizeBefore = this.saveStates.size();
    this.state = this.saveStates.pop();

    if (debugLog)
      logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Restored state " + sizeBefore + " (charIndex=" + state.charIndex + ")");
  }

  @Override
  public TokenizerState discardState(boolean debugLog) {
    int sizeBefore = this.saveStates.size();
    TokenizerState state = this.saveStates.pop();

    if (debugLog)
      logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Discarded state " + sizeBefore + " (charIndex=" + state.charIndex + ")");

    return state;
  }

  @Override
  public char nextChar() {
    char next = this.text[state.charIndex++];

    if (next == '\n') {
      ++state.row;
      state.colStack.push(state.col);
      state.col = 0;
    } else {
      ++state.col;
    }

    return next;
  }

  @Override
  public char peekNextChar() {
    return this.text[state.charIndex];
  }

  @Override
  public void undoNextChar() {
    char lastChar = this.text[state.charIndex - 1];

    if (lastChar == '\n') {
      --state.row;
      state.col = state.colStack.pop();
    }

    else
      --state.col;

    state.charIndex--;
  }

  @Override
  public @Nullable Token peekToken() throws AParserError {
    if (state.currentToken == null)
      readNextToken();

    logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Peeked token " + state.currentToken);

    return state.currentToken;
  }

  @Override
  public @Nullable Token consumeToken() throws AParserError {
    if (state.currentToken == null)
      readNextToken();

    Token result = state.currentToken;
    readNextToken();

    logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Consumed token " + result);

    return result;
  }

  @Override
  public int getCurrentRow() {
    return state.row;
  }

  @Override
  public int getCurrentCol() {
    return state.col;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  private void eatWhitespace() {
    int ate = 0;

    while (hasNextChar() && (isConsideredWhitespace(peekNextChar()) || peekNextChar() == '\n')) {
      ++ate;
      nextChar();
    }

    if (ate > 0) {
      int ateFinal = ate;
      logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Ate " + ateFinal + " character(s) of whitespace");
    }
  }

  /**
   * Reads the next token or null if nothing is available into the local state
   */
  private void readNextToken() throws AParserError {
    eatWhitespace();

    // EOF reached
    if (!hasNextChar()) {
      state.currentToken = null;
      return;
    }

    for (TokenType tryType : TokenType.valuesInTrialOrder) {
      FTokenReader reader = tryType.getTokenReader();

      saveState(false);

      String result = reader.apply(this);

      // This reader wasn't successful, restore and try the next in line
      if (result == null) {
        restoreState(false);
        continue;
      }

      // Discard the saved state (to move forwards) but use it as the token's row/col supplier
      TokenizerState previousState = discardState(false);
      state.currentToken = new Token(tryType, previousState.row, previousState.col, result);

      logger.log(Level.FINEST, () -> DebugLogSource.TOKENIZER + "Reader for " + tryType + " was successful");
      return;
    }

    // No tokenizer matched
    throw new UnknownTokenError(state.row, state.col, rawText);
  }
}
