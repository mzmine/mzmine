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
import io.github.mzmine.modules.dataprocessing.filter_scan_signals.ScanSignalRemovalModule;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ComponentWrapperParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.util.function.Supplier;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;

public class VendorImportParameters extends SimpleParameterSet {

  /*private static final ThermoImportOptions DEFAULT_THERMO_IMPORT = ThermoImportOptions.THERMO_RAW_FILE_PARSER;
  public static final ComponentWrapperParameter<ThermoImportOptions, ComboParameter<ThermoImportOptions>> thermoImportChoice = new ComponentWrapperParameter<>(
      new ComboParameter<>("Thermo data import", """
          Specify which path you want to use for Thermo raw data import.
          """, ThermoImportOptions.getOptionsForOs(), DEFAULT_THERMO_IMPORT),
      createJumpToPrefButton(VendorImportParameters.thermoImportChoice.getName()));*/
  // disabled, but moved from preferences. Disabled due to downstream bugs.
   /*public static final BooleanParameter applyTimsPressureCompensation = new BooleanParameter(
      "Use MALDI-TIMS pressure compensation", """
      Specifies if mobility values from Bruker timsTOF fleX MALDI raw data shall be recalibrated using a Bruker algorithm.
      This compensation is applied during file import and cannot be applied afterwards.
      Will cause additional memory consumption, because every pixel might have it's own mobility calibration (in theory).
      In practical cases, this memory consumption is mostly negligible.
      """, false);*/

  public static final MassLynxImportOptions DEFAULT_WATERS_OPTION = MassLynxImportOptions.NATIVE_MZMINE_CENTROIDING;
  public static final boolean DEFAULT_VENDOR_CENTROIDING = true;
  public static final boolean DEFAULT_WATERS_LOCKMASS_ENABLED = true;
  public static final boolean DEFAULT_THERMO_EXCEPTION_SIGNALS = true;

  private static final String JUMP_TO_PREFERENCE_TOOLTIP = """
      Open the preference dialog, which controls this parameter for the drag & drop import and the mzwizard.
      Changing this parameter here does not influence the preferences and vice versa.""";

  public static final ComponentWrapperParameter<MassLynxImportOptions, ComboParameter<MassLynxImportOptions>> massLynxImportChoice = new ComponentWrapperParameter<>(
      new ComboParameter<>("Waters MassLynx data import", """
          Select if Waters MassLynx data files shall be imported via MSConvert or via the native Waters library.
          The MSConvert import allows to retain the mzml files, allowing a faster import on the second iteration,
          but does not allow centroiding of IMS data files.
          The native import allows centroiding by mzmine or Waters. Note that the Waters centroiding is
          slow for IMS data. Centroiding is only applied if "Try vendor centroiding" is enabled.""",
          MassLynxImportOptions.values(), DEFAULT_WATERS_OPTION),
      createJumpToPrefButton("Waters MassLynx data import"));

  public static final ComponentWrapperParameter<Boolean, BooleanParameter> applyVendorCentroiding = new ComponentWrapperParameter<>(
      new BooleanParameter("Try vendor centroiding", """
          Vendor centroiding will be applied to the imported raw data if this option is selected and centroiding is supported.
          Using the vendor peak picking during conversion usually leads to better results that using a generic algorithm.""",
          DEFAULT_VENDOR_CENTROIDING),
      createJumpToPrefButton(MZminePreferences.applyVendorCentroiding.getName()));

  public static final ComponentWrapperParameter<Boolean, OptionalModuleParameter<WatersLockmassParameters>> watersLockmass = new ComponentWrapperParameter<>(
      new OptionalModuleParameter<>("Apply lockmass on import (Waters)",
          "Apply lockmass correction for native Waters raw data during raw data import via MSConvert.",
          new WatersLockmassParameters(), DEFAULT_WATERS_LOCKMASS_ENABLED),
      createJumpToPrefButton("Apply lockmass on import (Waters)"));

  public static final ComponentWrapperParameter<Boolean, BooleanParameter> excludeThermoExceptionMasses = new ComponentWrapperParameter<>(
      new BooleanParameter("Remove calibrant signals (Thermo)", """
          Internal calibration signals may be present in spectra of all MS-levels (MS1-MSn) from Thermo Orbitraps.
          This filter automatically removes those on import for MS1 and MS2 spectra.
          To remove them from MS>=3 spectra, use "%s".
          """.formatted(ScanSignalRemovalModule.MODULE_NAME), DEFAULT_THERMO_EXCEPTION_SIGNALS),
      createJumpToPrefButton("Remove calibrant signals (Thermo)"));

  public VendorImportParameters() {
    super(applyVendorCentroiding, excludeThermoExceptionMasses, watersLockmass,
        massLynxImportChoice);
  }

  private static @NotNull Supplier<Node> createJumpToPrefButton(String preferenceParameterName) {
    return () -> FxButtons.createButton(null, FxIcons.GEAR_PREFERENCES, JUMP_TO_PREFERENCE_TOOLTIP,
        () -> ConfigService.getPreferences().showSetupDialog(true, preferenceParameterName));
  }

  public static VendorImportParameters create(boolean applyCentroiding,
      MassLynxImportOptions massLynxOption, boolean watersLockmassEnabled,
      WatersLockmassParameters lockmassParam, boolean removeThermoExceptionMasses) {
    final VendorImportParameters param = (VendorImportParameters) new VendorImportParameters().cloneParameterSet();

    param.setParameter(applyVendorCentroiding, applyCentroiding);
    param.setParameter(massLynxImportChoice, massLynxOption);
    param.getParameter(watersLockmass).setValue(watersLockmassEnabled);
    param.getParameter(watersLockmass).getEmbeddedParameter().setEmbeddedParameters(lockmassParam);
    param.setParameter(excludeThermoExceptionMasses, removeThermoExceptionMasses);
    return param;
  }

  public static VendorImportParameters createDefault() {
    return create(DEFAULT_VENDOR_CENTROIDING, DEFAULT_WATERS_OPTION,
        DEFAULT_WATERS_LOCKMASS_ENABLED, WatersLockmassParameters.createDefault(),
        DEFAULT_THERMO_EXCEPTION_SIGNALS);
  }

  /**
   * Creates a copy of the currently selected vendor parameters in the preferences. Used for drag &
   * drop and wizard import.
   */
  public static VendorImportParameters createFromPreferences() {
    final MZminePreferences preferences = ConfigService.getConfiguration().getPreferences();
    return create(preferences.getValue(MZminePreferences.applyVendorCentroiding),
        /*preferences.getValue(MZminePreferences.thermoImportChoice)*/
        preferences.getValue(MZminePreferences.massLynxImportChoice),
        preferences.getValue(MZminePreferences.watersLockmass),
        preferences.getParameter(MZminePreferences.watersLockmass).getEmbeddedParameters(),
        preferences.getValue(MZminePreferences.excludeThermoExceptionMasses));
  }
}
