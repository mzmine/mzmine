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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.text.DecimalFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.autocarbon.AutoCarbonParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePeakScannerParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final BooleanParameter bestScores = new BooleanParameter("Keep best scores only",
      "If several isotope distributions were searched for: Shows only the matches with "
          + "the best score for the searched patterns within the same retention range and mass range of the isotope pattern.",
      true);

  public static final BooleanParameter onlyMonoisotopic = new BooleanParameter(
      "Annotate monoisotopic signals only",
      "Show only the match with the best result for an isotopic pattern (major isotopes).", true);

  public static final BooleanParameter resolvedByMobility = new BooleanParameter(
      "Use mobility-resolved spectra",
      "Use of time- and mobility-resolved mass spectra instead of only time-resolved spectra.",
      true);

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final MobilityToleranceParameter mobTolerance = new MobilityToleranceParameter();

  public static final StringParameter formula = new StringParameter("Chemical formula",
      "Element (combinations) whose isotope pattern to be searched for. "
          + "Separate individual Elements (combinations) by \",\". Please enter the two letter Symbol."
          + " (e.g. \"Gd\", \"Cl2BrS\")", "");

  public static final DoubleParameter minHeight = new DoubleParameter("Minimum height",
      "Minimum peak height to be considered as an isotope peak.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  public static final DoubleParameter mergeWidth = new DoubleParameter("Merge width(m/z)",
      "This will be used to merge peaks in the calculated isotope pattern if they overlap in the spectrum."
          + " Specify in m/z, this depends on the resolution of your mass spectrometer.",
      MZmineCore.getConfiguration().getMZFormat(), 0.0005, 0.0d, 10.0);

  public static final DoubleParameter minPatternIntensity = new DoubleParameter(
      "Min. pattern intensity",
      "The minimum normalized intensity of a peak in the final calculated isotope pattern. "
          + "Depends on the sensitivity of your MS.\nMin = 0.0, Max = 0.99999",
      new DecimalFormat("0.####"), 0.01, 0.0d, 0.99999);

  public static final DoubleParameter minIsotopePatternScore = new DoubleParameter(
      "Min. score value",
      "The minimum similarity score value of the detected isotope pattern compared to the calculated isotope pattern. ",
      new DecimalFormat("0.####"), 0.7, 0.0d, 0.99999);

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name. If \"auto\" then this module will create a suffix.",
      "auto");

  public static final IntegerParameter charge = new IntegerParameter("Maximum charge",
      "Amount and polarity of the maximum charge (e.g.: [M]+=+1 / [M]-=-1", 1, true);


  public IsotopePeakScannerParameters() {
    super(new Parameter[]{PEAK_LISTS, mzTolerance, onlyMonoisotopic, bestScores, resolvedByMobility,
            rtTolerance, mobTolerance, formula, charge, minPatternIntensity, minIsotopePatternScore,
            mergeWidth, minHeight, suffix},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_isotope_peak_scanner/isotope_peak_scanner.html");
  }

//  @Override
//  public ExitCode showSetupDialog(boolean valueCheckRequired) {
//    if ((getParameters() == null) || (getParameters().length == 0)) {
//      return ExitCode.OK;
//    }
//
//    IsotopePeakScannerSetupDialog dialog = new IsotopePeakScannerSetupDialog(valueCheckRequired,
//        this);
//    dialog.showAndWait();
//    return dialog.getExitCode();
//  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);
    if (getValue(charge) == 0) {
      errorMessages.add("Charge may not be 0.");
      return false;
    }

    return superCheck;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put("Use time and mobility resolved mass spectra", getParameter(resolvedByMobility));
    map.put("Only the best Scores", getParameter(bestScores));
    map.put("Only the monoisotopic signals", getParameter(onlyMonoisotopic));

    return map;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 ->
          "The Isotope peak scanner module was completely reworked to support IMS dimension, identify the monoisotopic signals and use a new scoring algorithm.";
      default -> null;
    };
  }
}
