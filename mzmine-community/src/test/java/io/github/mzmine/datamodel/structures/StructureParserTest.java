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

import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

class StructureParserTest {

  record Case(String input, String formula, String isomericSmiles, String canonicalSmiles,
              int charge) {

  }

  final static List<Case> cases = List.of( //
      new Case("CC(=O)O", "C2H4O2", "CC(=O)O", "O=C(O)C", 0) //
//      , new Case("C(=O)[O-]", "[CHO2]-", "C(=O)[O-]", "O=C[O-]", -1) //
//      , new Case("[12CH](=O)[O-]", "[CHO2]-", "C(=O)[O-]", "O=C[O-]", -1) //
//      , new Case("[13CH](=O)[O-]", "[[13]CHO2]-", "C(=O)[O-]", "O=C[O-]", -1) //
  );

  @ParameterizedTest
  @FieldSource(value = "cases")
  void parseChargedStructure(Case c) {
    StructureParser parser = new StructureParser(true);
    final MolecularStructure struc = parser.parseStructure(c.input, StructureInputType.SMILES);
    Assertions.assertNotNull(struc);
    Assertions.assertEquals(c.charge, struc.totalFormalCharge(), "charge mismatch");
    Assertions.assertEquals(c.charge, struc.formula().getCharge(), "charge in formula mismatch");
//    final IMolecularFormula formula = struc.formula();
//    Assertions.assertEquals(c.formula, FormulaUtils.getFormulaString(formula), "formula mismatch");
    Assertions.assertEquals(c.formula, struc.formulaString(), "formula mismatch");
    Assertions.assertEquals(c.canonicalSmiles, struc.canonicalSmiles(), "canonicalSmiles mismatch");
    Assertions.assertEquals(c.isomericSmiles, struc.isomericSmiles(), "isomericSmiles mismatch");
  }

  @Test
  void parseStructureSmiles() {
    StructureParser parser = new StructureParser(false);
    var structure = parser.parseStructure("CCCO", StructureInputType.SMILES);
    Assertions.assertNotNull(structure);
  }

  @Test
  void parseStructureInchi() {
    StructureParser parser = new StructureParser(false);
    var structure = parser.parseStructure("InChI=1S/C2H4O2/c1-2(3)4/h1H3,(H,3,4)",
        StructureInputType.INCHI);
    Assertions.assertNotNull(structure);
  }

  @Test
  void testParseStructure() {
    var structure = StructureParser.silent()
        .parseStructure("CCCO", "InChI=1S/C2H4O2/c1-2(3)4/h1H3,(H,3,4)");
    Assertions.assertNotNull(structure);
  }

  @Test
  void testParseFailingFormula() throws InvalidSmilesException {
    String smiles = "CC(=O)O";

    int CHOO = 0;
    int CHO2 = 0;
    final int iterations = 1000;
    for (int i = 0; i < iterations; i++) {
      IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();
      final SmilesParser parser = new SmilesParser(builder);
      final IAtomContainer struc = parser.parseSmiles(smiles);
      Assertions.assertNotNull(struc);
      Assertions.assertEquals(0, AtomContainerManipulator.getTotalFormalCharge(struc),
          "charge mismatch");
      Assertions.assertEquals(0, MolecularFormulaManipulator.getMolecularFormula(struc).getCharge(),
          "charge in formula mismatch");
      final IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(struc);
      final String formulaString = MolecularFormulaManipulator.getString(formula);

      for (int j = 0; j < 1000; j++) {
        Assertions.assertEquals(3,
            MolecularFormulaManipulator.getMolecularFormula(struc).getIsotopeCount());
      }
      for (int j = 0; j < 1000; j++) {
        Assertions.assertEquals(3, StructureUtils.getFormula(struc).getIsotopeCount());
      }
      if (formulaString.equals("CCH4OO")) {
        CHOO++;
      } else if (formulaString.equals("C2H4O2")) {
        CHO2++;
      }
    }

    Assertions.assertEquals(0, CHOO);
    Assertions.assertEquals(iterations, CHO2);
  }

  @Test
  void testParseFailingFormulaInternal() throws InvalidSmilesException {
    String smiles = "CC(=O)O";

    int CHOO = 0;
    int CHO2 = 0;
    final int iterations = 1000;
    for (int i = 0; i < iterations; i++) {
      StructureParser parser = new StructureParser(true);
      final MolecularStructure struc = parser.parseStructure(smiles, StructureInputType.SMILES);
      Assertions.assertNotNull(struc);
//      Assertions.assertEquals(0, struc.totalFormalCharge(), "charge mismatch");
//      Assertions.assertEquals(0, struc.formula().getCharge(), "charge in formula mismatch");
      final String formulaString1 = struc.formulaString();
//    final IMolecularFormula formula = struc.formula();
//    Assertions.assertEquals(c.formula, FormulaUtils.getFormulaString(formula), "formula mismatch");

      for (int j = 0; j < 1000; j++) {
        Assertions.assertEquals(3, struc.formula().getIsotopeCount());
      }

      final String formulaString = struc.formulaString();
      if (formulaString.equals("CCH4OO")) {
        CHOO++;
      } else if (formulaString.equals("C2H4O2")) {
        CHO2++;
      }
    }

    Assertions.assertEquals(0, CHOO);
    Assertions.assertEquals(iterations, CHO2);
  }

  @Test
  void testParseFailingFormulaInternal2() throws InvalidSmilesException {
    String smiles = "CC(=O)O";

    int CHOO = 0;
    int CHO2 = 0;
    final int iterations = 100;
    StructureParser parser = new StructureParser(true);
    final MolecularStructure struc = parser.parseStructure(smiles, StructureInputType.SMILES);
    Assertions.assertNotNull(struc);
    for (int i = 0; i < iterations; i++) {
      final IMolecularFormula formula = struc.formula();
//      final IMolecularFormula formula = MolecularFormulaManipulator.getMolecularFormula(
//          struc.structure());
      final String formulaString = FormulaUtils.getFormulaString(formula);
      if (formulaString.equals("CCH4OO")) {
        CHOO++;
      } else if (formulaString.equals("C2H4O2")) {
        CHO2++;
      }
    }

    Assertions.assertEquals(0, CHOO);
    Assertions.assertEquals(iterations, CHO2);
  }
}