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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

class IonPartParserTest {

  record Case(String input, int charge, int count, String formula) {

  }

  final static List<Case> cases = List.of( //
      new Case("+C2ArS+", 1, 1, "C2ArS"),//
      // check if charge overwrite works
      new Case("+Na-", -1, 1, "Na"),//
      new Case("+Na-2", -2, 1, "Na"),//
// will be charge 1 because this is the predefined charge state of Na
      new Case("+Na", 1, 1, "Na"), //
      // regular cases
      new Case("+H", 1, 1, "H"),//
      new Case("+2H", 1, 2, "H"),//
      new Case("+(H)", 1, 1, "H"),//
      new Case("+2(H)", 1, 2, "H"),//
      // exotic formula for unknown charge and then check spaces
      new Case("  + CH  +", 1, 1, "CH"),//
      new Case("  + CH  +2", 2, 1, "CH"),//
      new Case("  + [CH] +2", 2, 1, "CH"),//
      new Case("  + [CH]2+", 2, 1, "CH"),//
      new Case("  + ([CH]2+)", 2, 1, "CH"),//
      new Case("  + ([CH] +2)", 2, 1, "CH"),//
      new Case("  + CH2+", 1, 1, "CH2"),//
      new Case("  + CH2+2", 2, 1, "CH2"),//
      // need to use some more exotic formula to avoid charge being known in predefined parts
      new Case("+C2ArS+1", 1, 1, "C2ArS"),//
      new Case("+C2ArS-2", -2, 1, "C2ArS"),//
      new Case("+C2ArS-2", -2, 1, "C2ArS"),//
      new Case("+(C2ArS)", 0, 1, "C2ArS"),//
      new Case("+(C2ArS+)", 1, 1, "C2ArS"),//
      new Case("+(C2ArS2+)", 1, 1, "C2ArS2"),//
      new Case("+(C2ArS-2)", -2, 1, "C2ArS"),//
      // notation of isotopes
      new Case("+[C5[13]CH12]+2", 2, 1, "C5[13]CH12"),//
      new Case("+C5[13]CH12+2", 2, 1, "C5[13]CH12"),//
      new Case("+(C5[13]CH12+2)", 2, 1, "C5[13]CH12"),//
      // electrons
      new Case("-e", -1, -1, null),//
      new Case("+e", -1, 1, null)//
  );

  @ParameterizedTest
  @FieldSource("cases")
  public void testParse(Case c) {
    IonPart part = IonPartParser.parse(c.input);
    final String message = "For input: " + c.input;
    assertNotNull(part, message);
    assertEquals(c.charge, part.singleCharge(), message);
    assertEquals(c.count, part.count(), message);
    assertEquals(c.formula, part.singleFormula(), message);
    assertTrue(part.absSingleMass() > 0, message);
  }

  @Test
  void testParseCase() {
    final Case c = new Case("  + [CH]2+", 2, 1, "CH");
    testParse(c);
  }

  @Test
  void isotopeFormula() {
    final IonPart part = IonParts.parse("+[C5[13]CH12]+2");
//    final IonPart part = IonParts.parse("+2C5[13]CH12+");
    assertNotNull(part);
    assertEquals(1, part.count());
    assertEquals(2, part.singleCharge());
  }

  @Test
  void testParseElectron() {
    final IonPart part = IonPartParser.parse("+2e");
    assertNotNull(part);
    assertEquals("e", part.name());
    assertEquals(-1, part.singleCharge());
    assertEquals(2, part.count());
    assertEquals(IonUtils.ELECTRON_MASS, part.absSingleMass(), 0.000001);
  }

  @Test
  void testParse1() {
    final IonPart part = IonPartParser.parse("+3C2ArS+1");
    assertNotNull(part);
    assertEquals("C2ArS", part.name());
    assertEquals(1, part.singleCharge());
    assertEquals(3, part.count());
  }

  @Test
  void testParse2() {
    final IonPart part = IonPartParser.parse("+3Na-");
    assertNotNull(part);
    assertEquals("Na", part.name());
    assertEquals(-1, part.singleCharge());
    assertEquals(3, part.count());
  }

  @Test
  public void testAllowedSpaces() {
    List<String> inputs = List.of( //
        // this input without braces () is not preferred. With charge prefer braces
        "-2H2O + H + +Fe +3  -2Na + + H2O", //
        "-2H2O+H++Fe+3-2Na++H2O", //
        "-2H2O+(H+)+(Fe+3)-2(Na+)+H2O", //
        "\t-2 H2O+( H+)+(Fe+3)-2 (Na+)+H2O", //
        "   -2H2O+(H+)+(Fe+3)-2(Na+)+H2O", //
        "  -2 H2O+ (H+) +(Fe+3)-2 (Na+)+(H2O) " //
    );

    final List<IonPart> expected = List.of(IonParts.H2O_2, IonParts.H, IonParts.FEIII,
        IonParts.NA.withCount(-2), IonParts.H2O.withCount(1));

    for (String input : inputs) {
      List<IonPart> parts = IonParts.parseMultiple(input);

      assertEquals(expected.size(), parts.size(), input);

      for (int i = 0; i < expected.size(); i++) {
        assertEquals(expected.get(i), parts.get(i), input);
      }
    }
  }

  @Test
  void testAddPartDefinition() {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();

    final IonPart unknown = IonPartParser.parse("+TE");
    assertEquals(1, unknown.count());
    assertEquals("TE", unknown.name());
    assertNull(unknown.singleFormula());
    assertEquals(0d, unknown.absSingleMass(), 0.0001);
    assertEquals(0, unknown.singleCharge());

    final IonPartDefinition def = IonPartDefinition.ofFormula("TE", "C3H6", 1);
    global.addPartDefinition(def);

    final IonPart known = IonPartParser.parse("+TE");
    assertNotNull(known);
    assertEquals(def.singleFormula(), known.singleFormula());
    assertEquals(def.absSingleMass(), known.absSingleMass());
    assertEquals(def.singleCharge(), known.singleCharge());
  }

}