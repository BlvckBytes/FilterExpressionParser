package me.blvckbytes.gpeee.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LiteralType {

  TRUE(true),
  FALSE(false),
  NULL(null)
  ;

  private final Object value;
}