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


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * Contains precomputed values in case they need to be accessed more frequently
 */
public record PrecomputedMolecularStructure(@NotNull IAtomContainer structure,
                                            @NotNull IMolecularFormula formula,
                                            @Nullable String canonicalSmiles,
                                            @Nullable String isomericSmiles, @Nullable String inchi,
                                            @Nullable String inchiKey, double monoIsotopicMass,
                                            double mostAbundantMass,
                                            int totalFormalCharge) implements MolecularStructure {

  @Override
  public @NotNull String toString() {
    return "PrecomputedMolecularStructure[" + "formula=" + MolecularFormulaManipulator.getString(formula())
        + ", " + "canonicalSmiles=" + canonicalSmiles + ", " + "isomericSmiles=" + isomericSmiles
        + ", " + "inchi=" + inchi + ", " + "inchiKey=" + inchiKey + ", " + "monoIsotopicMass="
        + monoIsotopicMass + ", " + "mostAbundantMass=" + mostAbundantMass + ", "
        + "totalFormalCharge=" + totalFormalCharge + ']';
  }
}
