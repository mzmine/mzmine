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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

/**
 * Covers the hard cases of {@link StructureHarmonizer}: metal complexes that have to survive, salts
 * written with and without the fragment separator, protonation states, and the tautomer convergence
 * between smiles and inchi sources that the former inchi round trip used to provide.
 */
class StructureHarmonizerTest {

  private static final double MASS_TOLERANCE = 0.0001;

  /**
   * heme b, iron held by a porphyrin macrocycle
   */
  private static final String HEME = "CC1=C(CCC(O)=O)C2=CC3=C(C=C)C(C)=C4C=C5C(C)=C(C=C)C6=[N]5[Fe]5([N]34)[N]2=C1C=C1C(C)=C(CCC(O)=O)C(=C6)[N]15";
  /**
   * magnesium held by a porphyrin like macrocycle, the chlorophyll motif
   */
  private static final String MG_MACROCYCLE = "CC1=C2C=C3C(C)=C(CC)C4=[N]3[Mg]3([N]2=C1C=C1C(C)=C(CC)C(=C2)[N]13)[N]4=C2";
  private static final String PEPTIDE_SMILES = "CC(C)CC(N)C(=O)NC(CC(C)C)C(=O)NC(Cc1ccc(O)cc1)C(=O)O";
  private static final String PEPTIDE_INCHI = "InChI=1S/C21H33N3O5/c1-12(2)9-16(22)19(26)23-17(10-13(3)4)20(27)24-18(21(28)29)11-14-5-7-15(25)8-6-14/h5-8,12-13,16-18,25H,9-11,22H2,1-4H3,(H,23,26)(H,24,27)(H,28,29)";
  private static final String OLEAMIDE_INCHI = "InChI=1S/C22H43NO/c1-2-3-4-5-6-7-8-9-10-11-12-13-14-15-16-17-18-19-20-21-22(23)24/h9-10H,2-8,11-21H2,1H3,(H2,23,24)";

  private static MolecularStructure parse(String input, StructureInputType type) {
    return parse(input, type, HarmonizationOptions.DEFAULT);
  }

  private static MolecularStructure parse(String input, StructureInputType type,
      HarmonizationOptions options) {
    // without cache so each case is independent of the order the tests run in
    return StructureParser.silent().parseStructureWithoutCache(input, type, options);
  }

  private static MolecularStructure smiles(String smiles) {
    return parse(smiles, StructureInputType.SMILES);
  }

  // ------------------------------------------------------------ metals

  record MetalCase(String name, String smiles, String expectedFormula, double expectedMass) {

  }

  /**
   * Salts are frequently written without the fragment separator, so the metal has to be cut off the
   * ligand before the main fragment can be picked. Coordination complexes and organometallics must
   * stay intact.
   */
  static final List<MetalCase> metalCases = List.of(
      // the case that motivated the metal handling: sodium bound to nitrogen, no dot in the smiles
      new MetalCase("amide with bound Na", "C(N[Na])=O", "CH3NO", 45.0215),
      new MetalCase("Na acetate without dot", "CC(=O)O[Na]", "C2H4O2", 60.0211),
      new MetalCase("Ca acetate without dot", "CC(=O)O[Ca]OC(C)=O", "C2H4O2", 60.0211),
      new MetalCase("K benzoate without dot", "[K]OC(=O)c1ccccc1", "C7H6O2", 122.0368),
      new MetalCase("Li phenoxide without dot", "[Li]Oc1ccccc1", "C6H6O", 94.0419),
      // chelated central ions stay part of the molecule
      new MetalCase("heme b keeps Fe", HEME, "C34H32FeN4O4", 616.1773),
      new MetalCase("macrocycle keeps Mg", MG_MACROCYCLE, "C22H23MgN4", 367.1773));

  @ParameterizedTest(name = "{0}")
  @FieldSource("metalCases")
  void metalHandling(MetalCase testCase) {
    final MolecularStructure structure = smiles(testCase.smiles());
    Assertions.assertNotNull(structure, testCase.name());
    Assertions.assertEquals(testCase.expectedFormula(), structure.formulaString(),
        "formula mismatch for " + testCase.name());
    Assertions.assertEquals(testCase.expectedMass(), structure.monoIsotopicMass(), MASS_TOLERANCE,
        "mass mismatch for " + testCase.name());
    Assertions.assertEquals(0, structure.totalFormalCharge(),
        "charge mismatch for " + testCase.name());
  }

  @Test
  @DisplayName("a multidentate transition metal complex is kept even outside a ring")
  void keepsCisplatin() {
    final MolecularStructure structure = smiles("N[Pt](N)(Cl)Cl");
    Assertions.assertNotNull(structure);
    // all five atoms of the complex survive, unlike the standard inchi round trip which splits
    // cisplatin into [Cl-].[Cl-].[NH2-].[NH2-].[Pt+4]
    Assertions.assertEquals(5, structure.totalAtomsCount());
    Assertions.assertEquals(296.9399, structure.monoIsotopicMass(), MASS_TOLERANCE);
    Assertions.assertEquals("Cl[Pt](Cl)(N)N", structure.isomericSmiles());
    Assertions.assertEquals("Cl[Pt](Cl)(N)N", structure.canonicalSmiles());
  }

  @Test
  @DisplayName("an organometallic keeps its halide because the metal is bound to carbon")
  void keepsMethylmercury() {
    final MolecularStructure structure = smiles("C[Hg]Cl");
    Assertions.assertNotNull(structure);
    Assertions.assertEquals(3, structure.totalAtomsCount());
    Assertions.assertEquals(251.9630, structure.monoIsotopicMass(), MASS_TOLERANCE);
  }

  @Test
  @DisplayName("DISCONNECT_ALL strips even a chelated central ion")
  void disconnectAllRemovesHemeIron() {
    final HarmonizationOptions options = new HarmonizationOptions(true, MetalPolicy.DISCONNECT_ALL,
        FragmentPolicy.MAJOR_FRAGMENT, true, true);
    final MolecularStructure structure = parse(HEME, StructureInputType.SMILES, options);
    Assertions.assertNotNull(structure);
    // iron is gone and the freed nitrogen anions were protonated to the porphyrin free base
    Assertions.assertEquals("C34H33N4O4", structure.formulaString());
    Assertions.assertFalse(structure.formulaString().contains("Fe"));
  }

  @Test
  @DisplayName("KEEP_ALL leaves a salt bond in place")
  void keepAllKeepsBoundSodium() {
    final HarmonizationOptions options = new HarmonizationOptions(true, MetalPolicy.KEEP_ALL,
        FragmentPolicy.MAJOR_FRAGMENT, true, true);
    final MolecularStructure structure = parse("C(N[Na])=O", StructureInputType.SMILES, options);
    Assertions.assertNotNull(structure);
    Assertions.assertEquals("CH2NNaO", structure.formulaString());
  }

  // ------------------------------------------------------------ salts and solvates

  record SaltCase(String name, String smiles, String expectedFormula, double expectedMass) {

  }

  static final List<SaltCase> saltCases = List.of(
      new SaltCase("sodium acetate", "CC(=O)[O-].[Na+]", "C2H4O2", 60.0211),
      new SaltCase("tris dihydrochloride", "OCC(N)(CO)CO.Cl.Cl", "C4H11NO3", 121.0739),
      // the counter ion has fewer heavy atoms here, but the list also demotes larger ones
      new SaltCase("ibuprofen trifluoroacetate", "CC(C)Cc1ccc(cc1)C(C)C(O)=O.OC(=O)C(F)(F)F",
          "C13H18O2", 206.1307),
      new SaltCase("paracetamol monohydrate", "CC(=O)Nc1ccc(O)cc1.O", "C8H9NO2", 151.0633),
      new SaltCase("amine methanesulfonate", "CNC.CS(O)(=O)=O", "C2H7N", 45.0578));

  @ParameterizedTest(name = "{0}")
  @FieldSource("saltCases")
  void stripsSaltsAndSolvates(SaltCase testCase) {
    final MolecularStructure structure = smiles(testCase.smiles());
    Assertions.assertNotNull(structure, testCase.name());
    Assertions.assertEquals(testCase.expectedFormula(), structure.formulaString(),
        "formula mismatch for " + testCase.name());
    Assertions.assertEquals(testCase.expectedMass(), structure.monoIsotopicMass(), MASS_TOLERANCE,
        "mass mismatch for " + testCase.name());
  }

  @Test
  @DisplayName("a counter ion is never stripped when nothing else would remain")
  void keepsLoneCounterIon() {
    // acetic acid is on the counter ion list, but it is the only fragment so size decides again
    final MolecularStructure structure = smiles("CC(=O)O.O");
    Assertions.assertNotNull(structure);
    Assertions.assertEquals("C2H4O2", structure.formulaString());
  }

  @Test
  @DisplayName("KEEP_FRAGMENTS keeps the ion pair and keeps it charge balanced")
  void keepFragmentsKeepsIonPair() {
    final MolecularStructure structure = parse("CC(=O)[O-].[Na+]", StructureInputType.SMILES,
        HarmonizationOptions.KEEP_FRAGMENTS);
    Assertions.assertNotNull(structure);
    // the carboxylate is NOT protonated here: its charge balances the sodium cation, which cannot
    // lose a hydrogen. Protonating it would report a net +1 species.
    Assertions.assertEquals("C2H3NaO2", structure.formulaString());
    Assertions.assertEquals(0, structure.totalFormalCharge());
    Assertions.assertEquals(82.0031, structure.monoIsotopicMass(), MASS_TOLERANCE);
  }

  @Test
  @DisplayName("KEEP_FRAGMENTS keeps the ion pair and keeps it charge balanced")
  void neutralizeSalt() {
    final MolecularStructure structure = parse("CC(=O)[O-].[Na+]", StructureInputType.SMILES,
        HarmonizationOptions.DEFAULT);
    Assertions.assertNotNull(structure);
    Assertions.assertEquals("C2H4O2", structure.formulaString());
    Assertions.assertEquals(0, structure.totalFormalCharge());
    Assertions.assertEquals(60.0211, structure.monoIsotopicMass(), MASS_TOLERANCE);
  }

  // ------------------------------------------------------------ protonation

  record ChargeCase(String name, String smiles, String expectedFormula, int expectedCharge,
                    double expectedMass) {

  }

  static final List<ChargeCase> chargeCases = List.of(
      // a deprotonated acid from a library would otherwise report a neutral mass 1.008 too low
      new ChargeCase("acetate", "CC(=O)[O-]", "C2H4O2", 0, 60.0211),
      new ChargeCase("phenoxide", "[O-]c1ccccc1", "C6H6O", 0, 94.0419),
      new ChargeCase("thiolate", "CC[S-]", "C2H6S", 0, 62.0190),
      new ChargeCase("ethylammonium", "CC[NH3+]", "C2H7N", 0, 45.0578),
      new ChargeCase("tetra methyl amine stays charged", "C[N+](C)(C)(C)", "[C4H12N]+", 1, 74.0970),
      new ChargeCase("guanidinium", "NC(N)=[NH2+]", "CH5N3", 0, 59.0483),
      new ChargeCase("ATP tetraanion",
          "Nc1ncnc2c1ncn2C1OC(COP([O-])(=O)OP([O-])(=O)OP([O-])([O-])=O)C(O)C1O", "C10H16N5O13P3",
          0, 506.9957),
      // charges that cannot be moved stay, and so does the charge that balances them
      new ChargeCase("betaine stays a zwitterion", "C[N+](C)(C)CC(=O)[O-]", "C5H11NO2", 0,
          117.0790),
      new ChargeCase("choline stays a cation", "C[N+](C)(C)CCO", "[C5H14NO]+", 1, 104.1075),
      new ChargeCase("Phosphatidylcholine stays a cation", "C[N+](C)(C)CCOP(=O)(O)OCC(COC=O)OC=O",
          "[C10H21NO8P]+", 1, 314.1005),
      new ChargeCase("nitro stays charge separated", "C[N+](=O)[O-]", "CH3NO2", 0, 61.0164),
      new ChargeCase("amine oxide stays charge separated", "C[N+](C)(C)[O-]", "C3H9NO", 0, 75.0684),
      new ChargeCase("azide stays charge separated", "[N-]=[N+]=N", "HN3", 0, 43.0170),
      new ChargeCase("Cs split away", "[Cs+].[O-]C(=O)[O-].[Cs+]", "CH2O3", 0, 62.0004),
      // a carbanion cannot take a proton without changing the species
      new ChargeCase("carbanion is left alone", "[CH3-]", "[CH3]-", -1, 15.0235));

  @ParameterizedTest(name = "{0}")
  @FieldSource("chargeCases")
  void neutralizesProtonationState(ChargeCase testCase) {
    final MolecularStructure structure = smiles(testCase.smiles());
    Assertions.assertNotNull(structure, testCase.name());
    Assertions.assertEquals(testCase.expectedFormula(), structure.formulaString(),
        "formula mismatch for " + testCase.name());
    Assertions.assertEquals(testCase.expectedCharge(), structure.totalFormalCharge(),
        "charge mismatch for " + testCase.name());
    Assertions.assertEquals(testCase.expectedMass(), structure.monoIsotopicMass(), MASS_TOLERANCE,
        "mass mismatch for " + testCase.name());
  }

  @Test
  @DisplayName("KEEP_CHARGES reports the input protonation state")
  void keepChargesLeavesAnionAlone() {
    final MolecularStructure structure = parse("CC(=O)[O-]", StructureInputType.SMILES,
        HarmonizationOptions.KEEP_CHARGES);
    Assertions.assertNotNull(structure);
    Assertions.assertEquals(-1, structure.totalFormalCharge());
    Assertions.assertEquals(59.0133, structure.monoIsotopicMass(), MASS_TOLERANCE);
  }

  @Test
  @DisplayName("a zwitterion and its neutral form become the same structure")
  void glycineZwitterionMatchesNeutralForm() {
    final MolecularStructure zwitterion = smiles("[NH3+]CC(=O)[O-]");
    final MolecularStructure neutral = smiles("NCC(=O)O");
    Assertions.assertNotNull(zwitterion);
    Assertions.assertNotNull(neutral);
    Assertions.assertEquals(neutral.isomericSmiles(), zwitterion.isomericSmiles());
    Assertions.assertEquals(neutral.inchiKey(), zwitterion.inchiKey());
    Assertions.assertEquals(0, zwitterion.totalFormalCharge());
  }

  // ------------------------------------------------------------ tautomers and representations

  @Test
  @DisplayName("amide, imidic acid and inchi all converge on the amide form")
  void amideTautomersConverge() {
    final MolecularStructure fromAmide = smiles("CCCCCCCCC=CCCCCCCCCCCCC(N)=O");
    final MolecularStructure fromImidic = smiles("CCCCCCCCC=CCCCCCCCCCCCC(=N)O");
    final MolecularStructure fromInchi = parse(OLEAMIDE_INCHI, StructureInputType.INCHI);
    Assertions.assertNotNull(fromAmide);
    Assertions.assertNotNull(fromImidic);
    Assertions.assertNotNull(fromInchi);

    // the amide is what a chemist expects to see, not the imidic acid the inchi parser hands back
    Assertions.assertEquals("CCCCCCCCC=CCCCCCCCCCCCC(=O)N", fromAmide.isomericSmiles());
    Assertions.assertEquals(fromAmide.isomericSmiles(), fromImidic.isomericSmiles());
    Assertions.assertEquals(fromAmide.isomericSmiles(), fromInchi.isomericSmiles());
    Assertions.assertEquals(fromAmide.inchiKey(), fromImidic.inchiKey());
    Assertions.assertEquals(fromAmide.inchiKey(), fromInchi.inchiKey());
  }

  @Test
  @DisplayName("a peptide keeps its amide bonds when it comes from an inchi")
  void peptideFromInchiKeepsAmideBonds() {
    final MolecularStructure fromSmiles = parse(PEPTIDE_SMILES, StructureInputType.SMILES);
    final MolecularStructure fromInchi = parse(PEPTIDE_INCHI, StructureInputType.INCHI);
    Assertions.assertNotNull(fromSmiles);
    Assertions.assertNotNull(fromInchi);
    // the former inchi round trip turned every C(=O)N of this tripeptide into C(=N)O
    Assertions.assertEquals("CC(C)CC(C(=O)NC(CC(C)C)C(=O)NC(CC1=CC=C(C=C1)O)C(=O)O)N",
        fromInchi.isomericSmiles());
    Assertions.assertEquals(fromSmiles.isomericSmiles(), fromInchi.isomericSmiles());
    Assertions.assertEquals(fromSmiles.inchiKey(), fromInchi.inchiKey());
  }

  @Test
  @DisplayName("pentavalent and charge separated nitro converge")
  void nitroRepresentationsConverge() {
    final MolecularStructure pentavalent = smiles("CN(=O)=O");
    final MolecularStructure chargeSeparated = smiles("C[N+](=O)[O-]");
    Assertions.assertNotNull(pentavalent);
    Assertions.assertNotNull(chargeSeparated);
    Assertions.assertEquals("C[N+](=O)[O-]", pentavalent.isomericSmiles());
    Assertions.assertEquals(chargeSeparated.isomericSmiles(), pentavalent.isomericSmiles());
  }

  /**
   * Aromatic ring tautomers such as guanine are the known limitation: the imidic rule only matches
   * aliphatic carbon, so a guanine that comes from an inchi keeps the enol form the inchi parser
   * produced. Matching is unaffected because the standard inchi key collapses both, which is what
   * this test pins down.
   */
  @Test
  @DisplayName("aromatic ring tautomers still share an inchi key")
  void aromaticRingTautomersShareInchiKey() {
    final MolecularStructure fromSmiles = smiles("Nc1nc2[nH]cnc2c(=O)[nH]1");
    final MolecularStructure fromInchi = parse(
        "InChI=1S/C5H5N5O/c6-5-9-3-2(4(11)10-5)7-1-8-3/h1H,(H4,6,7,8,9,10,11)",
        StructureInputType.INCHI);
    Assertions.assertNotNull(fromSmiles);
    Assertions.assertNotNull(fromInchi);
    Assertions.assertEquals(fromSmiles.inchiKey(), fromInchi.inchiKey());
    Assertions.assertEquals(fromSmiles.formulaString(), fromInchi.formulaString());
    // documented gap: the depicted tautomer still differs between the two sources
    Assertions.assertNotEquals(fromSmiles.isomericSmiles(), fromInchi.isomericSmiles());
  }

  record UnchangedCase(String name, String smiles, String expectedIsomericSmiles) {

  }

  /**
   * Groups that look similar to the normalization rules but must not be rewritten.
   */
  static final List<UnchangedCase> unchangedCases = List.of(
      new UnchangedCase("hydroxamic acid", "CC(=O)NO", "CC(=O)NO"),
      new UnchangedCase("amidoxime", "CC(N)=NO", "CC(=NO)N"),
      new UnchangedCase("ketoxime", "CC(C)=NO", "CC(=NO)C"),
      new UnchangedCase("imidate ester", "CC(=N)OC", "CC(=N)OC"),
      new UnchangedCase("carbamic acid", "NC(O)=O", "C(=O)(N)O"),
      new UnchangedCase("urea", "NC(N)=O", "C(=O)(N)N"),
      new UnchangedCase("tetrazole", "c1nnn[nH]1", "C1=NN=NN1"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("unchangedCases")
  void leavesSimilarGroupsAlone(UnchangedCase testCase) {
    final MolecularStructure structure = smiles(testCase.smiles());
    Assertions.assertNotNull(structure, testCase.name());
    Assertions.assertEquals(testCase.expectedIsomericSmiles(), structure.isomericSmiles(),
        testCase.name() + " must not be rewritten");
  }

  // ------------------------------------------------------------ preserved information

  @Test
  @DisplayName("stereo chemistry survives harmonization")
  void keepsStereoChemistry() {
    final MolecularStructure lAlanine = smiles("C[C@H](N)C(O)=O");
    final MolecularStructure dAlanine = smiles("C[C@@H](N)C(O)=O");
    Assertions.assertNotNull(lAlanine);
    Assertions.assertNotNull(dAlanine);
    Assertions.assertEquals("C[C@@H](C(=O)O)N", lAlanine.isomericSmiles());
    Assertions.assertNotEquals(lAlanine.isomericSmiles(), dAlanine.isomericSmiles());
    Assertions.assertNotEquals(lAlanine.inchiKey(), dAlanine.inchiKey());
    // the canonical flavor drops stereo, so both share it
    Assertions.assertEquals(lAlanine.canonicalSmiles(), dAlanine.canonicalSmiles());
  }

  @Test
  @DisplayName("isotope labels survive harmonization")
  void keepsIsotopes() {
    final MolecularStructure labelled = smiles("[2H]C([2H])([2H])O");
    Assertions.assertNotNull(labelled);
    Assertions.assertEquals("CH[2]H3O", labelled.formulaString());
    Assertions.assertEquals(35.0450, labelled.monoIsotopicMass(), MASS_TOLERANCE);
  }

  @Test
  @DisplayName("a structure with a pseudo atom still parses from smiles")
  void keepsPseudoAtoms() {
    // inchi fails outright for these, so smiles has to carry them
    final MolecularStructure structure = smiles("CCC*");
    Assertions.assertNotNull(structure);
    Assertions.assertEquals("CCC*", structure.isomericSmiles());
    Assertions.assertNull(structure.formula(), "a pseudo atom has no formula");
  }

  /**
   * The smiles, inchi and smarts row filters allow several structures in one query, so main
   * fragment selection has to be off there, and an explicit hydrogen has to survive because it
   * narrows the query. See {@link HarmonizationOptions#QUERY}.
   */
  @Test
  @DisplayName("QUERY keeps every fragment of a multi structure query")
  void queryKeepsAllFragments() {
    final MolecularStructure query = parse("O.O.O", StructureInputType.SMILES,
        HarmonizationOptions.QUERY);
    Assertions.assertNotNull(query);
    Assertions.assertEquals(3, query.totalAtomsCount(), "a query asking for three oxygens");
    Assertions.assertEquals("O.O.O", query.isomericSmiles());

    // the default would reduce the same input to a single oxygen
    final MolecularStructure asAnalyte = smiles("O.O.O");
    Assertions.assertNotNull(asAnalyte);
    Assertions.assertEquals(1, asAnalyte.totalAtomsCount());
  }

  @Test
  @DisplayName("QUERY keeps explicit hydrogens so OH stays narrower than O")
  void queryKeepsExplicitHydrogens() {
    final MolecularStructure hydroxyl = parse("OH.OH.OH", StructureInputType.SMILES,
        HarmonizationOptions.QUERY);
    final MolecularStructure anyOxygen = parse("O.O.O", StructureInputType.SMILES,
        HarmonizationOptions.QUERY);
    Assertions.assertNotNull(hydroxyl);
    Assertions.assertNotNull(anyOxygen);
    // three oxygens plus their explicit hydrogens against three bare oxygens
    Assertions.assertEquals(6, hydroxyl.totalAtomsCount());
    Assertions.assertEquals(3, anyOxygen.totalAtomsCount());
  }

  @Test
  @DisplayName("NONE leaves the structure as written")
  void noneChangesNothing() {
    final MolecularStructure structure = parse("CC(=O)[O-].[Na+]", StructureInputType.SMILES,
        HarmonizationOptions.NONE);
    Assertions.assertNotNull(structure);
    Assertions.assertEquals("CC(=O)[O-].[Na+]", structure.isomericSmiles());
    Assertions.assertTrue(HarmonizationOptions.NONE.isNoOp());
    Assertions.assertFalse(HarmonizationOptions.DEFAULT.isNoOp());
  }
}
