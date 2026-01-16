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

package io.github.mzmine.datamodel.identities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.identities.iontype.IonPartDefinition;
import io.github.mzmine.util.FormulaUtils;
import org.junit.jupiter.api.Test;

class IonPartDefinitionTest {

  @Test
  void ofFormula() {
  }

  @Test
  void parse() {
    IonPartDefinition parsed = IonPartDefinition.parse("Cu+2");
    assertNotNull(parsed);
    assertEquals(2, parsed.singleCharge());
    assertTrue(parsed.isCharged());
    assertEquals("Cu", parsed.name());
    assertEquals("Cu", parsed.singleFormula());
    assertEquals(FormulaUtils.calculateExactMass("Cu", 2), parsed.absSingleMass(), 0.0000001);

    parsed = IonPartDefinition.parse("Cu+");
    assertNotNull(parsed);
    assertEquals(1, parsed.singleCharge());
    assertTrue(parsed.isCharged());
    assertEquals("Cu", parsed.name());
    assertEquals("Cu", parsed.singleFormula());
    assertEquals(FormulaUtils.calculateExactMass("Cu", 1), parsed.absSingleMass(), 0.0000001);
  }
}