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

import io.github.mzmine.datamodel.identities.IonPart.IonPartStringFlavor;
import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

class IonPartParserTest {

  record Case(String input, int charge, int count, String formula) {

  }

  final static List<Case> cases = List.of( //
      new Case("+Na-", -1, 1, "Na"),//
      new Case("+(C-OH)", 0, 1, "CH4O"),//
      new Case("+2(C-OH+)", 1, 2, "CH4O"),//
      new Case("+2(C-OH+1)", 1, 2, "CH4O"),//
      new Case("+2(C-OH-)", -1, 2, "CH4O"),//
      new Case("+2(C-OH-1)", -1, 2, "CH4O"),//
      new Case("+H", 1, 1, "H"),//
      new Case("+2H", 1, 2, "H"),//
      new Case("+(H)", 1, 1, "H"),//
      new Case("+2(H)", 1, 2, "H"),//
// will be charge 1 because this is the predefined charge state of Na
      new Case("+Na", 1, 1, "Na"), //
      // need to use some more exotic formula to avoid charge being known in predefined parts
      new Case("+Ar+", 1, 1, "Ar"),//
      new Case("+Ar+1", 1, 1, "Ar"),//
      new Case("+Ar-2", -2, 1, "Ar"),//
      new Case("+Ar-2", -2, 1, "Ar"),//
      new Case("+(Ar)", 1, 1, "Ar"),//
      new Case("+(Ar+)", 1, 1, "Ar"),//
      new Case("+(Ar-2)", -2, 1, "Ar")//
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
  public void testParseBindings() {
    var charges = List.of(0, 2, -1);
    String input = "+2(C-OH) +  2(C-OH+2) + 2(C-OH-)";
    List<IonPart> parts = IonPartParser.parseMultiple(input);
    assertEquals(3, parts.size());
    for (int i = 0; i < parts.size(); i++) {
      final IonPart part = parts.get(i);
      assertEquals(charges.get(i), part.singleCharge());
      assertEquals("CH4O", part.name());
      assertEquals(2, part.count());
    }
  }

  @Test
  public void testParseSmiles() {
    final List<String> smiles = List.of("C#C-C", "C-OH", "C=O");
    final List<String> names = List.of("C3H4", "CH4O", "CH2O");
    final var charges = List.of(0, 2, -1);
    final var counts = List.of(-1, 1, 2);

    final List<IonPart> ions = IntStream.range(0, counts.size())
        .mapToObj(i -> new IonPart(smiles.get(i), charges.get(i), counts.get(i))).toList();
    var input = ions.stream().map(ion -> ion.toString(IonPartStringFlavor.SIMPLE_WITH_CHARGE))
        .collect(Collectors.joining(""));

    var parts = IonPartParser.parseMultiple(input);
    assertEquals(3, parts.size());
    for (int i = 0; i < parts.size(); i++) {
      final String message =
          "For part: " + ions.get(i).toString(IonPartStringFlavor.SIMPLE_WITH_CHARGE);

      final IonPart part = parts.get(i);
      assertEquals(charges.get(i), part.singleCharge(), message);
      assertEquals(names.get(i), part.name(), message);
      assertEquals(counts.get(i), part.count(), message);
    }
  }

  @Test
  public void testIonPartRegex() {
    Pattern pattern = IonPartParser.PART_PATTERN;

    List<String> inputs = List.of( //
        // this input without braces () is not preferred. With charge prefer braces
        "-2H2O + H + +Fe +3  -2Na + + H2O", //
        "-2H2O+H++Fe+3-2Na++H2O", //
        "-2H2O+(H+)+(Fe+3)-2(Na+)+H2O", //
        "\t-2 H2O+( H+)+(Fe+3)-2 (Na+)+H2O", //
        "   - 2H2O+(H+)+(Fe+3)-2(Na+)+H2O", //
        "  -2H2O+ (H+) +(Fe+3)-2 (Na+)+(H2O) " //
    );

    for (String input : inputs) {
      Matcher matcher = pattern.matcher(StringUtils.removeAllWhiteSpace(input));

      List<String> units = new ArrayList<>();

      while (matcher.find()) {
        units.add(matcher.group());
        String allGroups = IntStream.range(0, matcher.groupCount() + 1)
            .mapToObj(i -> "Group %d=%s".formatted(i, matcher.group(i)))
            .collect(Collectors.joining(" ; "));
//      logger.info(allGroups);
      }
      assertEquals(5, units.size(), "For input: " + input);

      List<IonPart> parts = IonPart.parseMultiple(input);
      assertEquals(5, parts.size(), "For input: " + input);

      IonPart part = parts.get(0);
      assertEquals("H2O", part.name(), "For input: " + input);
      assertEquals(0, part.singleCharge(), "For input: " + input);
      assertEquals(-2, part.count(), "For input: " + input);

      part = parts.get(1);
      assertEquals("H", part.name(), "For input: " + input);
      assertEquals(1, part.singleCharge(), "For input: " + input);
      assertEquals(1, part.count(), "For input: " + input);

      part = parts.get(2);
      assertEquals("Fe", part.name(), "For input: " + input);
      assertEquals(3, part.singleCharge(), "For input: " + input);
      assertEquals(1, part.count(), "For input: " + input);

      part = parts.get(3);
      assertEquals("Na", part.name(), "For input: " + input);
      assertEquals(1, part.singleCharge(), "For input: " + input);
      assertEquals(-2, part.count(), "For input: " + input);

      part = parts.get(4);
      assertEquals("H2O", part.name(), "For input: " + input);
      assertEquals(0, part.singleCharge(), "For input: " + input);
      assertEquals(1, part.count(), "For input: " + input);

//    for (String unit : units) {
//      logger.info(unit);
//    }
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

    final IonPartDefinition def = IonPartDefinition.ofFormula("TE", "T", 1);
    global.addPartDefinition(def);

    final IonPart known = IonPartParser.parse("+TE");
    assertNotNull(known);
    assertEquals(def.formula(), known.singleFormula());
    assertEquals(def.absMass(), known.absSingleMass());
    assertEquals(def.charge(), known.singleCharge());
  }

}