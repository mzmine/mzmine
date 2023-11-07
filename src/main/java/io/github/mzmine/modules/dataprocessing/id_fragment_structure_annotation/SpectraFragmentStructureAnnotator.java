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

package io.github.mzmine.modules.dataprocessing.id_fragment_structure_annotation;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.structures.FragmentedStructure;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.DoubleProperty;
import org.jetbrains.annotations.Nullable;

public class SpectraFragmentStructureAnnotator {

  private MZTolerance mzTol;

  public SpectraFragmentStructureAnnotator(final MZTolerance mzTol) {
    this.mzTol = mzTol;
  }

  /**
   * @param spectrum
   * @param inchikey
   * @return map of signal index and fragment structure
   */
  public Map<Integer, MolecularStructure> annotateSpectralSignals(
      final Map<String, FragmentedStructure> fragments, MassSpectrum spectrum, String inchikey) {
    double[] mzs = spectrum.getMzValues(new double[0]);
    return annotateSpectralSignals(fragments, mzs, inchikey);
  }

  /**
   * @param fragments
   * @param mzs
   * @param inchikey
   * @return map of signal index and fragment structure
   */
  public Map<Integer, MolecularStructure> annotateSpectralSignals(
      final Map<String, FragmentedStructure> fragments, double[] mzs, String inchikey) {
    return annotateSpectralSignals(fragments, mzs, inchikey, null);
  }

  public Map<Integer, MolecularStructure> annotateSpectralSignals(
      final Map<String, FragmentedStructure> fragments, double[] mzs, String inchikey,
      @Nullable DoubleProperty progress) {
    FragmentedStructure structure = fragments.get(inchikey);
    if (structure == null || structure.fragments().isEmpty()) {
      return Map.of();
    }

    Map<Integer, MolecularStructure> map = new HashMap<>();

    int fragIndex = 0;
    List<MolecularStructure> frags = structure.fragments();
    MolecularStructure fragment = frags.get(fragIndex);
    double fragMz = fragment.getMonoIsotopicMass();

    for (int i = 0; i < mzs.length; i++) {
      while (true) {
        double specMz = mzs[i];
        if (mzTol.checkWithinTolerance(specMz, fragMz)) {
          map.put(i, fragment);
          break;
        } else if (specMz > fragMz) {
          fragIndex++;
          if (fragIndex >= frags.size()) {
            break;
          }
          fragment = frags.get(fragIndex);
          fragMz = fragment.getMonoIsotopicMass();
        } else {
          break;
        }
      }
      if (fragIndex >= frags.size()) {
        break;
      }
      if (progress != null) {
        progress.set((double) i / mzs.length);
      }
    }
    return map;
  }
}
