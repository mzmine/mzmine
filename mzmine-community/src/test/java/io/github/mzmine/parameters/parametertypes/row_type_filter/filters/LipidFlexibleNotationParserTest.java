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

package io.github.mzmine.parameters.parametertypes.row_type_filter.filters;

import static io.github.mzmine.util.maths.MathOperator.EQUAL;
import static io.github.mzmine.util.maths.MathOperator.GREATER;
import static io.github.mzmine.util.maths.MathOperator.GREATER_EQ;
import static io.github.mzmine.util.maths.MathOperator.LESS_EQ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.LipidFlexibleNotationParser.LipidFlexibleChain;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.LipidFlexibleNotationParser.LipidFlexibleNotation;
import io.github.mzmine.util.maths.MathOperator;
import org.junit.jupiter.api.Test;

class LipidFlexibleNotationParserTest {

  private void assertParsingTotal(String notation, boolean requireMoreThanClass,
      boolean requireDoubleBondDefinition, int expectedC, MathOperator cOp, int expectedDb,
      MathOperator dbOp, int expectedO, MathOperator oOp) throws QueryFormatException {
    this.assertParsingChains(notation, requireMoreThanClass, requireDoubleBondDefinition, 1,
        expectedC, cOp, expectedDb, dbOp, expectedO, oOp);
  }

  private void assertParsingChains(String notation, boolean requireMoreThanClass,
      boolean requireDoubleBondDefinition, int chains, int expectedC, MathOperator cOp,
      int expectedDb, MathOperator dbOp, int expectedO, MathOperator oOp)
      throws QueryFormatException {
    LipidFlexibleNotation result = LipidFlexibleNotationParser.parseLipidNotation(notation,
        requireMoreThanClass, requireDoubleBondDefinition);

    assertEquals(chains, result.chains().size());
    final LipidFlexibleChain total = result.totalCount();
    assertEquals(expectedC, total.carbons().value());
    assertEquals(cOp, total.carbons().operator());
    assertEquals(expectedDb, total.doubleBonds().value());
    assertEquals(dbOp, total.doubleBonds().operator());
    assertEquals(expectedO, total.oxygens().value());
    assertEquals(oOp, total.oxygens().operator());
  }

  @Test
  void parseLipidNotation() throws QueryFormatException {
    // Test lipid class only
    assertParsingChains("PC", false, false, 0, 0, GREATER_EQ, 0, GREATER_EQ, 0, GREATER_EQ);

    // Test C for any lipid class
    assertParsingChains("C", false, false, 0, 0, GREATER_EQ, 0, GREATER_EQ, 0, GREATER_EQ);

    // Test specific carbon number
    assertParsingTotal("C20", false, false, 20, EQUAL, 0, GREATER_EQ, 0, GREATER_EQ);

    // Test carbon and double bonds
    assertParsingTotal("C20:2", false, false, 20, EQUAL, 2, EQUAL, 0, GREATER_EQ);

    // Test with lipid class specified
    assertParsingTotal("PC20:2", false, false, 20, EQUAL, 2, EQUAL, 0, GREATER_EQ);

    // Test greater than syntax
    assertParsingTotal("C>20:2", false, false, 20, GREATER, 2, EQUAL, 0, GREATER_EQ);
    assertParsingTotal("C>=20:>2", false, false, 20, GREATER_EQ, 2, GREATER, 0, GREATER_EQ);
    assertParsingTotal("C>=20:>2;<=1", false, false, 20, GREATER_EQ, 2, GREATER, 1, LESS_EQ);
    assertParsingTotal("C>=20:>2:<=1", false, false, 20, GREATER_EQ, 2, GREATER, 1, LESS_EQ);

    // Test chain definitions
    assertParsingChains("PC18:2_18:0", false, false, 2, 36, EQUAL, 2, EQUAL, 0, GREATER_EQ);
    // last chain defines the operators for now
    assertParsingChains("PC>18:>2;1/>18:0", false, false, 2, 36, GREATER, 2, EQUAL, 1, GREATER_EQ);
  }

  @Test
  void testStrict() {
    assertNotNull(LipidFlexibleNotationParser.parseLipidNotation("C", false, false));
    assertThrows(QueryFormatException.class,
        () -> LipidFlexibleNotationParser.parseLipidNotation("C", true, true));
    assertThrows(QueryFormatException.class,
        () -> LipidFlexibleNotationParser.parseLipidNotation("C", true, false));

    // Test specific carbon number
    assertNotNull(LipidFlexibleNotationParser.parseLipidNotation("C20", false, false));
    assertThrows(QueryFormatException.class,
        () -> LipidFlexibleNotationParser.parseLipidNotation("C20", false, true));
    assertThrows(QueryFormatException.class,
        () -> LipidFlexibleNotationParser.parseLipidNotation("C20", true, true));

  }
}