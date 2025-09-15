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

import io.github.dan2097.jnainchi.InchiKeyOutput;
import io.github.dan2097.jnainchi.InchiKeyStatus;
import io.github.dan2097.jnainchi.InchiStatus;
import io.github.dan2097.jnainchi.JnaInchi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.Aromaticity.Model;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Stereo;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IStereoElement;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class StructureUtils {

  private static final Logger logger = Logger.getLogger(StructureUtils.class.getName());

  public enum SmilesFlavor {
    CANONICAL, ISOMERIC
  }

  public enum HydrogenFlavor {
    /**
     * convenience to keep previous
     */
    UNCHANGED,
    /**
     * Suppresses all hydrogens. Do not do this on target structures in substructure search
     */
    SUPRESS_HYDROGENS,
    /**
     * Removes only non chiral
     */
    REMOVE_NON_CHIRAL_HYDROGENS,
    /**
     * Convert implicit (not shown) to explicit hydrogens. This can help in substructure search:
     * Query structure will ask for x hydrogens and target structure needs explicit hydrogens
     */
    CONVERT_IMPLICIT_TO_EXPLICIT;
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
    // otherwise structure CC(OH) will contain H in smiles
    structure = AtomContainerManipulator.copyAndSuppressedHydrogens(structure);
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
    return getInchiKey(structure, false);
  }

  @Nullable
  public static String getInchiKey(IAtomContainer structure, boolean removeStereoChemistry) {
    try {
      if (removeStereoChemistry) {
        structure = removeStereoChemistry(structure, false);
      }
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

  /**
   * @return true if there are stereo elements
   */
  public static boolean hasStereoChemistry(@NotNull IAtomContainer structure) {
    final Iterator<IStereoElement> iterator = structure.stereoElements().iterator();
    if (iterator.hasNext()) {
      return true;
    }

    for (IBond bond : structure.bonds()) {
      if (Stereo.NONE != bond.getStereo()) {
        return true;
      }
    }
    return false;
  }

  /**
   * KEEPS stereochemistry, applies hydrogen flavor that may be important for visualization and for
   * substructure matching
   *
   * @param inPlace change input mol in place
   * @return may return input mol or a clone
   */
  public static IAtomContainer harmonize(IAtomContainer mol, @NotNull HydrogenFlavor flavor,
      boolean removeStereoChemistry, boolean inPlace) {
    // 1. Clone
    if (!inPlace) {
      mol = cloneStructure(mol);
    }

    if (removeStereoChemistry) {
      // already copied so work in place
      mol = removeStereoChemistry(mol, true);
    }

    // handle hydrogens
    mol = applyHydrogenFlavor(mol, flavor, true);

    // aromaticity
    Cycles.markRingAtomsAndBonds(mol);
    Aromaticity.apply(Model.Daylight, mol);
    return mol;
  }

  /**
   * Remove stereochemistry
   *
   * @param inPlace change input mol in place
   * @return may return input mol or a clone
   */
  public static IAtomContainer removeStereoChemistry(IAtomContainer mol, boolean inPlace) {
    // 1. Clone
    if (!inPlace) {
      mol = cloneStructure(mol);
    }

    // 2. Remove all StereoElement objects
    mol.setStereoElements(new ArrayList<>());

    // 3. Reset bond stereochemistry
    for (IBond bond : mol.bonds()) {
      bond.setStereo(Stereo.NONE);
    }

    return mol;
  }

  /**
   * Applies hydrogen flavor that may be important for visualization and for substructure matching
   *
   * @param inPlace change input mol in place
   * @return may return input mol or a clone
   */
  public static IAtomContainer applyHydrogenFlavor(IAtomContainer mol, HydrogenFlavor flavor,
      boolean inPlace) {
    // 1. Clone
    if (!inPlace) {
      mol = cloneStructure(mol);
    }
    return switch (flavor) {
      case UNCHANGED -> mol;
      case SUPRESS_HYDROGENS -> AtomContainerManipulator.suppressHydrogens(mol);
      case REMOVE_NON_CHIRAL_HYDROGENS -> AtomContainerManipulator.removeNonChiralHydrogens(mol);
      case CONVERT_IMPLICIT_TO_EXPLICIT -> {
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(mol);
        yield mol;
      }
    };
  }


  private static IAtomContainer cloneStructure(IAtomContainer mol) {
    try {
      mol = mol.clone();
    } catch (CloneNotSupportedException e) {
      logger.log(Level.WARNING, "Failed to clone structure. " + e.getMessage(), e);
      throw new IllegalStateException("Failed to clone structure", e);
    }
    return mol;
  }

  public static boolean equalsSmiles(IAtomContainer structure1, IAtomContainer structure2,
      SmilesFlavor smilesFlavor) {
    final String a = getSmiles(smilesFlavor, structure1);
    final String b = getSmiles(smilesFlavor, structure2);
    return Objects.equals(a, b);
  }

  public static boolean equalsInchiKey(IAtomContainer structure1, IAtomContainer structure2) {
    return equalsInchiKey(structure1, structure2, false);
  }

  public static boolean equalsInchiKey(IAtomContainer structure1, IAtomContainer structure2,
      boolean removeStereoChemistry) {
    final String a = getInchiKey(structure1, removeStereoChemistry);
    final String b = getInchiKey(structure2, removeStereoChemistry);
    return Objects.equals(a, b);
  }

}
