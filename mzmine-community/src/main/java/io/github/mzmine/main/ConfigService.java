/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.main;

import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;

public final class ConfigService {

  private static final Logger logger = Logger.getLogger(ConfigService.class.getName());
  private static final MZmineConfiguration config = new MZmineConfigurationImpl();

  /**
   * only set from cli
   */
  private static volatile boolean tdfPseudoProfile = false;

  /**
   * only set from cli
   */
  private static volatile boolean ignoreParameterWarningsInBatch = false;

  public static MZmineConfiguration getConfiguration() {
    return config;
  }

  public static MZminePreferences getPreferences() {
    return config.getPreferences();
  }

  public static <V, T extends Parameter<V>> V getPreference(T parameter) {
    return config.getPreferences().getValue(parameter);
  }

  public static NumberFormats getExportFormats() {
    return config.getExportFormats();
  }

  public static NumberFormats getGuiFormats() {
    return config.getGuiFormats();
  }

  public static SimpleColorPalette getDefaultColorPalette() {
    return config.getDefaultColorPalette();
  }

  public static BooleanProperty isDarkModeProperty() {
    return config.getPreferences().darkModeProperty();
  }

  public static void setDarkMode(final Boolean dark) {
    getPreferences().setDarkMode(dark);
  }

  /**
   * Save current config to user directory
   *
   * @return true if successful, false otherwise
   */
  public static boolean saveUserConfig() {
    try {
      getConfiguration().saveConfiguration(MZmineConfiguration.CONFIG_FILE);
      return true;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot save user config", e);
      return false;
    }
  }

  static void setTdfPseudoProfile(final boolean tdfPseudoProfile) {
    ConfigService.tdfPseudoProfile = tdfPseudoProfile;
  }

  public static boolean isTdfPseudoProfile() {
    return tdfPseudoProfile;
  }

  public static boolean isApplyVendorCentroiding() {
    return getPreferences().getValue(MZminePreferences.applyVendorCentroiding);
  }

  public static void openTempPreferences() {
    MZminePreferences pref = MZmineCore.getConfiguration().getPreferences();
    FxThread.runLater(() -> pref.showSetupDialog(true, "temp"));
  }

  public static boolean isIgnoreParameterWarningsInBatch() {
    return ignoreParameterWarningsInBatch;
  }

  public static void setIgnoreParameterWarningsInBatch(boolean ignoreParameterWarningsInBatch) {
    ConfigService.ignoreParameterWarningsInBatch = ignoreParameterWarningsInBatch;
  }
}
