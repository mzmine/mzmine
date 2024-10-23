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


import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public sealed interface MolecularStructure permits ComplexMolecularStructure,
    SimpleMolecularStructure {

  enum ValueType {
    PRECOMPUTED_VALUES, COMPUTE_ON_DEMAND
  }

  Logger logger = Logger.getLogger(MolecularStructure.class.getName());

  /**
   * Either precompute all values like smiles, inchi, inchikey, monoisotopic mass, formula. Or save
   * memory and use structure that computes these values on demand
   */
  @NotNull
  static MolecularStructure create(ValueType type, IAtomContainer structure) {
    SimpleMolecularStructure simple = new SimpleMolecularStructure(structure);
    return switch (type) {
      case PRECOMPUTED_VALUES -> simple.precomputeValues();
      case COMPUTE_ON_DEMAND -> simple;
    };
  }

  @NotNull
  IAtomContainer structure();

  @Nullable
  IMolecularFormula formula();

  @Nullable
  default String formulaString() {
    var f = formula();
    if (f == null) {
      return null;
    }
    return MolecularFormulaManipulator.getString(f);
  }

  String canonicalSmiles();

  String isomericSmiles();

  String inchi();

  String inchiKey();

  double monoIsotopicMass();

  double mostAbundantMass();

  int totalFormalCharge();

}