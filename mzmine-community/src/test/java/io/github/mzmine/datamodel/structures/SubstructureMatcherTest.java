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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.structures.SubstructureMatcher.StructureMatchMode;
import org.junit.jupiter.api.Test;

class SubstructureMatcherTest {

  @Test
  void testSmartsWithStereo() {

    assertTrue(matchSmarts("C[C@H](O)C", "C[CH]"));
    assertTrue(matchSmarts("C[C@H](O)C", "C[C@H]"));
    assertTrue(matchSmarts("C[C@H](O)C", "C[C@H]"));
    assertTrue(matchSmarts("C[C@@H](O)C", "C[C@@H]"));
    assertTrue(matchSmarts("C/C=C/C", "C=C"));
    assertTrue(matchSmarts("C/C=C\\C", "C=C"));
    assertTrue(matchSmarts("C/C=C\\C", "C/C"));
    assertTrue(matchSmarts("C/C=C\\C", "C-C"));
    assertTrue(matchSmarts("C/C=C\\C", "C=C\\C"));

    // sulfate
    assertTrue(matchSmarts("CCOS(=O)(=O)O",
        "[$([#16X4](=[OX1])(=[OX1])([OX2H,OX1H0-])[OX2][#6]),$([#16X4+2]([OX1-])([OX1-])([OX2H,OX1H0-])[OX2][#6])]"));
    // sulfone
    assertTrue(matchSmarts("CS(=O)(=O)O", "[$([#16X4](=[OX1])=[OX1]),$([#16X4+2]([OX1-])[OX1-])]"));
    assertTrue(
        matchSmarts("CS(=O)(=O)OC", "[$([#16X4](=[OX1])=[OX1]),$([#16X4+2]([OX1-])[OX1-])]"));

    // correct mismatch
    assertFalse(matchSmarts("C\\C=C\\C", "C/C=C\\C"));
    assertFalse(matchSmarts("C[C@H](F)O", "C[C@@H](F)O"));
    assertFalse(matchSmarts("C[C@@H](F)O", "C[C@H](F)O"));

    // aromatic
    assertTrue(matchSmarts("c1ccccc1", "c"));
    assertFalse(matchSmarts("c1ccccc1", "ccccccccc"));
    assertFalse(matchSmarts("CCCC", "a"));
    // aliphatic
    assertFalse(matchSmarts("c1ccccc1", "AAA"));
    assertTrue(matchSmarts("CCCC", "AAA"));

  }

  @Test
  void testSmarts() {
    assertTrue(matchSmarts("CC(O)C", "[OH]"));
    assertTrue(matchSmarts("CC(O)C", "[#6][OH]"));
    assertTrue(matchSmarts("CC(N)C", "[NH2]"));
    assertTrue(matchSmarts("CCC(=O)O", "C=O"));
    assertTrue(matchSmarts("CCC(=O)O", "[#6]=O"));
    assertTrue(matchSmarts("c1ccccc1C", "c1ccccc1"));
    assertTrue(matchSmarts("CC#CC", "[#6X2]"));
    assertFalse(matchSmarts("CCCC", "[#6X2]"));
    assertTrue(matchSmarts("C(F)(F)C(F)(F)C(F)(F)C", "C(F)(F)C(F)(F)C(F)(F)"));

    // not enough CF2
    assertFalse(matchSmarts("C(F)(F)C(F)CC", "C(F)(F)C(F)(F)C(F)(F)"));
  }

  boolean matchSmarts(String smiles, String smarts) {
    var target = StructureParser.silent().parseStructure(smiles, StructureInputType.SMILES);
    assertNotNull(target, "SMILES was wrong: " + smiles);
    // SMARTS for the xanthine core

    final SubstructureMatcher matcher = SubstructureMatcher.fromSmarts(smarts);
    assertNotNull(matcher, "SMARTS was wrong: " + smarts);
    return matcher.matches(target);
  }

  @Test
  void testSmilesSubMultipleMolecules() {
    String glycerol = "C(C(CO)O)O";

    // contains 3 OH
    testSmiles(true, glycerol, "OH.OH.OH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(true, glycerol, "OH.OH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(true, glycerol, "COH.COH.COH", StructureMatchMode.SUBSTRUCTURE);
    // this also match COC as there is no H provided
    testSmiles(true, glycerol, "O.O.O", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(true, glycerol, "O.O", StructureMatchMode.SUBSTRUCTURE);

    // not 4OH
    testSmiles(false, glycerol, "O.O.O.OH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(false, glycerol, "OH.OH.OH.OH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(false, glycerol, "O.O.O.O", StructureMatchMode.SUBSTRUCTURE);

    // contains 1 OH
    String Carboxy_PEG5_t_butyl_ester = "CC(C)(C)OC(=O)CCOCCOCCOCCOCCOCCC(=O)O";
    testSmiles(false, Carboxy_PEG5_t_butyl_ester, "OH.OH.OH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(false, Carboxy_PEG5_t_butyl_ester, "COH.COH.COH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(true, Carboxy_PEG5_t_butyl_ester, "O.O.O", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(true, Carboxy_PEG5_t_butyl_ester, "O.O.O.OH", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(true, Carboxy_PEG5_t_butyl_ester, "O.O.O.OH.C=O", StructureMatchMode.SUBSTRUCTURE);
  }

  @Test
  void testSmilesSub() {
    testSmiles(true, "CCC(C)CC1=CC=C(C=C1)C(C)C(=O)O", "C(C)C(=O)O",
        StructureMatchMode.SUBSTRUCTURE);
    testSmiles(false, "CC1=CC=C(C=C1)C(C)C(=N)O", "C(C)C(=O)O", StructureMatchMode.SUBSTRUCTURE);
    testSmiles(false, "CC1=CC=C(C=C1)C(C)C(=N)O", "C(C)C(=O)O", StructureMatchMode.SUBSTRUCTURE);
  }

  @Test
  void testSmilesExact() {
    // should fail
    testSmiles(false, "CC(C)CC1=CC=C(C=C1)C(C)C(=O)O", "CC(C)C", StructureMatchMode.EXACT);
    testSmiles(true, "CC(OH)", "CCO", StructureMatchMode.EXACT);
    testSmiles(true, "CCO", "CC(OH)", StructureMatchMode.EXACT);
    testSmiles(true, "C([C@H]1[C@@H]([C@H]([C@@H](C(O1)O)O)O)O)O", "OCC1OC(O)C(O)C(O)C1O",
        StructureMatchMode.EXACT);

    // isomeric query will ask for isomeric answer
    testSmiles(false, "OCC1OC(O)C(O)C(O)C1O", "C([C@H]1[C@@H]([C@H]([C@@H](C(O1)O)O)O)O)O",
        StructureMatchMode.EXACT);

    // this is a substructure but not exact
    testSmiles(true, "CCC(C)CC1=CC=C(C=C1)C(C)C(=O)O", "C(C)C(=O)O",
        StructureMatchMode.SUBSTRUCTURE);
    testSmiles(false, "CCC(C)CC1=CC=C(C=C1)C(C)C(=O)O", "C(C)C(=O)O", StructureMatchMode.EXACT);
  }

  private static void testSmiles(boolean expected, String smiles, String querySmiles,
      StructureMatchMode matching) {
    var target = StructureParser.silent().parseStructure(smiles, StructureInputType.SMILES);
    var query = StructureParser.silent().parseStructure(querySmiles, StructureInputType.SMILES);

    SubstructureMatcher matcher = SubstructureMatcher.fromStructure(query, matching);
    assertEquals(expected, matcher.matches(target));

    if (matching == StructureMatchMode.EXACT && expected) {
      // also needs to be a substructure match if exact is true
      matcher = SubstructureMatcher.fromStructure(query, StructureMatchMode.SUBSTRUCTURE);
      assertTrue(matcher.matches(target));
    }
  }


}