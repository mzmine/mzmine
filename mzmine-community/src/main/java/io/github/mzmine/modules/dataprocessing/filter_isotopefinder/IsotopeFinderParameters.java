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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import java.util.Map;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top-level isotope finder parameters: the feature lists plus the algorithm choice. The whole setup
 * of a detection run lives in the algorithm's embedded parameters, see
 * {@link IsotopeFinderModeOptions} and {@link CarbonAveragineAlgorithmParameters}.
 */
public class IsotopeFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final ModuleOptionsEnumComboParameter<IsotopeFinderModeOptions> mode = new ModuleOptionsEnumComboParameter<>(
      "Algorithm",
      "Automatic only asks for m/z tolerance, the 13C requirement, and the maximum charge and uses "
          + "sensible defaults for the rest. Carbon-averagine exposes the full setup of the same "
          + "algorithm.", IsotopeFinderModeOptions.AUTOMATIC);

  // legacy parameters: they used to live on this top level and moved into the algorithm parameters.
  // They are not part of this parameter set anymore and only exist to read the value of an older
  // batch/config XML, see getNameParameterMap() and handleLoadedParameters(). Private on purpose -
  // nothing but the legacy loading may read them.
  private static final MZToleranceParameter legacyIsotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN, 0.0005, 10);

  private static final IntegerParameter legacyMaxCharge = new IntegerParameter(
      "Maximum charge of isotope m/z", "Legacy parameter, moved into the algorithm parameters.",
      CarbonAveragineAlgorithmParameters.DEFAULT_MAX_CHARGE, true, 1, 1000);

  public IsotopeFinderParameters() {
    super(new UserParameter[]{featureLists, mode},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_isotope_finder/isotope_finder.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    Region message = FxTextFlows.newTextFlowInAccordion("Important note", true, FxTexts.text("""
        The isotope finder searches for all plausible isotope signals around each feature m/z. It
        selects the most probable charge state, flags other highly probable charges (e.g. overlapping
        [M+H]+ and [2M+2H]2+), and bounds the pattern with rough relative-intensity estimates. The
        resulting pattern is intentionally inclusive so that downstream formula prediction can refine
        it further.
        """));
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(final int version) {
    return switch (version) {
      // only mention the major change - the detection itself is different, not just the parameters
      case 2 -> """
          The isotope finder was reworked: the detection algorithm now searches all plausible isotope \
          signals around the feature m/z, selects the most probable charge state, and bounds the \
          pattern with modelled relative intensities. Results therefore differ from earlier versions \
          and are generally more complete and more reliable.
          The parameters were restructured into algorithm options, the new default being "Automatic". \
          Your m/z tolerance and maximum charge were carried over; all other settings use the new \
          defaults. Note that the default maximum charge is now 3 instead of 1.""";
      default -> null;
    };
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var nameParameterMap = super.getNameParameterMap();
    // parameters that moved into the algorithm parameters, see handleLoadedParameters
    nameParameterMap.put(legacyIsotopeMzTolerance.getName(), legacyIsotopeMzTolerance);
    // even older name of the same tolerance parameter
    nameParameterMap.put("m/z tolerance", legacyIsotopeMzTolerance);
    nameParameterMap.put(legacyMaxCharge.getName(), legacyMaxCharge);
    return nameParameterMap;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);

    final boolean hasTolerance = loadedParams.containsKey(legacyIsotopeMzTolerance.getName());
    final boolean hasMaxCharge = loadedParams.containsKey(legacyMaxCharge.getName());
    if (!hasTolerance && !hasMaxCharge) {
      return;
    }

    // decision: a batch that still carries these on the top level predates the algorithm options, so
    // it only ever ran the carbon-averagine algorithm with defaults. Map it to the automatic option,
    // which is that algorithm with defaults plus exactly these two values.
    final ParameterSet automatic = getParameter(mode).setOptionGetParameters(
        IsotopeFinderModeOptions.AUTOMATIC);
    if (hasTolerance) {
      automatic.setParameter(AutomaticIsotopeFinderParameters.isotopeMzTolerance,
          legacyIsotopeMzTolerance.getValue());
    }
    if (hasMaxCharge) {
      automatic.setParameter(AutomaticIsotopeFinderParameters.maxCharge, legacyMaxCharge.getValue());
    }
  }
}
