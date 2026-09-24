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

  /// Mo(CO)5 bound to a bicyclic C7 hydrocarbon, as a V2000 connection table. decision: this block
  /// starts directly at the counts line, the three molfile header lines (title, program, comment)
  /// are missing. That is how several databases hand out molfiles, so it is the case worth
  /// testing.
  private static final String MOL_V2000 = """
       18 20  0  0  0  0  0  0  0  0999 V2000
         15.0000  -17.0000    0.0000 O   0  0  0     0  3  0  0  0  0
         11.0000   -1.0000    0.0000 C   0  0  0     0  0  0  0  0  0
         36.0000   -1.0000    0.0000 O   0  0  0     0  3  0  0  0  0
         21.0000    7.0000    0.0000 C   0  0  0     0  0  0  0  0  0
         40.0000   15.0000    0.0000 O   0  0  0     0  3  0  0  0  0
         23.0000   15.0000    0.0000 C   0  0  0     0  0  0  0  0  0
         36.0000   32.0000    0.0000 O   0  0  0     0  3  0  0  0  0
         21.0000   24.0000    0.0000 C   0  0  0     0  0  0  0  0  0
         23.0000   44.0000    0.0000 O   0  0  0     0  3  0  0  0  0
         15.0000   29.0000    0.0000 C   0  0  0     0  0  0  0  0  0
          6.0000   15.0000    0.0000 Mo  0  0  0     0  7  0  0  0  0
         -4.0000   -4.0000    0.0000 C   0  0  0     0  0  0  0  0  0
        -22.0000    8.0000    0.0000 C   0  0  0     0  0  0  0  0  0
         -9.0000   16.0000    0.0000 C   0  0  0     0  0  0  0  0  0
        -27.0000   28.0000    0.0000 C   0  0  0     0  0  0  0  0  0
          5.0000   33.0000    0.0000 C   0  0  0     0  0  0  0  0  0
        -12.0000   46.0000    0.0000 C   0  0  0     0  0  0  0  0  0
        -40.0000   24.0000    0.0000 C   0  0  0     0  0  0  0  0  0
        1  2  3  0  0  0  0
        2 11  1  0  0  0  0
        3  4  3  0  0  0  0
        4 11  1  0  0  0  0
        5  6  3  0  0  0  0
        6 11  1  0  0  0  0
        7  8  3  0  0  0  0
        8 11  1  0  0  0  0
        9 10  3  0  0  0  0
       10 11  1  0  0  0  0
       11 12  1  0  0  0  0
       11 13  1  0  0  0  0
       12 14  1  0  0  0  0
       12 13  1  0  0  0  0
       13 15  1  0  0  0  0
       14 16  1  0  0  0  0
       14 18  1  0  0  0  0
       15 17  1  0  0  0  0
       15 18  1  0  0  0  0
       16 17  1  0  0  0  0
      M  END""";

  /// the same molecule as {@link #MOL_V2000} written as a V3000 connection table, where the counts
  /// line no longer carries the counts and the tables moved into the M V30 CTAB block. Also without
  /// header lines.
  private static final String MOL_V3000 = """
        0  0  0     0  0            999 V3000
      M  V30 BEGIN CTAB
      M  V30 COUNTS 18 20 0 0 0
      M  V30 BEGIN ATOM
      M  V30 1 O 15 -17 0 0
      M  V30 2 C 11 -1 0 0
      M  V30 3 O 36 -1 0 0
      M  V30 4 C 21 7 0 0
      M  V30 5 O 40 15 0 0
      M  V30 6 C 23 15 0 0
      M  V30 7 O 36 32 0 0
      M  V30 8 C 21 24 0 0
      M  V30 9 O 23 44 0 0
      M  V30 10 C 15 29 0 0
      M  V30 11 Mo 6 15 0 0
      M  V30 12 C -4 -4 0 0
      M  V30 13 C -22 8 0 0
      M  V30 14 C -9 16 0 0
      M  V30 15 C -27 28 0 0
      M  V30 16 C 5 33 0 0
      M  V30 17 C -12 46 0 0
      M  V30 18 C -40 24 0 0
      M  V30 END ATOM
      M  V30 BEGIN BOND
      M  V30 1 3 1 2
      M  V30 2 1 2 11
      M  V30 3 3 3 4
      M  V30 4 1 4 11
      M  V30 5 3 5 6
      M  V30 6 1 6 11
      M  V30 7 3 7 8
      M  V30 8 1 8 11
      M  V30 9 3 9 10
      M  V30 10 1 10 11
      M  V30 11 1 11 12
      M  V30 12 1 11 13
      M  V30 13 1 12 14
      M  V30 14 1 12 13
      M  V30 15 1 13 15
      M  V30 16 1 14 16
      M  V30 17 1 14 18
      M  V30 18 1 15 17
      M  V30 19 1 15 18
      M  V30 20 1 16 17
      M  V30 END BOND
      M  V30 END CTAB
      M  END""";

  private static final String MOL_HEADER_LINES = "some title\n  mzmine\ncomment line\n";

  private static final String MOL_FORMULA = "C12H10[98]MoO5";
  private static final String MOL_SMILES = "C1CC2CC1C3C2[Mo]3(C#O)(C#O)(C#O)(C#O)C#O";
  private static final String MOL_INCHI_KEY = "PFCFPDWTNAFVLP-UHFFFAOYSA-N";

  record MolFlavor(String name, String molBlock) {

    @Override
    public String toString() {
      return name;
    }
  }

  /// all of these describe the same molecule. The format version comes from the counts line and
  /// missing header lines are padded, so every flavor has to parse to the same structure.
  final static List<MolFlavor> molFlavors = List.of( //
      new MolFlavor("V2000 without header lines", MOL_V2000) //
      , new MolFlavor("V2000 with header lines", MOL_HEADER_LINES + MOL_V2000) //
      // a title line alone is what some exports write, the counts line then sits at index 1
      , new MolFlavor("V2000 with title line only", "some title\n" + MOL_V2000) //
      , new MolFlavor("V2000 with crlf line endings", MOL_V2000.replace("\n", "\r\n")) //
      , new MolFlavor("V3000 without header lines", MOL_V3000) //
      , new MolFlavor("V3000 with header lines", MOL_HEADER_LINES + MOL_V3000) //
      , new MolFlavor("V3000 with crlf line endings", MOL_V3000.replace("\n", "\r\n")) //
  );

  @ParameterizedTest
  @FieldSource(value = "molFlavors")
  void parseMolFlavors(MolFlavor flavor) {
    final MolecularStructure structure = StructureParser.silent().parseMol(flavor.molBlock());
    Assertions.assertNotNull(structure, flavor.name());

    // the full connection table was read, not just the part before a misaligned counts line
    Assertions.assertEquals(18, structure.structure().getAtomCount(), flavor.name());
    Assertions.assertEquals(20, structure.structure().getBondCount(), flavor.name());

    Assertions.assertEquals(MOL_FORMULA, structure.formulaString(), flavor.name());
    Assertions.assertEquals(0, structure.totalFormalCharge(), flavor.name());
    Assertions.assertEquals(MOL_SMILES, structure.canonicalSmiles(), flavor.name());
    Assertions.assertEquals(MOL_SMILES, structure.isomericSmiles(), flavor.name());
    Assertions.assertEquals(MOL_INCHI_KEY, structure.inchiKey(), flavor.name());
  }

  @Test
  void parseMol() {
    final MolecularStructure structure = StructureParser.silent().parseMol(MOL_V2000);
    Assertions.assertNotNull(structure);

    Assertions.assertEquals(18, structure.structure().getAtomCount());
    Assertions.assertEquals(20, structure.structure().getBondCount());
    Assertions.assertEquals(MOL_FORMULA, structure.formulaString());
    Assertions.assertEquals(0, structure.totalFormalCharge());
    Assertions.assertEquals(MOL_SMILES, structure.canonicalSmiles());
    Assertions.assertEquals(MOL_SMILES, structure.isomericSmiles());
    Assertions.assertEquals(MOL_INCHI_KEY, structure.inchiKey());
  }

  /// the metal is kept, see HarmonizationOptions.DEFAULT with MetalPolicy.KEEP_CENTRAL_IONS, and
  /// the molfile route has to agree with the smiles route on the same molecule
  @Test
  void parseMolMatchesSmiles() {
    final MolecularStructure fromMol = StructureParser.silent().parseMol(MOL_V2000);
    Assertions.assertNotNull(fromMol);
    final MolecularStructure fromSmiles = StructureParser.silent()
        .parseStructure(fromMol.canonicalSmiles(), StructureInputType.SMILES);
    Assertions.assertNotNull(fromSmiles);

    Assertions.assertEquals(fromSmiles.formulaString(), fromMol.formulaString());
    Assertions.assertEquals(fromSmiles.inchiKey(), fromMol.inchiKey());
    Assertions.assertEquals(fromSmiles.canonicalSmiles(), fromMol.canonicalSmiles());
    Assertions.assertEquals(fromSmiles.monoIsotopicMass(), fromMol.monoIsotopicMass(), 1e-8);
  }

  @Test
  void parseMolInvalid() {
    final StructureParser parser = StructureParser.silent();
    Assertions.assertNull(parser.parseMol(null));
    Assertions.assertNull(parser.parseMol(""));
    Assertions.assertNull(parser.parseMol("   "));
    Assertions.assertNull(parser.parseMol("CCCO"), "smiles is not a molfile");
    // a molblock cut off mid atom block makes MDLV2000Reader throw a raw NullPointerException
    Assertions.assertNull(parser.parseMol(MOL_V2000.substring(0, 300)));
    Assertions.assertNull(parser.parseMol(MOL_V3000.substring(0, 300)));
  }

  /// the version tag decides which reader is used, so a body that does not match its tag is
  /// rejected instead of silently parsed as a truncated structure
  @Test
  void parseMolWrongVersionTag() {
    final StructureParser parser = StructureParser.silent();
    Assertions.assertNull(parser.parseMol(MOL_V2000.replace("V2000", "V3000")),
        "V2000 body tagged as V3000");
    Assertions.assertNull(parser.parseMol(MOL_V3000.replace("V3000", "V2000")),
        "V3000 body tagged as V2000");
  }

  /// the clean cache deduplicates on the isomeric smiles, so inputs that describe the same molecule
  /// share one instance no matter how they were written. decision: none of these inputs is a clean
  /// form of the molecule, otherwise the first cache lookup would hit before deduplication and the
  /// test would pass without it.
  @Test
  void parseStructureReusesInstanceForSameMolecule() {
    final StructureParser parser = StructureParser.silent();
    final MolecularStructure a = parser.parseStructure("OC(=O)CCCCCCCC", StructureInputType.SMILES);
    final MolecularStructure b = parser.parseStructure("C(CCCCCCCC)(=O)O",
        StructureInputType.SMILES);
    final MolecularStructure c = parser.parseStructure("CCCCCCCCC(=O)[OH]",
        StructureInputType.SMILES);
    Assertions.assertNotNull(a);

    Assertions.assertEquals("CCCCCCCCC(=O)O", a.isomericSmiles());
    Assertions.assertSame(a, b, "second writing of the same molecule");
    Assertions.assertSame(a, c, "third writing of the same molecule");
    // the isomeric smiles is a clean key, so it resolves to the very same instance as well
    Assertions.assertSame(a, parser.parseStructure(a.isomericSmiles(), StructureInputType.SMILES));
  }

  /// a different molecule must not be deduplicated onto an existing instance
  @Test
  void parseStructureKeepsInstancesForDifferentMolecules() {
    final StructureParser parser = StructureParser.silent();
    final MolecularStructure nonanoic = parser.parseStructure("OC(=O)CCCCCCCC",
        StructureInputType.SMILES);
    final MolecularStructure decanoic = parser.parseStructure("OC(=O)CCCCCCCCC",
        StructureInputType.SMILES);
    Assertions.assertNotNull(nonanoic);
    Assertions.assertNotNull(decanoic);

    Assertions.assertNotSame(nonanoic, decanoic);
    Assertions.assertNotEquals(nonanoic.isomericSmiles(), decanoic.isomericSmiles());
  }

  /// the canonical smiles drops stereo, so it is deliberately not a clean cache key. Serving it
  /// would hand a stereo free input back whichever stereoisomer was parsed before it.
  @Test
  void canonicalSmilesIsNotACleanCacheKey() {
    final StructureParser parser = StructureParser.silent();
    // parse the stereo defined molecule first so its canonical smiles could pollute the cache
    final MolecularStructure stereo = parser.parseStructure("C/C=C/CCCCCCCC",
        StructureInputType.SMILES);
    Assertions.assertNotNull(stereo);
    final String canonicalSmiles = stereo.canonicalSmiles();
    Assertions.assertNotEquals(canonicalSmiles, stereo.isomericSmiles(),
        "test needs a molecule whose canonical smiles differs from its isomeric smiles");

    // the canonical smiles as input describes the molecule without the double bond geometry
    final MolecularStructure withoutStereo = parser.parseStructure(canonicalSmiles,
        StructureInputType.SMILES);
    Assertions.assertNotNull(withoutStereo);

    Assertions.assertNotSame(stereo, withoutStereo);
    Assertions.assertNotEquals(stereo.isomericSmiles(), withoutStereo.isomericSmiles());
    Assertions.assertNotEquals(stereo.inchiKey(), withoutStereo.inchiKey());
    // without any stereo to write, both smiles flavors give the same string
    Assertions.assertEquals(canonicalSmiles, withoutStereo.isomericSmiles());

    // and the cached result equals what a parse without any cache gives
    final MolecularStructure uncached = parser.parseStructureWithoutCache(canonicalSmiles,
        StructureInputType.SMILES);
    Assertions.assertNotNull(uncached);
    Assertions.assertEquals(uncached.inchiKey(), withoutStereo.inchiKey());
    Assertions.assertEquals(uncached.isomericSmiles(), withoutStereo.isomericSmiles());
  }

}