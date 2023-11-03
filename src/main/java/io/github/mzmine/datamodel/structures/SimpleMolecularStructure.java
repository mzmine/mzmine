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

import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public record SimpleMolecularStructure(@NotNull IAtomContainer structure) implements
    MolecularStructure {

  @Nullable
  public IMolecularFormula getMolecularFormula() {
    AtomContainerManipulator.getMass(structure(), AtomContainerManipulator.MonoIsotopic);
    return MolecularFormulaManipulator.getMolecularFormula(structure());
  }

  public double getMonoIsotopicMass() {
    return AtomContainerManipulator.getMass(structure(), AtomContainerManipulator.MonoIsotopic);
  }

  public double getMostAbundantMass() {
    return AtomContainerManipulator.getMass(structure(), AtomContainerManipulator.MostAbundant);
  }

  public int getTotalFormalCharge() {
    return AtomContainerManipulator.getTotalFormalCharge(structure());
  }

  @Nullable
  public String getInChIKey(@NotNull StructureParser parser) {
    try {
      // Generate the InChI Key
      return parser.getInchiFactory().getInChIGenerator(structure()).getInchiKey();
    } catch (CDKException e) {
      String message = "Cannot parse 'structure' %s".formatted(structure());
      if (parser.isVerbose()) {
        logger.log(Level.WARNING, message, e);
      } else {
        logger.log(Level.WARNING, message);
      }
      return null;
    }
  }

  /**
   * Precompute values in case they are access more often
   *
   * @param parser structure parser, can be the same that was used to create this structure
   * @return a structure with precomputed values
   */
  public ComplexMolecularStructure precomputeValues(StructureParser parser) {
    return new ComplexMolecularStructure(structure, getMolecularFormula(), getInChIKey(parser),
        getMonoIsotopicMass(), getMostAbundantMass(), getTotalFormalCharge());
  }
}
