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

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains predicted substructures
 *
 * @param structure parent structure (most likely neutral)
 * @param fragments ionic fragment structures
 */
public record FragmentedStructure(@NotNull MolecularStructure structure,
                                  List<MolecularStructure> fragments) {


  /**
   * Uses binarySearch to find the closest fragment with matching mono isotopic mass
   *
   * @param mz    to search
   * @param mzTol the tolerance
   * @return the closest fragment if within tolerance or null
   */
  @Nullable
  public MolecularStructure findFragmentAtMz(final double mz, final MZTolerance mzTol) {
    int index = binarySearch(mz);
    MolecularStructure strc = fragments.get(index);
    return mzTol.checkWithinTolerance(mz, strc.getMonoIsotopicMass()) ? strc : null;
  }

  /**
   * Uses binarySearch to find the closest fragment with matching mono isotopic mass
   *
   * @param mz    to search
   * @param mzTol the tolerance
   * @return all fragments within tolerance or empty list
   */
  @NotNull
  public List<MolecularStructure> findAllFragmentAtMz(final double mz, final MZTolerance mzTol) {
    int index = binarySearch(mz);
    MolecularStructure strc = fragments.get(index);
    if (!mzTol.checkWithinTolerance(mz, strc.getMonoIsotopicMass())) {
      return List.of();
    }
    List<MolecularStructure> list = new ArrayList<>();
    list.add(strc);

    for (int i = index - 1; i >= 0; i--) {
      strc = fragments.get(i);
      if (mzTol.checkWithinTolerance(mz, strc.getMonoIsotopicMass())) {
        list.add(strc);
      }
    }
    for (int i = index + 1; i < fragments.size(); i++) {
      strc = fragments.get(i);
      if (mzTol.checkWithinTolerance(mz, strc.getMonoIsotopicMass())) {
        list.add(strc);
      }
    }
    return list;
  }

  /**
   * Searches for the value - or the closest available. Copied from
   * {@link Arrays#binarySearch(double[], double)}
   *
   * @param mz search for this value
   * @return closest index
   */
  public int binarySearch(final double mz) {
    return BinarySearch.binarySearch(mz, true, fragments.size(),
        index -> fragments.get(index).getMonoIsotopicMass());
  }
}
