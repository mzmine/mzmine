/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.gui.preferences;

import static io.github.mzmine.util.files.ExtensionFilters.MSCONVERT;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.KeepInMemory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.download.AssetGroup;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OptOutParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPaletteParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithDownloadParameter;
import io.github.mzmine.parameters.parametertypes.paintscale.PaintScalePaletteParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.web.Proxy;
import io.github.mzmine.util.web.ProxyType;
import io.github.mzmine.util.web.ProxyUtils;
import io.mzio.users.gui.fx.UsersController;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.Nullable;

public class MZminePreferences extends SimpleParameterSet {

  public static final HiddenParameter<String> username = new HiddenParameter<>(
      new StringParameter("username", "last active username", "", false, true));

  public static final NumberFormatParameter mzFormat = new NumberFormatParameter("m/z value format",
      "Format of m/z values", false, new DecimalFormat("0.0000"));

  public static final NumberFormatParameter rtFormat = new NumberFormatParameter(
      "Retention time value format", "Format of retention time values", false,
      new DecimalFormat("0.00"));

  public static final NumberFormatParameter mobilityFormat = new NumberFormatParameter(
      "Mobility value format", "Format of mobility values", false, new DecimalFormat("0.000"));

  public static final NumberFormatParameter ccsFormat = new NumberFormatParameter(
      "CCS value format", "Format for collision cross section (CCS) values.", false,
      new DecimalFormat("0.0"));

  public static final NumberFormatParameter intensityFormat = new NumberFormatParameter(
      "Intensity format", "Format of intensity values", true, new DecimalFormat("0.0E0"));

  public static final NumberFormatParameter ppmFormat = new NumberFormatParameter("PPM format",
      "Format used for PPM values such as mass errors", true, new DecimalFormat("0.0"));

  public static final NumberFormatParameter scoreFormat = new NumberFormatParameter("Score format",
      "Format used for scores, e.g., Pearson correlation, cosine similarity etc.", false,
      new DecimalFormat("0.000"));

  public static final NumberFormatParameter percentFormat = new NumberFormatParameter(
      "Percent format", "Format used for percentages, e.g., relative errors (except ppm) etc.",
      false, new DecimalFormat("0.0 %"));

  public static final ComboParameter<UnitFormat> unitFormat = new ComboParameter<>("Unit format",
      "The default unit format to format e.g. axis labels in MZmine.",
      FXCollections.observableArrayList(UnitFormat.values()), UnitFormat.DIVIDE);

  public static final NumOfThreadsParameter numOfThreads = new NumOfThreadsParameter();

  public static final BooleanParameter runGCafterBatchStep = new BooleanParameter(
      "Free memory in batch (experimental)", """
      This option runs garbage collection after every step in batch mode.
      This reclaims free memory, but may also reduce processing throughput slightly, although the impact should be small.
      Typically the Java Virtual Machine will hold on to RAM and manage it to achieve the highest throughput.
      The recommendation is to keep this setting turned off.""", false);

  public static final BooleanParameter deleteTempFiles = new BooleanParameter(
      "Fast temp files cleanup", """
      Cleanup temp files as soon as possible. This is the new default behavior. \
      This options is only added temporarily to allow using the old temp file cleanup, which should not be necessary.
      Default is true (checked).""", true);

  public static final OptionalModuleParameter<ProxyParameters> proxySettings = new OptionalModuleParameter<>(
      "Use proxy", "Use proxy for internet connection?", new ProxyParameters(), false);

//  public static final BooleanParameter sendStatistics = new BooleanParameter(
//      "Send anonymous statistics", "Allow MZmine to send anonymous statistics on the module usage?",
//      true);
//  public static final OptionalModuleParameter sendErrorEMail = new OptionalModuleParameter(
//      "Send error e-Mail notifications", "Send error e-Mail notifications",
//      new ErrorMailSettings());

  public static final WindowSettingsParameter windowSetttings = new WindowSettingsParameter();

  public static final ColorPaletteParameter defaultColorPalette = new ColorPaletteParameter(
      "Default color palette",
      "Defines the default color palette used to create charts throughout MZmine");

  public static final PaintScalePaletteParameter defaultPaintScale = new PaintScalePaletteParameter(
      "Default paint scale",
      "Defines the default paint scale used to create charts throughout MZmine");

  public static final ParameterSetParameter<ChartThemeParameters> chartParam = new ParameterSetParameter<>(
      "Chart parameters", "The default chart parameters to be used throughout MZmine",
      new ChartThemeParameters());

  public static final ComboParameter<Themes> theme = new ComboParameter<>("Theme",
      "Select JavaFX style to theme the MZmine window.", Themes.values(), Themes.JABREF_LIGHT);

  public static final BooleanParameter presentationMode = new BooleanParameter("Presentation mode",
      "If checked, fonts in the MZmine gui will be enlarged. The chart fonts are still controlled by the chart theme.",
      false);

  public static final HiddenParameter<Map<String, Boolean>> imsModuleWarnings = new HiddenParameter<>(
      new OptOutParameter("Ion mobility compatibility warnings",
          "Shows a warning message when a module without explicit ion mobility support is "
          + "used to process ion mobility data."));

  public static final DirectoryParameter tempDirectory = new DirectoryParameter(
      "Temporary file directory", "Directory where temporary files"
                                  + " will be stored. Directory should be located on a drive with fast read and write "
                                  + "(e.g., an SSD). Requires a restart of MZmine to take effect (the program argument --temp "
                                  + "overrides this parameter, if set: --temp D:\\your_tmp_dir\\)",
      System.getProperty("java.io.tmpdir"));

  public static final ComboParameter<KeepInMemory> memoryOption = new ComboParameter<>(
      "Keep in memory", String.format(
      "Specifies the objects that are kept in memory rather than memory mapping "
      + "them into temp files in the temp directory. Parameter is overriden by the program "
      + "argument --memory. Depending on the read/write speed of the temp directory,"
      + " memory mapping is a fast and memory efficient way to handle data, therefore, the "
      + "default is to memory map all spectral data and feature data with the option %s. On "
      + "systems where memory (RAM) is no concern, viable options are %s and %s, to keep all in memory "
      + "or to keep mass lists and feauture data in memory, respectively.", KeepInMemory.NONE,
      KeepInMemory.ALL, KeepInMemory.MASSES_AND_FEATURES), KeepInMemory.values(),
      KeepInMemory.NONE);

  /*public static final BooleanParameter applyTimsPressureCompensation = new BooleanParameter(
      "Use MALDI-TIMS pressure compensation", """
      Specifies if mobility values from Bruker timsTOF fleX MALDI raw data shall be recalibrated using a Bruker algorithm.
      This compensation is applied during file import and cannot be applied afterwards.
      Will cause additional memory consumption, because every pixel might have it's own mobility calibration (in theory).
      In practical cases, this memory consumption is mostly negligible. 
      """, false);*/

  public static final BooleanParameter showPrecursorWindow = new BooleanParameter(
      "Show precursor windows", "Show the isolation window instead of just the precursor m/z.",
      true);

  public static final BooleanParameter showTempFolderAlert = new BooleanParameter("Show temp alert",
      "Show temp folder alert", true);

  public static final ComboParameter<ImageNormalization> imageNormalization = new ComboParameter<ImageNormalization>(
      "Normalize images",
      "Specifies if displayed images shall be normalized to the average TIC or shown according to the raw data."
      + "only applies to newly generated plots.", ImageNormalization.values(),
      ImageNormalization.NO_NORMALIZATION);

  public static final ComboParameter<PaintScaleTransform> imageTransformation = new ComboParameter<>(
      "Image paint scale transformation", "Transforms the paint scale for images.",
      PaintScaleTransform.values(), PaintScaleTransform.LINEAR);

  private static final NumberFormats exportFormat = new NumberFormats(new DecimalFormat("0.#####"),
      new DecimalFormat("0.####"), new DecimalFormat("0.####"), new DecimalFormat("0.##"),
      new DecimalFormat("0.###E0"), new DecimalFormat("0.##"), new DecimalFormat("0.####"),
      new DecimalFormat("0.###"), UnitFormat.DIVIDE);
  private final BooleanProperty darkModeProperty = new SimpleBooleanProperty(false);
  private NumberFormats guiFormat = exportFormat; // default value

  public static final FileNameParameter msConvertPath = new FileNameWithDownloadParameter(
      "MSConvert path",
      "Set a path to MSConvert to automatically convert unknown vendor formats to mzML while importing.",
      List.of(MSCONVERT), AssetGroup.MSCONVERT);

  public static final BooleanParameter keepConvertedFile = new BooleanParameter(
      "Keep files converted by MSConvert",
      "Store the files after conversion by MSConvert to an mzML file.\n"
      + "This will reduce the import time when re-processing, but require more disc space.", false);

  public static final BooleanParameter applyPeakPicking = new BooleanParameter(
      "Apply peak picking (recommended)", """
      Apply vendor peak picking during import of native vendor files with MSConvert.
      Using the vendor peak picking during conversion usually leads to better results that using a generic algorithm.
      Peak picking is only """, true);

  public static final ComboParameter<ThermoImportOptions> thermoImportChoice = new ComboParameter<>(
      "Thermo data import", """
      Specify which path you want to use for Thermo raw data import. MSConvert allows import of
      UV spectra and chromatograms and is therefore recommended, but only available on windows.
      """, ThermoImportOptions.getOptionsForOs(), ThermoImportOptions.MSCONVERT);

  public static final FileNameWithDownloadParameter thermoRawFileParserPath = new FileNameWithDownloadParameter(
      "Thermo raw file parser location", "The file path to the thermo raw file parser.", List.of(
      new ExtensionFilter("Executable or zip", "ThermoRawFileParser.exe",
          "ThermoRawFileParserLinux", "ThermoRawFileParserMac", "ThermoRawFileParser.zip"),
      new ExtensionFilter("zip", "ThermoRawFileParser.zip"),
      new ExtensionFilter("Windows executable", "ThermoRawFileParser.exe"),
      new ExtensionFilter("Linux executable", "ThermoRawFileParserLinux"),
      new ExtensionFilter("Mac executable", "ThermoRawFileParserMac")),
      AssetGroup.ThermoRawFileParser);

  public static final OptionalParameter<ParameterSetParameter<WatersLockmassParameters>> watersLockmass = new OptionalParameter<>(
      new ParameterSetParameter<>("Apply lockmass on import (Waters)",
          "Apply lockmass correction for native Waters raw data during raw data import via MSConvert.",
          new WatersLockmassParameters()), true);

  public MZminePreferences() {
    super(// start with performance
        numOfThreads, memoryOption, tempDirectory, runGCafterBatchStep, deleteTempFiles,
        proxySettings,
        /*applyTimsPressureCompensation,*/
        // visuals
        // number formats
        mzFormat, rtFormat, mobilityFormat, ccsFormat, intensityFormat, ppmFormat, scoreFormat,
        percentFormat,
        // how to format unit strings
        unitFormat,
        // other preferences
        defaultColorPalette, defaultPaintScale, chartParam, theme, presentationMode,
        imageNormalization, imageTransformation, showPrecursorWindow, imsModuleWarnings,
        windowSetttings,
        // silent parameters without controls
        showTempFolderAlert, username,
        //
        msConvertPath, keepConvertedFile, applyPeakPicking, watersLockmass, thermoRawFileParserPath,
        thermoImportChoice);

    darkModeProperty.subscribe(state -> {
      var oldTheme = getValue(theme);

      if (oldTheme.isDark() != state) {
        var theme = state ? Themes.JABREF_DARK : Themes.JABREF_LIGHT;
        setParameter(MZminePreferences.theme, theme);
        applyConfig(oldTheme);
      }
    });
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return showSetupDialog(valueCheckRequired, "");
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired, String filterParameters) {
    assert Platform.isFxApplicationThread();
    final Themes previousTheme = getValue(MZminePreferences.theme);
    GroupedParameterSetupDialog dialog = new GroupedParameterSetupDialog(valueCheckRequired, this);

    // add groups
    dialog.addParameterGroup("General", numOfThreads, memoryOption, tempDirectory,
        runGCafterBatchStep, deleteTempFiles, proxySettings
        /*, applyTimsPressureCompensation*/);
    dialog.addParameterGroup("Formats", mzFormat, rtFormat, mobilityFormat, ccsFormat,
        intensityFormat, ppmFormat, scoreFormat, unitFormat);
    dialog.addParameterGroup("Visuals", defaultColorPalette, defaultPaintScale, chartParam, theme,
        presentationMode, showPrecursorWindow, imageTransformation, imageNormalization);
    dialog.addParameterGroup("MS data import", msConvertPath, keepConvertedFile, applyPeakPicking,
        watersLockmass, thermoRawFileParserPath, thermoImportChoice);
//    dialog.addParameterGroup("Other", new Parameter[]{
    // imsModuleWarnings, showTempFolderAlert, windowSetttings  are hidden parameters
//    });
    dialog.setFilterText(filterParameters);

    // check
    dialog.showAndWait();
    final ExitCode retVal = dialog.getExitCode();
    if (retVal != ExitCode.OK) {
      return retVal;
    }

    applyConfig(previousTheme);

    return retVal;
  }

  public void applyConfig() {
    applyConfig(null);
  }

  public void applyConfig(final @Nullable Themes previousTheme) {
    // Update proxy settings
    updateSystemProxySettings();

    // enforce memory option (only applies to new data)
    final KeepInMemory keepInMemory = getValue(MZminePreferences.memoryOption);
    keepInMemory.enforceToMemoryMapping();

    final Themes theme = getValue(MZminePreferences.theme);
    if (previousTheme != null) {
      showDialogToAdjustColorsToTheme(previousTheme, theme);
    }
    // need to check as MZmineCore.getDesktop() throws exception if not initialized
    // if apply is called before window is opened the new settings will by taken up during window creation
    if (DesktopService.isGUI()) {
      theme.apply(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
      darkModeProperty.set(theme.isDark());

      Boolean presentation = getValue(MZminePreferences.presentationMode);
      if (presentation) {
        MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets()
            .add("themes/MZmine_default_presentation.css");
      } else {
        MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets()
            .removeIf(e -> e.contains("MZmine_default_presentation"));
      }
    }

    updateGuiFormat();

    final File tempDir = getValue(MZminePreferences.tempDirectory);
    if (tempDir != null && tempDir.isDirectory()) {
      if (!tempDir.exists()) {
        tempDir.mkdirs();
      }
      FileAndPathUtil.setTempDir(tempDir);
    }
    // delete temp files as soon as possible
    FileAndPathUtil.setEarlyTempFileCleanup(getValue(MZminePreferences.deleteTempFiles));
  }

  private void showDialogToAdjustColorsToTheme(Themes previousTheme, Themes theme) {
    if (previousTheme.isDark() != theme.isDark()) {
      final ChartThemeParameters chartParams = getValue(MZminePreferences.chartParam);
      final Color bgColor = chartParams.getValue(ChartThemeParameters.color);
      final FontSpecs axisFont = chartParams.getValue(ChartThemeParameters.axisLabelFont);
      final FontSpecs itemFont = chartParams.getValue(ChartThemeParameters.itemLabelFont);
      final FontSpecs titleFont = chartParams.getValue(ChartThemeParameters.titleFont);
      final FontSpecs subTitleFont = chartParams.getValue(ChartThemeParameters.subTitleFont);

      boolean changeColors = false;
      if (theme.isDark() && (ColorUtils.isDark(bgColor) || ColorUtils.isDark(axisFont.getColor())
                             || ColorUtils.isDark(itemFont.getColor()) || ColorUtils.isDark(
          titleFont.getColor()) || ColorUtils.isDark(subTitleFont.getColor()))) {
        if (DialogLoggerUtil.showDialogYesNo("Change theme?", """
            MZmine detected that you changed the GUI theme.
            The current chart theme colors might not be readable.
            Would you like to adapt them?
            """)) {
          changeColors = true;
        }
      } else if (!theme.isDark() && (ColorUtils.isLight(bgColor) || ColorUtils.isLight(
          axisFont.getColor()) || ColorUtils.isLight(itemFont.getColor()) || ColorUtils.isLight(
          titleFont.getColor()) || ColorUtils.isLight(subTitleFont.getColor()))) {
        if (DialogLoggerUtil.showDialogYesNo("Change theme?", """
            MZmine detected that you changed the GUI theme.
            The current chart theme colors might not be readable.
            Would you like to adapt them?
            """)) {
          changeColors = true;
        }
      }

      if (!changeColors) {
        return;
      }

      adjustColorsToThemeDarkMode(theme);
    }
  }

  private void adjustColorsToThemeDarkMode(final Themes theme) {
    final ChartThemeParameters chartParams = getValue(MZminePreferences.chartParam);
    final Color bgColor = chartParams.getValue(ChartThemeParameters.color);
    final FontSpecs axisFont = chartParams.getValue(ChartThemeParameters.axisLabelFont);
    final FontSpecs itemFont = chartParams.getValue(ChartThemeParameters.itemLabelFont);
    final FontSpecs titleFont = chartParams.getValue(ChartThemeParameters.titleFont);
    final FontSpecs subTitleFont = chartParams.getValue(ChartThemeParameters.subTitleFont);
    if (theme.isDark()) {
      if (ColorUtils.isLight(bgColor)) {
        chartParams.setParameter(ChartThemeParameters.color, Color.TRANSPARENT);
      }
      if (ColorUtils.isDark(axisFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.axisLabelFont,
            new FontSpecs(Color.WHITE, axisFont.getFont()));
      }
      if (ColorUtils.isDark(itemFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.itemLabelFont,
            new FontSpecs(Color.WHITE, itemFont.getFont()));
      }
      if (ColorUtils.isDark(titleFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.titleFont,
            new FontSpecs(Color.WHITE, titleFont.getFont()));
      }
      if (ColorUtils.isDark(subTitleFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.subTitleFont,
            new FontSpecs(Color.WHITE, subTitleFont.getFont()));
      }
    } else {
      if (ColorUtils.isDark(bgColor)) {
        chartParams.setParameter(ChartThemeParameters.color, Color.WHITE);
      }
      if (ColorUtils.isLight(axisFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.axisLabelFont,
            new FontSpecs(Color.BLACK, axisFont.getFont()));
      }
      if (ColorUtils.isLight(itemFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.itemLabelFont,
            new FontSpecs(Color.BLACK, itemFont.getFont()));
      }
      if (ColorUtils.isLight(titleFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.titleFont,
            new FontSpecs(Color.BLACK, titleFont.getFont()));
      }
      if (ColorUtils.isLight(subTitleFont.getColor())) {
        chartParams.setParameter(ChartThemeParameters.subTitleFont,
            new FontSpecs(Color.BLACK, subTitleFont.getFont()));
      }
    }
  }

  private void updateGuiFormat() {
    guiFormat = new NumberFormats(getValue(MZminePreferences.mzFormat),
        getValue(MZminePreferences.rtFormat), getValue(MZminePreferences.mobilityFormat),
        getValue(MZminePreferences.ccsFormat), getValue(MZminePreferences.intensityFormat),
        getValue(MZminePreferences.ppmFormat), getValue(MZminePreferences.percentFormat),
        getValue(MZminePreferences.scoreFormat), getValue(MZminePreferences.unitFormat));
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams) {
    updateSystemProxySettings();
    updateGuiFormat();
    darkModeProperty.set(getValue(MZminePreferences.theme).isDark());
    String username = ConfigService.getPreference(MZminePreferences.username);
    // this will set the current user to CurrentUserService
    // loads all users already logged in from the user folder
    if (StringUtils.hasValue(username)) {
      UsersController.getInstance().setCurrentUserByName(username);
    }
  }

  private void updateSystemProxySettings() {
    // Update system proxy settings
    Boolean proxyEnabled = getParameter(proxySettings).getValue();
    if ((proxyEnabled != null) && (proxyEnabled)) {
      ParameterSet proxyParams = getParameter(proxySettings).getEmbeddedParameters();
      String address = proxyParams.getParameter(ProxyParameters.proxyAddress).getValue();
      String port = proxyParams.getParameter(ProxyParameters.proxyPort).getValue();

      // some proxy urls contain http:// at the beginning, we need to filter this out
      if (address.startsWith("http://")) {
        proxyParams.setParameter(ProxyParameters.proxyType, ProxyType.HTTP);
        address = address.replaceFirst("http://", "");
      } else if (address.startsWith("https://")) {
        proxyParams.setParameter(ProxyParameters.proxyType, ProxyType.HTTPS);
        address = address.replaceFirst("https://", "");
      }

      final ProxyType proxyType = proxyParams.getValue(ProxyParameters.proxyType);
      // need to set both proxies anyway
      ProxyUtils.setSystemProxy(address, port, proxyType);
    } else {
      ProxyUtils.clearSystemProxy();
    }
  }


  public NumberFormats getExportFormats() {
    return exportFormat;
  }

  public NumberFormats getGuiFormats() {
    return guiFormat;
  }

  public boolean isDarkMode() {
    return darkModeProperty.getValue();
  }

  public void setDarkMode(final boolean dark) {
    darkModeProperty.set(dark);
  }

  public BooleanProperty darkModeProperty() {
    return darkModeProperty;
  }

  /**
   * Set system proxy in preferences and {@link ProxyUtils#setSystemProxy(Proxy)}
   */
  public void setProxy(final Proxy proxy) {
    OptionalModuleParameter<ProxyParameters> pp = getParameter(proxySettings);
    pp.setValue(proxy.active());
    ProxyParameters params = pp.getEmbeddedParameters();
    params.setProxy(proxy);
    ProxyUtils.setSystemProxy(proxy);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages,
      boolean skipRawDataAndFeatureListParameters) {
    final boolean superCheck = super.checkParameterValues(errorMessages,
        skipRawDataAndFeatureListParameters);

    return superCheck;
  }
}
