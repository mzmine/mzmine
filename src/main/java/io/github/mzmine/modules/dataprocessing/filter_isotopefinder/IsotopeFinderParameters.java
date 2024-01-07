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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import java.util.Map;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

public class IsotopeFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final ElementsParameter elements = new ElementsParameter("Chemical elements",
      "Chemical elements which isotopes will be considered");
  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN, 0.0005, 10);
  public static final IntegerParameter maxCharge = new IntegerParameter(
      "Maximum charge of isotope m/z",
      "Maximum possible charge of isotope distribution m/z's. All present m/z values obtained by dividing "
          + "isotope masses with 1, 2, ..., maxCharge values will be considered. The default value is 1, "
          + "but insert an integer greater than 1 if you want to consider ions of higher charge states.",
      1, true, 1, 1000);

  public static final ComboParameter<ScanRange> scanRange = new ComboParameter<>("Search in scans",
      " Options to search isotopes in the single most intense scan"
          + " or within all scans in full-width at half maximum range.", ScanRange.values(),
      ScanRange.SINGLE_MOST_INTENSE);

  public IsotopeFinderParameters() {
    super(new UserParameter[]{featureLists, elements, isotopeMzTolerance, maxCharge, scanRange},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_isotope_finder/isotope_finder.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    @Language("HTML") String message = """
        <p><strong>Important:</strong> The isotope finder will search for all possible isotope signals in the mass lists for each
          feature. The resulting pattern may contain signals from different charge states as this module tries
          to capture all available information, whereas the isotope grouper acts as a feature filter.</p>
                """;
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }


  public enum ScanRange {
    // IN_FWHM,
    SINGLE_MOST_INTENSE;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", isotopeMzTolerance);
    return nameParameterMap;
  }
}
