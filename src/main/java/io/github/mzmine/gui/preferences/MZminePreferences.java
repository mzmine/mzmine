/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.preferences;

import java.text.DecimalFormat;
import org.w3c.dom.Element;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.colorpalette.ColorPaletteParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.color.Vision;
import javafx.collections.FXCollections;

public class MZminePreferences extends SimpleParameterSet {

//  public static final ComboParameter<Vision> colorPalettes = new ComboParameter<>(
//      "Color palettes (color blindness mode)",
//      "Some modules use the color blindness aware palettes for a higher contrast. Think about using this mode even with \"normal vision\" to reach everyone.",
//      FXCollections.observableArrayList(Vision.values()), Vision.DEUTERANOPIA);

  public static final NumberFormatParameter mzFormat = new NumberFormatParameter("m/z value format",
      "Format of m/z values", false, new DecimalFormat("0.0000"));

  public static final NumberFormatParameter rtFormat =
      new NumberFormatParameter("Retention time value format", "Format of retention time values",
          false, new DecimalFormat("0.00"));

  public static final NumberFormatParameter intensityFormat = new NumberFormatParameter(
      "Intensity format", "Format of intensity values", true, new DecimalFormat("0.0E0"));

  public static final NumOfThreadsParameter numOfThreads = new NumOfThreadsParameter();

  public static final OptionalModuleParameter proxySettings = new OptionalModuleParameter(
      "Use proxy", "Use proxy for internet connection?", new ProxySettings());

  public static final FileNameParameter rExecPath = new FileNameParameter("R executable path",
      "Full R executable file path (If left blank, MZmine will try to find out automatically). On Windows, this should point to your R.exe file.",
      FileSelectionType.OPEN);

  public static final BooleanParameter sendStatistics =
      new BooleanParameter("Send anonymous statistics",
          "Allow MZmine to send anonymous statistics on the module usage?", true);

  public static final OptionalModuleParameter sendErrorEMail =
      new OptionalModuleParameter("Send error e-Mail notifications",
          "Send error e-Mail notifications", new ErrorMailSettings());

  public static final WindowSettingsParameter windowSetttings = new WindowSettingsParameter();

  public static final ColorPaletteParameter stdColorPalette =
      new ColorPaletteParameter("Main color palette",
          "Defines the default color palette used to create charts throughout MZmine");

  public static final ParameterSetParameter chartParam =
      new ParameterSetParameter("Chart parameters",
          "The default chart parameters to be used trhoughout MZmine", new ChartThemeParameters());

  public static final BooleanParameter darkMode = new BooleanParameter("Dark mode", "Enables dark mode throughout MZmine.", false);
  
  public MZminePreferences() {
    super(new Parameter[]{mzFormat, rtFormat, intensityFormat, numOfThreads,
        proxySettings, rExecPath, sendStatistics, windowSetttings, sendErrorEMail,
        stdColorPalette, chartParam});
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    ExitCode retVal = super.showSetupDialog(valueCheckRequired);

    if (retVal == ExitCode.OK) {

      // Update proxy settings
      updateSystemProxySettings();

      // Repaint windows to update number formats
      // MZmineCore.getDesktop().getMainWindow().repaint();
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
    } else {
      System.clearProperty("http.proxySet");
      System.clearProperty("http.proxyHost");
      System.clearProperty("http.proxyPort");
    }
  }

}
