/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Contains precomputed values in case they need to be accessed more frequently
 */
public class ComplexMolecularStructure implements MolecularStructure {

  private static final Logger logger = Logger.getLogger(ComplexMolecularStructure.class.getName());
  private @NotNull IAtomContainer structure;
  private IMolecularFormula formula;
  private String inchikey;
  private double monoIsotopicMass;
  private double mostAbundantMass;
  private int totalFormalCharge;

  public ComplexMolecularStructure(@NotNull final IAtomContainer structure,
      final IMolecularFormula formula, final String inchikey, final double monoIsotopicMass,
      final double mostAbundantMass, final int totalFormalCharge) {
    this.structure = structure;
    this.formula = formula;
    this.inchikey = inchikey;
    this.monoIsotopicMass = monoIsotopicMass;
    this.mostAbundantMass = mostAbundantMass;
    this.totalFormalCharge = totalFormalCharge;
  }

  @Override
  public @NotNull IAtomContainer structure() {
    return structure;
  }

  @Override
  public @Nullable IMolecularFormula getMolecularFormula() {
    return formula;
  }

  @Override
  public double getMonoIsotopicMass() {
    return monoIsotopicMass;
  }

  @Override
  public double getMostAbundantMass() {
    return mostAbundantMass;
  }

  @Override
  public int getTotalFormalCharge() {
    return totalFormalCharge;
  }

  @Override
  public @Nullable String getInChIKey(@NotNull final StructureParser parser) {
    return inchikey;
  }
}
