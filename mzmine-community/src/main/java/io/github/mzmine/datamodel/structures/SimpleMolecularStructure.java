/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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


import io.github.mzmine.datamodel.structures.StructureUtils.SmilesFlavor;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * All values are computed on demand. So if accessed often use {@link #precomputeValues()} to create
 * a {@link PrecomputedMolecularStructure} with direct value access.
 *
 * @param structure
 */
public record SimpleMolecularStructure(@NotNull IAtomContainer structure) implements
    MolecularStructure {

  private static final Logger logger = Logger.getLogger(SimpleMolecularStructure.class.getName());

  @NotNull
  public IMolecularFormula formula() {
    return StructureUtils.getFormula(structure());
  }

  @Override
  public String canonicalSmiles() {
    return StructureUtils.getSmiles(SmilesFlavor.CANONICAL, structure);
  }

  @Override
  public String isomericSmiles() {
    return StructureUtils.getSmiles(SmilesFlavor.ISOMERIC, structure);
  }

  @Override
  public String inchi() {
    return StructureUtils.getInchi(structure);
  }

  public double monoIsotopicMass() {
    return StructureUtils.getMonoIsotopicMass(structure());
  }

  public double mostAbundantMass() {
    return StructureUtils.getMostAbundantMass(structure());
  }

  public int totalFormalCharge() {
    return StructureUtils.getTotalFormalCharge(structure());
  }

  @Nullable
  public String inchiKey() {
    return StructureUtils.getInchiKey(structure());
  }

  /**
   * Precompute values in case they are access more often
   *
   * @return a structure with precomputed values
   */
  public PrecomputedMolecularStructure precomputeValues() {
    InchiStructure inchiStr = StructureUtils.getInchiStructure(structure);
    String inchi = inchiStr != null ? inchiStr.inchi() : null;
    String inchiKey = inchiStr != null ? inchiStr.inchiKey() : null;

    return new PrecomputedMolecularStructure(structure, formula(), canonicalSmiles(),
        isomericSmiles(), inchi, inchiKey, monoIsotopicMass(), mostAbundantMass(),
        totalFormalCharge());
  }

  @Override
  public @NotNull String toString() {
    final PrecomputedMolecularStructure val = precomputeValues();
    return "SimpleMolecularStructure[" + "formula=" + MolecularFormulaManipulator.getString(val.formula())
        + ", " + "canonicalSmiles=" + val.canonicalSmiles() + ", " + "isomericSmiles=" + val.isomericSmiles()
        + ", " + "inchi=" + val.inchi() + ", " + "inchiKey=" + val.inchiKey() + ", " + "monoIsotopicMass="
        + val.monoIsotopicMass() + ", " + "mostAbundantMass=" + val.mostAbundantMass() + ", "
        + "totalFormalCharge=" + val.totalFormalCharge() + ']';
  }
}
