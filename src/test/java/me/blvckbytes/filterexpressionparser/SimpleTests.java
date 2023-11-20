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

package me.blvckbytes.filterexpressionparser;

import me.blvckbytes.filterexpressionparser.error.UnexpectedIdentifierAfterStringLiteralError;
import me.blvckbytes.filterexpressionparser.parser.ComparisonOperator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SimpleTests extends TestsBase {

  @Test
  public void shouldParseComparison() {
    validate(
      "name != \"User\"",
      comparison("name", ComparisonOperator.NOT_EQUAL, stringValue("User"))
    );
  }

  @Test
  public void shouldParseCaseInsensitivityAndTrimming() {
    validate(
      "name != \"User\"i",
      comparison("name", ComparisonOperator.NOT_EQUAL, stringValue("User", false, false))
    );

    validate(
      "name != \"User\"t",
      comparison("name", ComparisonOperator.NOT_EQUAL, stringValue("User", true, true))
    );

    validate(
      "name != \"User\"it",
      comparison("name", ComparisonOperator.NOT_EQUAL, stringValue("User", false, true))
    );

    validate(
      "name != \"User\"ti",
      comparison("name", ComparisonOperator.NOT_EQUAL, stringValue("User", false, true))
    );

    assertThrows(UnexpectedIdentifierAfterStringLiteralError.class, () -> {
      validate(
        "name != \"User\"a",
        comparison("name", ComparisonOperator.NOT_EQUAL, stringValue("User"))
      );
    });
  }

  @Test
  public void shouldParseConjunction() {
    validate(
      "name == \"User\"it && age <= 50",
      conjunction(
        comparison("name", ComparisonOperator.EQUAL, stringValue("User", false, true)),
        comparison("age", ComparisonOperator.LESS_THAN_OR_EQUAL, longValue(50))
      )
    );
  }

  @Test
  public void shouldParseDisjunction() {
    validate(
      "name == \"User\" || age >= 50.23",
      disjunction(
        comparison("name", ComparisonOperator.EQUAL, stringValue("User")),
        comparison("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, doubleValue(50.23))
      )
    );
  }

  @Test
  public void shouldHandleJunctionPrecedence() {
    validate(
      "name == \"User\" || age >= 50 && color == \"green\" || height != weight",
      disjunction(
        disjunction(
          comparison("name", ComparisonOperator.EQUAL, stringValue("User")),
          conjunction(
            comparison("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, longValue(50)),
            comparison("color", ComparisonOperator.EQUAL, stringValue("green"))
          )
        ),
        comparison("height", ComparisonOperator.NOT_EQUAL, identifier("weight"))
      )
    );
  }

  @Test
  public void shouldHandleParenthesesAndMinification() {
    validate(
      "(name%%false||age>=50)&&(color!=\"green\"||height==weight)",
      conjunction(
        disjunction(
          comparison("name", ComparisonOperator.CONTAINS_FUZZY, falseLiteral()),
          comparison("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, longValue(50))
        ),
        disjunction(
          comparison("color", ComparisonOperator.NOT_EQUAL, stringValue("green")),
          comparison("height", ComparisonOperator.EQUAL, identifier("weight"))
        )
      )
    );

    validate(
      "(name>%true||age>=50)&&(color==null||height<%weight)",
      conjunction(
        disjunction(
          comparison("name", ComparisonOperator.STARTS_WITH, trueLiteral()),
          comparison("age", ComparisonOperator.GREATER_THAN_OR_EQUAL, longValue(50))
        ),
        disjunction(
          comparison("color", ComparisonOperator.EQUAL, nullLiteral()),
          comparison("height", ComparisonOperator.ENDS_WITH, identifier("weight"))
        )
      )
    );
  }
}
