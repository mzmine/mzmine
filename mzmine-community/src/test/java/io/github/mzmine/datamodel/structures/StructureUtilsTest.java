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

package io.github.mzmine.datamodel.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.structures.StructureUtils.HydrogenFlavor;
import io.github.mzmine.datamodel.structures.StructureUtils.SmilesFlavor;
import org.junit.jupiter.api.Test;

class StructureUtilsTest {

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