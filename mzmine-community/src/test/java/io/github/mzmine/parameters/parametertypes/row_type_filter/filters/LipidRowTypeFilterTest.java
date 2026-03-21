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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LipidRowTypeFilterTest {

  /**
   * Enum of different lipid notation types with examples that can be used for testing
   */
  enum LipidNotationType {
    // Lipid class only

    CLASS_ONLY("PC", "PC30:0", true), //
    CLASS_ONLY2("PC", "PC18:0_22:2", true), //
    ANY_LIPID("C", "PC36:2", true), //
    ANY_LIPID2("C", "PC36:0;0", true), //
    ANY_LIPID3("C", "PC36:0;0O", true), //

    // Species level notation
    SPECIES_CARBONS_ONLY("C20", "PC20:1", true), //
    SPECIES_SIMPLE("PC36:2", "PC36:2", true), //
    SPECIES_ANY_CLASS("C36:2", "PC36:2", true), //
    SPECIES_WITH_OXYGEN("PC36:2;1", "PC36:2;1", true), //

    // Ranges and operators
    CARBONS_GREATER_THAN("C>20:2", "PC22:2", true), //
    CARBONS_GREATER_EQUAL("C>=20:2", "PC20:2", true), //
    DOUBLE_BONDS_GREATER("C20:>2", "PC20:3", true), //
    DOUBLE_BONDS_LESS_EQUAL("PC36:<=2", "PC36:1", true), //
    OXYGEN_SPECIFICATION("PC36:2;>1", "PC36:2;2", true), //
    COMPLEX_RANGE("PC>34:>=2;<=1", "PC36:2;1", true), //

    // Molecular species level (chains)
    MOLECULAR_SPECIES("PC18:2_18:0", "PC18:2_18:0", true), //
    MOLECULAR_SPECIES_SLASH("PC18:2/18:0", "PC18:2_18:0", true), //
    MOLECULAR_SPECIES_ANY_CLASS("C18:2_18:0", "PC18:2_18:0", true), //
    MOLECULAR_SPECIES_OXYGEN("PC18:2;1_18:0", "PC18:2;1_18:0", true), //
    MOLECULAR_CHAINS_RANGE("PC>16:>1_>18:0", "PC18:2_20:0", true), //

    // Ranges with hyphen
    RANGE_SPECIES("PC32:0 - PC36:2", "PC34:1", true), //
    RANGE_SPECIES2("PC32:0 - PC34:6", "PC32:0", true), //
    RANGE_SPECIES3("PC32:0 - PC34:6", "PC34:6", true), //
    RANGE_SPECIES_WITH_OXYGEN("PC32:0 - PC36:2", "PC34:1;4", true), //
    RANGE_COMPLEX("C>30:>0 - PC40:6", "PC36:2", true), //
    RANGE_CLASS("Cer - Cer40:6", "Cer36:2;2O", true), //
    RANGE_CLASS_MISMATCH("Cer - Cer40:6;1O", "Cer36:2;2O", false), //

    // Non-matching examples
    NON_MATCHING_CLASS("PC", "PE36:2", false), //
    NON_MATCHING_CARBONS("PC34:2", "PC36:2", false), //
    NON_MATCHING_DOUBLE_BONDS("PC36:4", "PC36:2", false), //
    NON_MATCHING_COMPLEX("PC>36:>2", "PC36:2", false), //
    NON_MATCHING_RANGE("PC32:0 - PC34:6", "PC36:2", false), //
    NON_MATCHING_CHAINS("PC18:2_18:0", "PC16:0_18:2", false); //

    private final String filterPattern;
    private final String testLipidName;
    private final boolean shouldMatch;

    LipidNotationType(String filterPattern, String testLipidName, boolean shouldMatch) {
      this.filterPattern = filterPattern;
      this.testLipidName = testLipidName;
      this.shouldMatch = shouldMatch;
    }

    public String getFilterPattern() {
      return filterPattern;
    }

    public String getTestLipidName() {
      return testLipidName;
    }

    public boolean shouldMatch() {
      return shouldMatch;
    }
  }

  /**
   * Convenience method to test if a lipid name matches a filter pattern
   *
   * @param filterPattern the pattern to match against (used to create a LipidRowTypeFilter)
   * @param lipidName     the lipid name to test
   * @return true if the lipid name matches the filter pattern
   * @throws QueryFormatException if the filter pattern is invalid
   */
  private boolean matchLipidName(String filterPattern, String lipidName)
      throws QueryFormatException {
    LipidRowTypeFilter filter = new LipidRowTypeFilter(filterPattern);
    return filter.matchesLipidName(lipidName);
  }

  @ParameterizedTest
  @EnumSource(LipidNotationType.class)
  void testLipidNotationMatching(LipidNotationType notationType) throws QueryFormatException {
    boolean matches = matchLipidName(notationType.getFilterPattern(),
        notationType.getTestLipidName());
    assertEquals(notationType.shouldMatch(), matches,
        "Filter '" + notationType.getFilterPattern() + "' should " + (notationType.shouldMatch()
            ? "match" : "not match") + " lipid name '" + notationType.getTestLipidName() + "'");
  }

  @Test
  void matchesLipidName() throws QueryFormatException {
    // Test basic matching
    assertTrue(matchLipidName("PC", "PC36:2"), "Class only should match");
    assertTrue(matchLipidName("C", "PC36:2"), "Any class should match");

    // Test species level matching
    assertTrue(matchLipidName("PC36:2", "PC36:2"), "Exact species match");
    assertFalse(matchLipidName("PC36:2", "PC34:2"), "Different carbon count should not match");
    assertTrue(matchLipidName("C36:2", "PC36:2"), "Any class with specific chains should match");

    // Test operators
    assertTrue(matchLipidName("PC>34:2", "PC36:2"), "Greater than carbon count should match");
    assertTrue(matchLipidName("PC36:>1", "PC36:2"), "Greater than double bond count should match");
    assertFalse(matchLipidName("PC>36:2", "PC36:2"),
        "Greater than carbon count should not match equal value");

    // Test molecular species level
    assertTrue(matchLipidName("PC18:1_18:1", "PC18:1_18:1"), "Exact molecular species match");
    assertTrue(matchLipidName("PC18:1/18:1", "PC18:1_18:1"),
        "Slash notation should match underscore");
    assertFalse(matchLipidName("PC18:1_18:1", "PC18:0_18:2"),
        "Different chain distribution should not match");

    // Test range notation
    assertTrue(matchLipidName("PC34:0 - PC38:4", "PC36:2"), "Lipid within range should match");
    assertFalse(matchLipidName("PC32:0 - PC34:2", "PC36:2"),
        "Lipid outside range should not match");
  }
}
