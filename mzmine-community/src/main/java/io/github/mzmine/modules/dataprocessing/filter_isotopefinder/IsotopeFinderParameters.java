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
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.util.ExitCode;
import java.util.Map;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;

public class IsotopeFinderParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final ElementsParameter elements = new ElementsParameter("Chemical elements",
      "Chemical elements whose major stable isotopes are considered. Heavy isotopes (e.g. S, Cl, Br) "
          + "also widen the expected intensity bounds in the signal-based mode.");

  public static final MZToleranceParameter isotopeMzTolerance = new MZToleranceParameter(
      ToleranceType.FEATURE_TO_SCAN, 0.0005, 10);

  public static final IntegerParameter maxCharge = new IntegerParameter(
      "Maximum charge of isotope m/z",
      "Maximum possible charge of the isotope distribution. Charges 1..maxCharge are evaluated and "
          + "the most probable charge is selected; other highly probable charges are flagged.", 1,
      true, 1, 1000);

  public static final OptionalModuleParameter<FwhmRefineParameters> fwhmRefine = new OptionalModuleParameter<>(
      "Refine across FWHM scans",
      "Detect on the most intense scan, then refine relative intensities and recover fine structure "
          + "across the scans within the feature FWHM (instead of pre-merging the scans).",
      new FwhmRefineParameters(), false);

  public static final ModuleOptionsEnumComboParameter<IsotopeFinderModeOptions> mode = new ModuleOptionsEnumComboParameter<>(
      "Detection mode",
      "Signal-based uses a carbon-averagine envelope (no formula prediction). Formula prediction "
          + "enumerates candidate formulas and unions their predicted isotope patterns.",
      IsotopeFinderModeOptions.SIGNAL_BASED);

  public IsotopeFinderParameters() {
    super(new UserParameter[]{featureLists, elements, isotopeMzTolerance, maxCharge, fwhmRefine,
            mode},
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
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", getParameter(isotopeMzTolerance));
    return nameParameterMap;
  }
}
