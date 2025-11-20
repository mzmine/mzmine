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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.gui.preferences;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComponentWrapperParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.util.function.Supplier;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;

public class VendorImportParameters extends SimpleParameterSet {

  private static final String JUMP_TO_PREFERENCE_TOOLTIP = """
      Open the preference dialog, which controls this parameter for the drag & drop import and the mzwizard.
      Changing this parameter here does not influence the preferences and vice versa.""";

  // disabled, but moved from preferences. Disabled due to downstream bugs.
   /*public static final BooleanParameter applyTimsPressureCompensation = new BooleanParameter(
      "Use MALDI-TIMS pressure compensation", """
      Specifies if mobility values from Bruker timsTOF fleX MALDI raw data shall be recalibrated using a Bruker algorithm.
      This compensation is applied during file import and cannot be applied afterwards.
      Will cause additional memory consumption, because every pixel might have it's own mobility calibration (in theory).
      In practical cases, this memory consumption is mostly negligible.
      """, false);*/

  private static final boolean DEFAULT_VENDOR_CENTROIDING = true;
  public static final ComponentWrapperParameter<Boolean, BooleanParameter> applyVendorCentroiding = new ComponentWrapperParameter<>(
      new BooleanParameter("Try vendor centroiding", """
          Vendor centroiding will be applied to the imported raw data if this option is selected and centroiding is supported.
          Using the vendor peak picking during conversion usually leads to better results that using a generic algorithm.""",
          DEFAULT_VENDOR_CENTROIDING),
      createJumpToPrefButton(MZminePreferences.applyVendorCentroiding.getName()));

  /*private static final ThermoImportOptions DEFAULT_THERMO_IMPORT = ThermoImportOptions.THERMO_RAW_FILE_PARSER;
  public static final ComponentWrapperParameter<ThermoImportOptions, ComboParameter<ThermoImportOptions>> thermoImportChoice = new ComponentWrapperParameter<>(
      new ComboParameter<>("Thermo data import", """
          Specify which path you want to use for Thermo raw data import.
          """, ThermoImportOptions.getOptionsForOs(), DEFAULT_THERMO_IMPORT),
      createJumpToPrefButton(VendorImportParameters.thermoImportChoice.getName()));*/

  private static final boolean DEFAULT_WATERS_LOCKMASS_ENABLED = true;
  public static final ComponentWrapperParameter<Boolean, OptionalModuleParameter<WatersLockmassParameters>> watersLockmass = new ComponentWrapperParameter<>(
      new OptionalModuleParameter<>("Apply lockmass on import (Waters)",
          "Apply lockmass correction for native Waters raw data during raw data import via MSConvert.",
          new WatersLockmassParameters(), DEFAULT_WATERS_LOCKMASS_ENABLED),
      createJumpToPrefButton("Apply lockmass on import (Waters)"));

  private static final boolean DEFAULT_THERMO_EXCEPTION_SIGNALS = true;
  public static final ComponentWrapperParameter<Boolean, BooleanParameter> excludeThermoExceptionMasses = new ComponentWrapperParameter<>(
      new BooleanParameter("Remove calibrant signals (Thermo)",
          "Internal calibration signals may be present in MS1 and MS2 spectra from Thermo Orbitraps. This option automatically removes those on import.",
          DEFAULT_THERMO_EXCEPTION_SIGNALS),
      createJumpToPrefButton("Remove calibrant signals (Thermo)"));

  /*public static final ComboParameter<MassLynxImportOptions> watersImportChoice = new ComboParameter<>(
      "Waters MassLynx data import", """
      Select if Waters MassLynx data files shall be imported via MSConvert or via the native Waters library.
      The MSConvert import allows to retain the mzml files, allowing a faster import on the second iteration,
      but does not allow centroiding of IMS data files. The native import is slow when applying centroiding on import.""",
      MassLynxImportOptions.values(), MassLynxImportOptions.NATIVE);*/

  private static @NotNull Supplier<Node> createJumpToPrefButton(String preferenceParameterName) {
    return () -> FxButtons.createButton(null, FxIcons.GEAR_PREFERENCES, JUMP_TO_PREFERENCE_TOOLTIP,
        () -> ConfigService.getPreferences().showSetupDialog(true, preferenceParameterName));
  }

  public VendorImportParameters() {
    super(applyVendorCentroiding, watersLockmass, excludeThermoExceptionMasses);
  }

  public static VendorImportParameters create(boolean applyCentroiding,
      boolean watersLockmassEnabled, WatersLockmassParameters lockmassParam,
      boolean removeThermoExceptionMasses) {
    final VendorImportParameters param = (VendorImportParameters) new VendorImportParameters().cloneParameterSet();

    param.setParameter(applyVendorCentroiding, applyCentroiding);
    param.getParameter(watersLockmass).setValue(watersLockmassEnabled);
    param.getParameter(watersLockmass).getEmbeddedParameter().setEmbeddedParameters(lockmassParam);
    param.setParameter(excludeThermoExceptionMasses, removeThermoExceptionMasses);
    return param;
  }

  /**
   * Creates a copy of the currently selected vendor parameters in the preferences. Used for drag &
   * drop and wizard import.
   */
  public static VendorImportParameters createFromPreferences() {
    final MZminePreferences preferences = ConfigService.getConfiguration().getPreferences();
    return create(preferences.getValue(MZminePreferences.applyVendorCentroiding),
        /*preferences.getValue(MZminePreferences.thermoImportChoice)*/
        preferences.getValue(MZminePreferences.watersLockmass),
        preferences.getParameter(MZminePreferences.watersLockmass).getEmbeddedParameters(),
        preferences.getValue(MZminePreferences.excludeThermoExceptionMasses));
  }
}
