/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_spectral_library_match;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import java.util.Collection;

public class AdvancedSpectralLibrarySearchParameters extends SimpleParameterSet {

  public static final OptionalParameter<PercentParameter> ccsTolerance = new OptionalParameter<>(
      new PercentParameter("CCS tolerance [%]",
          "CCS tolerance for spectral library entries to be matched against a feature.\n"
              + "If the row or the library entry does not have a CCS value, no spectrum will be matched.",
          0.05), false);

  public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
      new RTToleranceParameter());

  public static final OptionalModuleParameter<MassListDeisotoperParameters> deisotoping = new OptionalModuleParameter<>(
      "13C deisotoping",
      "Removes 13C isotope signals from the query and library spectrum before matching",
      new MassListDeisotoperParameters(), false);

  public static final BooleanParameter cropSpectraToOverlap = new BooleanParameter(
      "Crop spectra to m/z overlap",
      "Crop query and library spectra to overlapping m/z range (+- spectra m/z tolerance). This is helptful if spectra were acquired with different fragmentation energies / methods.",
      false);

  public static final OptionalParameter<IntegerParameter> needsIsotopePattern = new OptionalParameter<>(
      new IntegerParameter("Min matched isotope signals",
          "Useful for scans and libraries with isotope pattern. Minimum matched signals of 13C isotopes, distance of H and 2H or Cl isotopes. Can not be applied with deisotoping",
          3, 0, 1000), false);

  public AdvancedSpectralLibrarySearchParameters() {
    super(rtTolerance, ccsTolerance, deisotoping, needsIsotopePattern, cropSpectraToOverlap);
  }


  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    boolean check = super.checkParameterValues(errorMessages);
    // not both isotope and deisotope
    boolean isotope = !getValue(deisotoping) || !getValue(needsIsotopePattern);
    if (!isotope) {
      errorMessages.add(
          "Choose only one of \"deisotoping\" and \"need isotope pattern\" at the same time");
      return false;
    }
    return check;
  }

}
