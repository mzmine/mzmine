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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.structures.SubstructureMatcher.StructureMatchMode;
import org.junit.jupiter.api.Test;

class SubstructureMatcherTest {

  @Test
  void testSmartsSubstructure2() {
//    matchSmarts("C1=CC=CC=C1", "[#6]1[#6](=[#8])[#6]1");
    assertTrue(matchSmarts("CC(O)C", "[OH]"));
    assertTrue(matchSmarts("CC(O)C", "[#6][OH]"));
    assertTrue(matchSmarts("CC(N)C", "[NH2]"));
    assertTrue(matchSmarts("CCC(=O)O", "C=O"));
    assertTrue(matchSmarts("CCC(=O)O", "[#6]=O"));
    assertTrue(matchSmarts("c1ccccc1C", "c1ccccc1"));
    assertTrue(matchSmarts("CC#CC", "[#6X2]"));
    assertFalse(matchSmarts("CCCC", "[#6X2]"));
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
  void testSmilesSubstructure() {
    var target = StructureParser.silent()
        .parseStructure("CCC(C)CC1=CC=C(C=C1)C(C)C(=O)O", StructureInputType.SMILES);
    var failing = StructureParser.silent()
        .parseStructure("CC1=CC=C(C=C1)C(C)C(=O)O", StructureInputType.SMILES);
    var query = StructureParser.silent().parseStructure("CC(C)C", StructureInputType.SMILES);

    final SubstructureMatcher matcher = SubstructureMatcher.fromStructure(query,
        StructureMatchMode.SUBSTRUCTURE);
    assertTrue(matcher.matches(target));
    assertFalse(matcher.matches(failing));
  }

  @Test
  void testSmilesExact() {
    // should fail
    var target = StructureParser.silent()
        .parseStructure("CC(C)CC1=CC=C(C=C1)C(C)C(=O)O", StructureInputType.SMILES);
    var query = StructureParser.silent().parseStructure("CC(C)C", StructureInputType.SMILES);

    SubstructureMatcher matcher = SubstructureMatcher.fromStructure(query,
        StructureMatchMode.EXACT);
    assertFalse(matcher.matches(target));

    // second case
    target = StructureParser.silent().parseStructure("CC(OH)", StructureInputType.SMILES);
    query = StructureParser.silent().parseStructure("CCO", StructureInputType.SMILES);

    matcher = SubstructureMatcher.fromStructure(query, StructureMatchMode.EXACT);
    assertTrue(matcher.matches(target));

    // glucose stereo matches non-stereo
    target = StructureParser.silent()
        .parseStructure("C([C@H]1[C@@H]([C@H]([C@@H](C(O1)O)O)O)O)O", StructureInputType.SMILES);
    query = StructureParser.silent()
        .parseStructure("OCC1OC(O)C(O)C(O)C1O", StructureInputType.SMILES);

    matcher = SubstructureMatcher.fromStructure(query, StructureMatchMode.EXACT);
    assertTrue(matcher.matches(target));

    // flipped target: non-stereo matches glucose stereo
    target = StructureParser.silent()
        .parseStructure("OCC1OC(O)C(O)C(O)C1O", StructureInputType.SMILES);
    query = StructureParser.silent()
        .parseStructure("C([C@H]1[C@@H]([C@H]([C@@H](C(O1)O)O)O)O)O", StructureInputType.SMILES);

    matcher = SubstructureMatcher.fromStructure(query, StructureMatchMode.EXACT);
    assertTrue(matcher.matches(target));
  }


}