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

import io.github.dan2097.jnainchi.InchiKeyOutput;
import io.github.dan2097.jnainchi.InchiKeyStatus;
import io.github.dan2097.jnainchi.InchiStatus;
import io.github.dan2097.jnainchi.JnaInchi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class StructureUtils {

  public enum SmilesFlavor {
    CANONICAL, ISOMERIC
  }


  /**
   * Canonical + isomeric + stereo chemistry
   */
  public static final SmilesGenerator isomericSmiGen = SmilesGenerator.absolute();
  /**
   * canonical smiles
   */
  public static final SmilesGenerator canonSmiGen = SmilesGenerator.unique();

  /**
   * Structure parsing
   *
   * @return silent
   */
  public static StructureParser getDefaultParser() {
    return StructureParser.silent();
  }

  public static SmilesGenerator getSmilesGen(SmilesFlavor flavor) {
    return switch (flavor) {
      case CANONICAL -> canonSmiGen;
      case ISOMERIC -> isomericSmiGen;
    };
  }

  @Nullable
  public static String getSmilesOrThrow(SmilesFlavor flavor, IAtomContainer structure)
      throws CDKException {
    return getSmilesGen(flavor).create(structure);
  }

  @Nullable
  public static String getSmiles(SmilesFlavor flavor, IAtomContainer structure) {
    try {
      return getSmilesOrThrow(flavor, structure);
    } catch (CDKException e) {
      return null;
    }
  }

  @Nullable
  public static InChIGenerator getInchiGenerator(IAtomContainer structure) {
    try {
      return getInchiGeneratorOrThrow(structure);
    } catch (CDKException e) {
      return null;
    }
  }

  @Nullable
  public static InChIGenerator getInchiGeneratorOrThrow(IAtomContainer structure)
      throws CDKException {
    return getDefaultParser().getInchiFactory().getInChIGenerator(structure);
  }

  /**
   * Typically better to parse a structure from inchi by {@link StructureParser} and then use
   * {@link #getInchiStructure(IAtomContainer)} to get inchi and inchikey in one call.
   *
   * @return inchi key
   */
  @Nullable
  public static String getInchiKey(String inchi) {
    InchiKeyOutput inchiKey = JnaInchi.inchiToInchiKey(inchi);
    if (inchiKey.getStatus() == InchiKeyStatus.OK) {
      return inchiKey.getInchiKey();
    } else {
      return null;
    }
  }

  /**
   * Computes inchi and inchi key in one call
   *
   * @return InchiStructure or null on error
   */
  @Nullable
  public static InchiStructure getInchiStructure(IAtomContainer structure) {
    try {
      var generator = getInchiGeneratorOrThrow(structure);
      if (generator != null && generator.getStatus() != InchiStatus.ERROR) {
        return new InchiStructure(generator.getInchi(), generator.getInchiKey());
      }
    } catch (CDKException e) {
    }
    return null;
  }

  /**
   * Computes inchi. Prefer {@link #getInchiStructure(IAtomContainer)} if both inchi and inchi key
   * are required
   *
   * @return string or null on error
   */
  @Nullable
  public static String getInchi(IAtomContainer structure) {
    try {
      var generator = getInchiGeneratorOrThrow(structure);
      if (generator != null && generator.getStatus() != InchiStatus.ERROR) {
        return generator.getInchi();
      }
    } catch (CDKException e) {
    }
    return null;
  }

  /**
   * Computes inchi key. Prefer {@link #getInchiStructure(IAtomContainer)} if both inchi and inchi
   * key are required
   *
   * @return string or null on error
   */
  @Nullable
  public static String getInchiKey(IAtomContainer structure) {
    try {
      var generator = getInchiGeneratorOrThrow(structure);
      if (generator != null && generator.getStatus() != InchiStatus.ERROR) {
        return generator.getInchiKey();
      }
    } catch (CDKException e) {
    }
    return null;
  }

  @NotNull
  public static IMolecularFormula getFormula(@NotNull IAtomContainer structure) {
    return MolecularFormulaManipulator.getMolecularFormula(structure);
  }

  public static double getMonoIsotopicMass(IAtomContainer structure) {
    return AtomContainerManipulator.getMass(structure, AtomContainerManipulator.MonoIsotopic);
  }

  public static double getMostAbundantMass(IAtomContainer structure) {
    return AtomContainerManipulator.getMass(structure, AtomContainerManipulator.MostAbundant);
  }

  public static int getTotalFormalCharge(IAtomContainer structure) {
    return AtomContainerManipulator.getTotalFormalCharge(structure);
  }

}
