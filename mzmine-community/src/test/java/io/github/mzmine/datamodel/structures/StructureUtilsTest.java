/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.datamodel.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.structures.StructureUtils.HydrogenFlavor;
import io.github.mzmine.datamodel.structures.StructureUtils.SmilesFlavor;
import io.github.mzmine.util.FormulaUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StructureUtilsTest {

  /**
   * CDK marks every double bond {@code E_Z_BY_COORDINATES} by default, so a naive check reports
   * stereo chemistry for benzene. Real stereo is carried by the stereo elements.
   */
  @Test
  void testHasStereoChemistry() {
    assertEquals(false, hasStereo("CCCO"), "no double bond, no stereo");
    assertEquals(false, hasStereo("c1ccccc1"), "benzene has no stereo chemistry");
    assertEquals(false, hasStereo("CC=CC"), "an unspecified double bond is not stereo chemistry");
    assertEquals(false, hasStereo("CC(=O)Nc1ccc(O)cc1"), "paracetamol has no stereo chemistry");
    assertEquals(true, hasStereo("C/C=C/C"), "specified E double bond");
    assertEquals(true, hasStereo("C[C@H](N)C(O)=O"), "tetrahedral center");
  }

  private static boolean hasStereo(String smiles) {
    final MolecularStructure structure = StructureParser.silent()
        .parseStructureWithoutCache(smiles, StructureInputType.SMILES);
    return StructureUtils.hasStereoChemistry(structure.structure());
  }

  /**
   * The two smiles flavors are not interchangeable, not even for a structure without stereo
   * chemistry, because SmiFlavor.Stereo also changes the traversal order of the writer. Wherever
   * both are needed both have to be generated.
   */
  @Test
  void testSmilesFlavorsAreNotInterchangeable() {
    final MolecularStructure noStereo = StructureParser.silent()
        .parseStructureWithoutCache("CC(C)=CCCC(C)=CCCC(C)=CCCC1(C)CCC2=C(C)C(=CC(C)=C2O1)O",
            StructureInputType.SMILES);
    assertEquals(false, StructureUtils.hasStereoChemistry(noStereo.structure()),
        "double bonds alone are not defined stereo chemistry");
    Assertions.assertNotEquals(
        StructureUtils.getSmiles(SmilesFlavor.CANONICAL, noStereo.structure()),
        StructureUtils.getSmiles(SmilesFlavor.ISOMERIC, noStereo.structure()),
        "the flavors differ even without stereo chemistry");
  }

  @Test
  void testHasExplicitHydrogens() {
    final MolecularStructure suppressed = StructureParser.silent()
        .parseStructureWithoutCache("CC(OH)", StructureInputType.SMILES);
    assertEquals(false, StructureUtils.hasExplicitHydrogens(suppressed.structure()),
        "the parser suppresses hydrogens by default");
    final MolecularStructure query = StructureParser.silent()
        .parseStructureWithoutCache("CC(OH)", StructureInputType.SMILES,
            HarmonizationOptions.QUERY);
    assertEquals(true, StructureUtils.hasExplicitHydrogens(query.structure()),
        "query options keep explicit hydrogens");
  }

  /**
   * Unlike in molecular formulas, D and T are valid SMILES atoms. CDK reads them as [2H] and [3H],
   * but only as single atoms/
   */
  @Test
  void testParseDeuteriumSmiles() {
    assertEquals("[2]H2O", formulaOfSmiles("[2H]O[2H]"));
    assertEquals("[2]H2O", formulaOfSmiles("DOD"));
    assertEquals("[2]H", formulaOfSmiles("[2H]"));
    assertEquals("[3]H2O", formulaOfSmiles("[3H]O[3H]"));
    assertEquals("[3]H", formulaOfSmiles("T"));
    // methanol-d4 and d3, mixed with regular hydrogens
    assertEquals("C[2]H4O", formulaOfSmiles("C([2H])([2H])([2H])O[2H]"));
    assertEquals("CH[2]H3O", formulaOfSmiles("C([2H])([2H])([2H])O"));

    // D followed by a digit is a ring bond in SMILES, so the formula shorthand does not parse here
    // even though FormulaUtils.parse accepts it
    Assertions.assertNull(
        StructureParser.silent().parseStructureWithoutCache("CD3OD", StructureInputType.SMILES));
    Assertions.assertNull(
        StructureParser.silent().parseStructureWithoutCache("D2O", StructureInputType.SMILES));
  }

  @Test
  void testDeuteriumSmilesMass() {
    final MolecularStructure d2o = parse("[2H]O[2H]", StructureInputType.SMILES);
    assertEquals(20.023118176, StructureUtils.getMonoIsotopicMass(d2o.structure()), 0.000001);
    final MolecularStructure h2o = parse("O", StructureInputType.SMILES);
    assertEquals(18.010564684, StructureUtils.getMonoIsotopicMass(h2o.structure()), 0.000001);
  }

  /**
   * Deuterium is carried in the InChI isotope layer (/i) and is parsed back from it. The InChIKey
   * differs in the second block, so a labeled standard is not equal to its unlabeled analog.
   */
  @Test
  void testDeuteriumInchi() {
    final MolecularStructure paracetamol = parse("CC(=O)Nc1ccc(O)cc1", StructureInputType.SMILES);
    final MolecularStructure paracetamolD3 = parse("[2H]C([2H])([2H])C(=O)Nc1ccc(O)cc1",
        StructureInputType.SMILES);

    assertEquals("C8H6[2]H3NO2", formula(paracetamolD3));
    assertEquals("InChI=1S/C8H9NO2/c1-6(10)9-7-2-4-8(11)5-3-7/h2-5,11H,1H3,(H,9,10)/i1D3",
        StructureUtils.getInchi(paracetamolD3.structure()));
    assertEquals("RZVAJINKPMORJF-UHFFFAOYSA-N",
        StructureUtils.getInchiKey(paracetamol.structure()));
    assertEquals("RZVAJINKPMORJF-FIBGUPNXSA-N",
        StructureUtils.getInchiKey(paracetamolD3.structure()));
    assertEquals(false,
        StructureUtils.equalsInchiKey(paracetamol.structure(), paracetamolD3.structure()));

    // and the isotope layer is read back
    final MolecularStructure fromInchi = parse("InChI=1S/H2O/h1H2/i/hD2", StructureInputType.INCHI);
    assertEquals("[2]H2O", formula(fromInchi));
    assertEquals(20.023118176, StructureUtils.getMonoIsotopicMass(fromInchi.structure()), 0.000001);
    assertEquals("XLYOFNOQVPJJNP-ZSJDYOACSA-N", StructureUtils.getInchiKey(fromInchi.structure()));
  }

  private static MolecularStructure parse(String input, StructureInputType type) {
    final MolecularStructure structure = StructureParser.silent()
        .parseStructureWithoutCache(input, type);
    Assertions.assertNotNull(structure, "Cannot parse " + input);
    return structure;
  }

  private static String formulaOfSmiles(String smiles) {
    return formula(parse(smiles, StructureInputType.SMILES));
  }

  private static String formula(MolecularStructure structure) {
    return FormulaUtils.getFormulaString(StructureUtils.getFormula(structure.structure()));
  }

  @Test
  void testEqual() {
    test(true, true, "CCO", "CC(OH)");
    test(false, false, "CCO", "CCC(OH)");
    // glucose
    test(true, false, "OCC1OC(O)C(O)C(O)C1O", "C([C@H]1[C@@H]([C@H]([C@@H](C(O1)O)O)O)O)O");
  }

  void test(boolean expected, boolean isomericExpected, String smiles1, String smiles2) {
    var target = StructureParser.silent().parseStructure(smiles1, StructureInputType.SMILES);
    var query = StructureParser.silent().parseStructure(smiles2, StructureInputType.SMILES);

    testEqualSmiles(expected, isomericExpected, query, target);
    testEqualInchiKey(expected, isomericExpected, query, target);
  }

  void testEqualInchiKey(boolean expected, boolean isomericExpected, MolecularStructure a,
      MolecularStructure b) {
    assertEquals(isomericExpected, StructureUtils.equalsInchiKey(a.structure(), b.structure()));

    assertEquals(expected, StructureUtils.equalsInchiKey(a.structure(), b.structure(), true));
    // hydrogens should not change anything
    assertEquals(expected, StructureUtils.equalsInchiKey(
        StructureUtils.harmonize(a.structure(), HydrogenFlavor.CONVERT_IMPLICIT_TO_EXPLICIT, true,
            false),
        StructureUtils.harmonize(b.structure(), HydrogenFlavor.UNCHANGED, true, false)));
    assertEquals(expected, StructureUtils.equalsInchiKey(
        StructureUtils.harmonize(a.structure(), HydrogenFlavor.REMOVE_NON_CHIRAL_HYDROGENS, true,
            false),
        StructureUtils.harmonize(b.structure(), HydrogenFlavor.REMOVE_NON_CHIRAL_HYDROGENS, true,
            false)));
  }

  void testEqualSmiles(boolean expected, boolean isomericExpected, MolecularStructure a,
      MolecularStructure b) {
    assertEquals(expected,
        StructureUtils.equalsSmiles(a.structure(), b.structure(), SmilesFlavor.CANONICAL));
    assertEquals(isomericExpected,
        StructureUtils.equalsSmiles(a.structure(), b.structure(), SmilesFlavor.ISOMERIC));
  }
}