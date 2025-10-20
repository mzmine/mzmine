/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JsonNaming(SnakeCaseStrategy.class)
@JsonFormat(with = Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
public enum MatchingMode implements UniqueIdSupplier {

  EQUAL, GREATER_EQUAL, LESSER_EQUAL, NOT_EQUAL, CONTAINS,

  /**
   * any is a list operation where any should match. For example, in Strings this is used by
   * separating the query string by space symbols and searching for ANY substring
   */
  ANY,
  /**
   * ALL is a list operation where ALL should match. For example, in Strings this is used by
   * separating the query string by space symbols and searching for ALL substring
   */
  ALL;

  @JsonCreator
  @Nullable
  public static MatchingMode fromUniqueID(String uniqueId) {
    return UniqueIdSupplier.parseOrElse(uniqueId, values(), null);
  }


  public static List<MatchingMode> getNumericMatchingModes() {
    return List.of(EQUAL, GREATER_EQUAL, LESSER_EQUAL, NOT_EQUAL);
  }

  public static List<MatchingMode> getStringMatchingModes() {
    return List.of(ANY, ALL, CONTAINS, EQUAL);
  }

  @Override
  public String toString() {
    return switch (this) {
      case EQUAL -> "=";
      case GREATER_EQUAL -> "≥";
      case LESSER_EQUAL -> "≤";
      case NOT_EQUAL -> "≠";
      case CONTAINS -> "⊂";
      case ANY -> "any";
      case ALL -> "all";
    };
  }

  public String getDescription() {
    return switch (this) {
      case EQUAL -> "Equal";
      case GREATER_EQUAL -> "Greater or equal";
      case LESSER_EQUAL -> "Lesser or equal";
      case NOT_EQUAL -> "Not equal";
      case CONTAINS -> "Contains (e.g., substring, substructure)";
      case ANY ->
          "Search for ANY substring match to any query word. Separate words by space symbols like: 'alpha hydroxy'. This query requires ANY of the two words in any order.";
      case ALL ->
          "Search for ALL substring matches to any query word. Separate words by space symbols like: 'alpha hydroxy'. This query requires ALL of the two words in any order.";
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case EQUAL -> "equal";
      case GREATER_EQUAL -> "greater_equal";
      case LESSER_EQUAL -> "lesser_equal";
      case NOT_EQUAL -> "not_equal";
      case CONTAINS -> "contains";
      case ANY -> "any";
      case ALL -> "all";
    };
  }

  /**
   * Should split query strings by spaces and search for each
   */
  public boolean isStringListOperation() {
    return switch (this) {
      case ANY, ALL -> true;
      case EQUAL, GREATER_EQUAL, LESSER_EQUAL, NOT_EQUAL, CONTAINS -> false;
    };
  }
}
