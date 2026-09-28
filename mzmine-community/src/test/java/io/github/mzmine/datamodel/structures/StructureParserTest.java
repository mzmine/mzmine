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

import io.github.mzmine.util.FormulaUtils;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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

class StructureParserTest {

  private static final Logger logger = Logger.getLogger(StructureParserTest.class.getName());

  record Case(String input, String formula, String isomericSmiles, String canonicalSmiles,
              int charge) {

  }

  // since the switch from the inchi round trip to StructureHarmonizer the protonation state is
  // neutralized by default, so a deprotonated acid is reported as the neutral molecule. That is the
  // mass adduct and m/z calculations need. Use HarmonizationOptions.KEEP_CHARGES to keep the input
  // state, see StructureHarmonizerTest.
  final static List<Case> cases = List.of( //
      new Case("CC(=O)O", "C2H4O2", "CC(=O)O", "CC(O)=O", 0) //
      , new Case("C(=O)[O-]", "CH2O2", "C(=O)O", "C(O)=O", 0) //
      , new Case("[12CH](=O)[O-]", "CH2O2", "[12CH](=O)O", "[12CH](O)=O", 0) //
      , new Case("[13CH](=O)[O-]", "[13]CH2O2", "[13CH](=O)O", "[13CH](O)=O", 0) //
      , new Case("C(=CCCC(C2)(C)Oc(c(C2)1)c(cc(O)c(C)1)C)(C)CCC=C(C)CCC=C(C)C", "C28H42O2",
          "CC(=CCCC(=CCCC(=CCCC1(C)CCC2=C(C)C(=CC(=C2O1)C)O)C)C)C",
          "CC(C)=CCCC(C)=CCCC(C)=CCCC1(C)CCC2=C(C)C(=CC(C)=C2O1)O", 0),
      new Case("C(CCCCCC)=CCC=CCC=CCC=CCCC(OC(COP(O)(=O)OCCN)COC=CCCCCCCCCCCCCCC)=O", "C41H74NO7P",
          "CCCCCCC=CCC=CCC=CCC=CCCC(=O)OC(COC=CCCCCCCCCCCCCCC)COP(=O)(O)OCCN",
          "CCCCCCC=CCC=CCC=CCC=CCCC(=O)OC(COC=CCCCCCCCCCCCCCC)COP(O)(=O)OCCN", 0),
      new Case("CC(C)CC(C(=O)NC(CC(C)C)C(=O)NC(CC1=CC=C(C=C1)O)C(=O)O)N", "C21H33N3O5",
          "CC(C)CC(C(=O)NC(CC(C)C)C(=O)NC(CC1=CC=C(C=C1)O)C(=O)O)N",
          "CC(C)CC(C(NC(CC(C)C)C(NC(CC1=CC=C(C=C1)O)C(O)=O)=O)=O)N", 0));

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
      Assertions.assertEquals(0, StructureUtils.getFormula(struc).getCharge(),
          "charge in formula mismatch");
      final IMolecularFormula formula = StructureUtils.getFormula(struc);
      final String formulaString = FormulaUtils.getFormulaString(formula);

      for (int j = 0; j < 1000; j++) {
        Assertions.assertEquals(3, StructureUtils.getFormula(struc).getIsotopeCount());
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

  @Test
  void testIsotopicStructure() {
    final String isotopicSmiles = "[2H]/C(CCCC(O)=O)=C([2H])/C/C([2H])=C([2H])\\C/C([2H])=C([2H])\\C=C([2H])\\C(CCCCC)([2H])O";

    MolecularStructure structure = StructureParser.silent()
        .parseStructure(isotopicSmiles, StructureInputType.SMILES);
    Assertions.assertEquals("C20H24[2]H8O3", structure.formulaString());
    Assertions.assertEquals(
        "CCCCCC(C(=CC(=C(CC(=C(CC(=C(CCCC(O)=O)[2H])[2H])[2H])[2H])[2H])[2H])[2H])(O)[2H]",
        structure.canonicalSmiles());
    Assertions.assertEquals(
        "CCCCCC([2H])(/C(/[2H])=C/C(/[2H])=C(/[2H])\\C/C(/[2H])=C(/[2H])\\C/C(/[2H])=C(/[2H])\\CCCC(=O)O)O",
        structure.isomericSmiles());
    Assertions.assertEquals("JSFATNQSLKRBCI-HAVWKUCESA-N", structure.inchiKey());
    Assertions.assertEquals(
        "InChI=1S/C20H32O3/c1-2-3-13-16-19(21)17-14-11-9-7-5-4-6-8-10-12-15-18-20(22)23/h4-5,8-11,14,17,19,21H,2-3,6-7,12-13,15-16,18H2,1H3,(H,22,23)/b5-4-,10-8-,11-9-,17-14+/i4D,5D,8D,9D,10D,11D,17D,19D",
        structure.inchi());

    final String isotopicInchi = "InChI=1S/C20H32O3/c1-2-3-13-16-19(21)17-14-11-9-7-5-4-6-8-10-12-15-18-20(22)23/h4-5,8-11,14,17,19,21H,2-3,6-7,12-13,15-16,18H2,1H3,(H,22,23)/b5-4-,10-8-,11-9-,17-14+/i4D,5D,8D,9D,10D,11D,17D,19D";
    structure = StructureParser.silent().parseStructure(isotopicInchi, StructureInputType.INCHI);
    Assertions.assertEquals("C20H24[2]H8O3", structure.formulaString());
    Assertions.assertEquals(
        "CCCCCC(C(=CC(=C(CC(=C(CC(=C(CCCC(O)=O)[2H])[2H])[2H])[2H])[2H])[2H])[2H])(O)[2H]",
        structure.canonicalSmiles());
    Assertions.assertEquals(
        "CCCCCC([2H])(/C(/[2H])=C/C(/[2H])=C(/[2H])\\C/C(/[2H])=C(/[2H])\\C/C(/[2H])=C(/[2H])\\CCCC(=O)O)O",
        structure.isomericSmiles());
    Assertions.assertEquals("JSFATNQSLKRBCI-HAVWKUCESA-N", structure.inchiKey());
    Assertions.assertEquals(
        "InChI=1S/C20H32O3/c1-2-3-13-16-19(21)17-14-11-9-7-5-4-6-8-10-12-15-18-20(22)23/h4-5,8-11,14,17,19,21H,2-3,6-7,12-13,15-16,18H2,1H3,(H,22,23)/b5-4-,10-8-,11-9-,17-14+/i4D,5D,8D,9D,10D,11D,17D,19D",
        structure.inchi());
  }

  record SourceEquivalence(String smiles, String inchi, boolean equal) {

    static SourceEquivalence equal(String smiles, String inchi) {
      return new SourceEquivalence(smiles, inchi, true);
    }

    static SourceEquivalence different(String smiles, String inchi) {
      return new SourceEquivalence(smiles, inchi, false);
    }

  }

  static List<SourceEquivalence> inchismiles = List.of(
      SourceEquivalence.equal("CCCCCCCCC=CCCCCCCCCCCCC(N)=O",
          "InChI=1S/C22H43NO/c1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22(23)24/h9-10H,2-8,11-21H2,1H3,(H2,23,24)"),
      SourceEquivalence.equal("CCCCCCCCC=CCCCCCCCCCCCC(=N)O",
          "InChI=1S/C22H43NO/c1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22(23)24/h9-10H,2-8,11-21H2,1H3,(H2,23,24)"),
      SourceEquivalence.equal("CCCCCCCC/C=C\\CCCCCCCCCCCC(=O)N",
          "InChI=1S/C22H43NO/c1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22(23)24/h9-10H,2-8,11-21H2,1H3,(H2,23,24)/b10-9-"),
      SourceEquivalence.equal("CCCCCCCC/C=C\\CCCCCCCCCCCC(=N)O",
          "InChI=1S/C22H43NO/c1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22(23)24/h9-10H,2-8,11-21H2,1H3,(H2,23,24)/b10-9-")
      //
  );

  @ParameterizedTest
  @FieldSource(value = "inchismiles")
  void checkEqualitySmilesInchi(SourceEquivalence c) {

    final MolecularStructure strSmi = StructureParser.silent()
        .parseStructureWithoutCache(c.smiles(), StructureInputType.SMILES);
    final MolecularStructure strInchi = StructureParser.silent()
        .parseStructureWithoutCache(c.inchi(), StructureInputType.INCHI);

    Assertions.assertNotNull(strSmi);
    Assertions.assertNotNull(strInchi);

    String input = """
        
        For input smiles: %s
             input inchi: %s
        expected is smiles result and actual is inchi result.""".formatted(c.smiles(), c.inchi());
    if (c.equal()) {
      Assertions.assertEquals(strSmi.inchi(), strInchi.inchi(), input);
      Assertions.assertEquals(strSmi.inchiKey(), strInchi.inchiKey(), input);
      Assertions.assertEquals(strSmi.isomericSmiles(), strInchi.isomericSmiles(), input);
    } else {
      Assertions.assertNotEquals(strSmi.inchi(), strInchi.inchi(), input);
      Assertions.assertNotEquals(strSmi.isomericSmiles(), strInchi.isomericSmiles(), input);
    }
  }

  /// only for generating tests
  @Test
  @Disabled
  void generateInchiSmilesTestCases() {

    var smiles = List.of("CCCCCCCCC=CCCCCCCCCCCCC(N)=O", "CCCCCCCCC=CCCCCCCCCCCCC(=N)O",
        "CCCCCCCC/C=C\\CCCCCCCCCCCC(=O)N", "CCCCCCCC/C=C\\CCCCCCCCCCCC(=N)O");

    StringBuilder sb = new StringBuilder("New Lines\n");
    for (String s : smiles) {
      final String inchi = StructureParser.silent()
          .parseStructureWithoutCache(s, StructureInputType.SMILES).inchi();

      sb.append(", SourceEquivalence.equal(\"%s\", \"%s\")".formatted(
          s.replace("\\", "\\\\").replace("\"", "\\\""),
          inchi.replace("\\", "\\\\").replace("\"", "\\\"")));
    }

    logger.info(sb.toString());
  }

  /// inchi applies a standardization that is not done through smiles parser. So smiles -> inchi ->
  /// smiles may differ. We will try to always use inchi parser for now. Like converting smiles to
  /// inchi and then back to smiles.
  @Test
  @Disabled
  void checkOutputs() {
    final String inSmiles = "CCCCCCCCC=CCCCCCCCCCCCC(N)=O.[Na]";
//    final String inSmiles = "CCCCCCCCC=CCCCCCCCCCCCC(N[Na])=O";
//    final String inSmiles = "CCCCCCCCC=CCCCCCCCCCCCC(=N)O";
    MolecularStructure mol = StructureParser.silent()
        .parseStructureWithoutCache(inSmiles, StructureInputType.SMILES);

    final MolecularStructure molInchi = StructureParser.silent()
        .parseStructureWithoutCache(mol.inchi(), StructureInputType.INCHI);

    logger.info("""
        
        from smiles
        iso: %s
        can: %s
        inc: %s
        key: %s
        
        from inchi
        iiso: %s
        ican: %s
        iinc: %s
        ikey: %s
        """.formatted(mol.isomericSmiles(), mol.canonicalSmiles(), mol.inchi(), mol.inchiKey(), //
        molInchi.isomericSmiles(), molInchi.canonicalSmiles(), molInchi.inchi(),
        molInchi.inchiKey()));
  }

}