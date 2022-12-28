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

package io.github.mzmine.gui.preferences;

import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.main.KeepInMemory;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.GroupedParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.OptOutParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPaletteParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.paintscale.PaintScalePaletteParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.ExitCode;
import java.text.DecimalFormat;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import org.w3c.dom.Element;

public class MZminePreferences extends SimpleParameterSet {

  // public static final ComboParameter<Vision> colorPalettes = new ComboParameter<>(
  // "Color palettes (color blindness mode)",
  // "Some modules use the color blindness aware palettes for a higher contrast. Think about using
  // this mode even with \"normal vision\" to reach everyone.",
  // FXCollections.observableArrayList(Vision.values()), Vision.DEUTERANOPIA);

  public static final NumberFormatParameter mzFormat = new NumberFormatParameter("m/z value format",
      "Format of m/z values", false, new DecimalFormat("0.0000"));

  public static final NumberFormatParameter rtFormat = new NumberFormatParameter(
      "Retention time value format", "Format of retention time values", false,
      new DecimalFormat("0.00"));

  public static final NumberFormatParameter mobilityFormat = new NumberFormatParameter(
      "Mobility value format", "Format of mobility values", false, new DecimalFormat("0.000"));

  public static final NumberFormatParameter ccsFormat = new NumberFormatParameter(
      "CCS value format", "Format for colission cross section (CCS) values.", false,
      new DecimalFormat("0.0"));

  public static final NumberFormatParameter intensityFormat = new NumberFormatParameter(
      "Intensity format", "Format of intensity values", true, new DecimalFormat("0.0E0"));

  public static final NumberFormatParameter ppmFormat = new NumberFormatParameter("PPM format",
      "Format used for PPM values such as mass errors", true, new DecimalFormat("0.0000"));

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

  public static final OptionalModuleParameter proxySettings = new OptionalModuleParameter(
      "Use proxy", "Use proxy for internet connection?", new ProxySettings(), false);

  public static final FileNameParameter rExecPath = new FileNameParameter("R executable path",
      "Full R executable file path (If left blank, MZmine will try to find out automatically). On Windows, this should point to your R.exe file.",
      FileSelectionType.OPEN);

  public static final BooleanParameter sendStatistics = new BooleanParameter(
      "Send anonymous statistics", "Allow MZmine to send anonymous statistics on the module usage?",
      true);

  public static final OptionalModuleParameter sendErrorEMail = new OptionalModuleParameter(
      "Send error e-Mail notifications", "Send error e-Mail notifications",
      new ErrorMailSettings());

  public static final WindowSettingsParameter windowSetttings = new WindowSettingsParameter();

  public static final ColorPaletteParameter defaultColorPalette = new ColorPaletteParameter(
      "Default color palette",
      "Defines the default color palette used to create charts throughout MZmine");

  public static final PaintScalePaletteParameter defaultPaintScale = new PaintScalePaletteParameter(
      "Default paint scale",
      "Defines the default paint scale used to create charts throughout MZmine");

  public static final ParameterSetParameter chartParam = new ParameterSetParameter(
      "Chart parameters", "The default chart parameters to be used throughout MZmine",
      new ChartThemeParameters());

  public static final ComboParameter<Themes> theme = new ComboParameter<>("Theme",
      "Select JavaFX style to theme the MZmine window.", Themes.values(), Themes.MZMINE_LIGHT);

  public static final BooleanParameter presentationMode = new BooleanParameter("Presentation mode",
      "If checked, fonts in the MZmine gui will be enlarged. The chart fonts are still controlled by the chart theme.",
      false);

  public static final HiddenParameter<OptOutParameter, Map<String, Boolean>> imsModuleWarnings = new HiddenParameter<>(
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

  public static final BooleanParameter showPrecursorWindow = new BooleanParameter(
      "Show precursor windows", "Show the isolation window instead of just the precursor m/z.",
      false);

  public static final BooleanParameter showTempFolderAlert = new BooleanParameter("Show temp alert",
      "Show temp folder alert", true);

  public static final ComboParameter<ImageNormalization> imageNormalization = new ComboParameter<ImageNormalization>(
      "Normalize images",
      "Specifies if displayed images shall be normalized to the average TIC or shown according to the raw data."
          + "only applies to newly generated plots.", ImageNormalization.values(),
      ImageNormalization.NO_NORMALIZATION);

  private boolean isDarkMode = false;

  public MZminePreferences() {
    super(new Parameter[]{
        // start with performance
        numOfThreads, memoryOption, tempDirectory, proxySettings, rExecPath, sendStatistics,
        // visuals
        // number formats
        mzFormat, rtFormat, mobilityFormat, ccsFormat, intensityFormat, ppmFormat, scoreFormat,
        percentFormat,
        // how to format unit strings
        unitFormat,
        // other preferences
        defaultColorPalette, defaultPaintScale, chartParam, theme, presentationMode,
        imageNormalization, showPrecursorWindow, imsModuleWarnings, windowSetttings, sendErrorEMail,
        // silent parameters without controls
        showTempFolderAlert});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    return showSetupDialog(valueCheckRequired, "");
  }

  public ExitCode showSetupDialog(boolean valueCheckRequired, String filterParameters) {
    assert Platform.isFxApplicationThread();
    GroupedParameterSetupDialog dialog = new GroupedParameterSetupDialog(valueCheckRequired, this);

    // add groups
    dialog.addParameterGroup("General",
        new Parameter[]{numOfThreads, memoryOption, tempDirectory, proxySettings, rExecPath,
            sendStatistics});
    dialog.addParameterGroup("Formats",
        new Parameter[]{mzFormat, rtFormat, mobilityFormat, ccsFormat, intensityFormat, ppmFormat,
            scoreFormat, unitFormat});
    dialog.addParameterGroup("Visuals",
        new Parameter[]{defaultColorPalette, defaultPaintScale, chartParam, theme,
            presentationMode, showPrecursorWindow, imageNormalization});
    dialog.addParameterGroup("Other", new Parameter[]{sendErrorEMail,
        // imsModuleWarnings, showTempFolderAlert, windowSetttings  are hidden parameters
    });
    dialog.setFilterText(filterParameters);

    // check
    dialog.showAndWait();
    ExitCode retVal = dialog.getExitCode();

    if (retVal == ExitCode.OK) {

      // Update proxy settings
      updateSystemProxySettings();

      // enforce memory option (only applies to new data)
      final KeepInMemory keepInMemory = MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.memoryOption).getValue();
      keepInMemory.enforceToMemoryMapping();

      // Repaint windows to update number formats
      // MZmineCore.getDesktop().getMainWindow().repaint();

      final Themes theme = getValue(MZminePreferences.theme);
      theme.apply(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

      System.out.println(
          MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets().toString());

      Boolean presentation = MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.presentationMode).getValue();
      if (presentation) {
        MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets()
            .add("themes/MZmine_default_presentation.css");
      } else {
        MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets()
            .removeIf(e -> e.contains("MZmine_default_presentation"));
      }
    }

    return retVal;
  }

  @Override
  public void loadValuesFromXML(Element xmlElement) {
    super.loadValuesFromXML(xmlElement);
    updateSystemProxySettings();
  }

  private void updateSystemProxySettings() {
    // Update system proxy settings
    Boolean proxyEnabled = getParameter(proxySettings).getValue();
    if ((proxyEnabled != null) && (proxyEnabled)) {
      ParameterSet proxyParams = getParameter(proxySettings).getEmbeddedParameters();
      String address = proxyParams.getParameter(ProxySettings.proxyAddress).getValue();
      String port = proxyParams.getParameter(ProxySettings.proxyPort).getValue();
      System.setProperty("http.proxySet", "true");
      System.setProperty("http.proxyHost", address);
      System.setProperty("http.proxyPort", port);

      System.setProperty("https.proxySet", "true");
      System.setProperty("https.proxyHost", address);
      System.setProperty("https.proxyPort", port);
    } else {
      System.clearProperty("http.proxySet");
      System.clearProperty("http.proxyHost");
      System.clearProperty("http.proxyPort");

      System.clearProperty("https.proxySet");
      System.clearProperty("https.proxyHost");
      System.clearProperty("https.proxyPort");
    }
  }

  public boolean isDarkMode() {
    return getValue(MZminePreferences.theme).isDark();
  }
}
