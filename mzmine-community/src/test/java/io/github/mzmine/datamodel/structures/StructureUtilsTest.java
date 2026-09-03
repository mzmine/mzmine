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