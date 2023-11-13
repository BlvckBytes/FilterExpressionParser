<!-- This file is rendered by https://github.com/BlvckBytes/readme_helper -->

# FilterExpressionParser

This project is a "fork" of [GPEEE](https://github.com/BlvckBytes/GPEEE), which reduced and adapted it's features to serve as a parser for filter expressions.

## Table of Contents
- [Introduction](#introduction)
- [Usage](#usage)
- [Grammar](#grammar)

## Introduction

It is quite the common case that a REST-API has to allow it's users to filter elements based on various comparison operators, like equals, greater, less, etc. While it is indeed possible to depict these requests using simple notation along the lines of LHS Brackets (`field[operator]=value`) or RHS Colon (`field=operator:value`) within the list of query parameters, there is no common consensus on how to implement the junction operators (`and`, `or`) within this system.

What if the user wants to select all items which are either of color `purple` and cost less than `5` or which are of the color `gold` and cost less than `10`? Let's assume LHS Bracket notation:

`color[EQ]=purple&price[LT]=5&color[EQ]=gold&price[LT]=10`

Each field has two constraints attached to it now, but there is no way of knowing how they're joined together. There are quite the possibilities, of which many are logical contradictions. It would certainly be possible to group these operators up, by introducing operator groups (`OG`) the user has to keep track of, as follows:

`color[EQ:OG1]=purple&price[LT:OG1]=5&color[EQ:OG2]=gold&price[LT:OG2]=10&OG1=AND&OG2=AND`

Now it is at least clear that one of the color comparisons and one of the price comparisons each build an `AND`ed group, which is a plausible request. But how are these groups joined together? It is not always clear that they should be either or, so there would have to be an implicit group zero, which represents the root join type:

`color[EQ:OG1]=purple&price[LT:OG1]=5&color[EQ:OG2]=gold&price[LT:OG2]=10&OG1=AND&OG2=AND&OG0=OR`

As one quickly notices, this system would work fine, but (since the user has to manage groups now) introduces a lot of variance. While it may not be that strong of an argument, readability also suffers tremendously from this notation. Let's express the above filtration by making use of this project's grammar:

`(color == "purple" && price < 5) || (color == "gold" && price < 10)`

Not only will there be less used characters in total, but the readability also skyrocketed. Since `&&` has greater precedence than `||`, this example doesn't need parentheses. It can also still be minified:

`color=="purple"&&price<5||color=="gold"&&price<10`

This parser adds a few dozens of **micro**-seconds to the request processing duration and is thus
a compromise who's necessity is to be decided by nobody but the reader, of course.

## Usage

Parsing an input string is quite straight-forward:

```java
Logger logger = Logger.getGlobal();
String input = "...";

// The parser is to be treated as a singleton and can be reused
FilterExpressionParser parser = new FilterExpressionParser(logger);

// For each input value, a new tokenizer must be instantiated
FilterExpressionTokenizer tokenizer = new FilterExpressionTokenizer(logger, input);
ABinaryFilterExpression<?, ?> expression = parser.parse(tokenizer);
```

The `expression` can now be one of the following types:
- [ComparisonExpression](src/main/java/me/blvckbytes/filterexpressionparser/parser/expression/ComparisonExpression.java)
- [DisjunctionExpression](src/main/java/me/blvckbytes/filterexpressionparser/parser/expression/DisjunctionExpression.java)
- [ConjunctionExpression](src/main/java/me/blvckbytes/filterexpressionparser/parser/expression/ConjunctionExpression.java)

If only a single `ComparisonExpression` has been defined in the input string, the act of filtration will be quite trivial to perform. On the other hand, if multiple comparisons are to be executed, the expression will either be a `Conjunction` or `Disjunction`, who's left- and right-hand-side will then either be more `-junction`s or `ComparisonExpression`s. This tree represents the desired filter setup and has to be processed recursively.

## Grammar

<details>
<summary>grammar.ebnf</summary>

```ebnf
Digit ::= [0-9]
Letter ::= [A-Za-z]

Long ::= "-"? Digit+ ("e" Digit+)?
Double ::= "-"? Digit* "." Digit+ ("e" "-"? Digit+)?
Literal ::= "true" | "false" | "null"

# Quotes within string literals need to be escaped
String ::= '"' ('\"' | [^"])* '"'

# Identifiers represent fields to be filtered by
Identifier ::= Letter (Digit | Letter | '_')*

Value := Long
       | Double
       | String
       | Literal
       | Identifier # Fields can be matched on other fields within
                    # the same object as well, not just static values

ComparisonOperator ::= ">"  # Greater than
                     | "<"  # Less than
                     | ">=" # Greater than or equal
                     | "<=" # Less than or equal
                     | "==" # Equals
                     | "!=" # Not Equals
                     | "?"  # Regex match
                     | "%"  # Contains

DisjunctionExpression ::= ConjunctionExpression ("||" ConjunctionExpression)*
ConjunctionExpression ::= ParenthesesExpression ("&&" ParenthesesExpression)*
ParenthesesExpression ::= ("(" FilterExpression ")") | ComparisonExpression
ComparisonExpression ::= Identifier ComparisonOperator Value
FilterExpression ::= DisjunctionExpression
```
</details>


