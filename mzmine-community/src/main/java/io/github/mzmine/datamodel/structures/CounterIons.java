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

import io.github.mzmine.datamodel.structures.StructureUtils.SmilesFlavor;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;

/**
 * Recognizes fragments that are counter ions or solvates rather than the analyte. This is only used
 * to demote such fragments in {@link FragmentPolicy#MAJOR_FRAGMENT}, never to remove them outright,
 * so a wrong classification can at worst pick the second largest fragment of a mixture.
 */
public final class CounterIons {

  /**
   * Single atom counter ions by atomic number: Li, Na, K, Rb, Cs, Be, Mg, Ca, Sr, Ba, Al, and the
   * halides F, Cl, Br, I. Only ever consulted for fragments of exactly one heavy atom, so a
   * chelated magnesium or a covalently bound chlorine is never affected.
   */
  private static final Set<Integer> SINGLE_ATOM_IONS = Set.of(3, 11, 19, 37, 55, 4, 12, 20, 38, 56,
      13, 9, 17, 35, 53);

  /**
   * Multi atom counter ions and solvates, keyed by canonical SMILES as produced by
   * {@link SmilesFlavor#CANONICAL}. Solvents that are plausible analytes on their own (benzene,
   * toluene, hexane) are deliberately absent.
   */
  private static final Set<String> CANONICAL_SMILES = Set.of( //
      "O", // water
      "Cl", "Br", "I", "F", // hydrogen halides
      "[OH-]", "[NH4+]", "N", // hydroxide, ammonium, ammonia
      "CO", "CCO", "CC(C)O", // methanol, ethanol, isopropanol
      "CS(C)=O", "CC#N", "CC(C)=O", "CCOC(C)=O", // DMSO, acetonitrile, acetone, ethyl acetate
      "C1CCOC1", "C1COCCO1", // THF, dioxane
      "OC=O", "CC(O)=O", // formic acid, acetic acid
      "OC(=O)C(F)(F)F", "OC(=O)C(Cl)(Cl)Cl", // TFA, TCA
      "OS(O)(=O)=O", "OP(O)(O)=O", "OC(O)=O", // sulfuric, phosphoric, carbonic
      "O[N+](=O)[O-]", // nitric acid
      "CS(O)(=O)=O", "CC1=CC=C(C=C1)S(O)(=O)=O", // methanesulfonic, tosylic
      "OC(=O)C(O)=O", "OC(=O)CC(O)=O", "OC(=O)CCC(O)=O", // oxalic, malonic, succinic
      "OC(=O)C=CC(O)=O", // maleic / fumaric
      "OC(=O)C(O)C(O)C(O)=O", // tartaric
      "OC(=O)CC(O)(CC(O)=O)C(O)=O" // citric
  );

  private CounterIons() {
  }

  /**
   * @param fragment        a single connected component
   * @param canonicalSmiles canonical SMILES of the fragment, or null if it could not be generated
   * @return true if the fragment looks like a counter ion or solvate
   */
  public static boolean isCounterIon(@NotNull IAtomContainer fragment,
      @Nullable String canonicalSmiles) {
    if (heavyAtomCount(fragment) == 1) {
      for (final IAtom atom : fragment.atoms()) {
        if (atom.getAtomicNumber() != null && SINGLE_ATOM_IONS.contains(atom.getAtomicNumber())) {
          return true;
        }
      }
    }
    return canonicalSmiles != null && CANONICAL_SMILES.contains(canonicalSmiles);
  }

  /**
   * @return number of non hydrogen atoms
   */
  public static int heavyAtomCount(@NotNull IAtomContainer container) {
    int count = 0;
    for (final IAtom atom : container.atoms()) {
      final Integer atomicNumber = atom.getAtomicNumber();
      if (atomicNumber == null || atomicNumber != 1) {
        count++;
      }
    }
    return count;
  }
}
