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

package io.github.mzmine.main;

import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.Themes;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.StringCrypter;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * MZmine configuration interface
 */
public interface MZmineConfiguration {

  String CONFIG_EXTENSION = ".mzconfig";
  File CONFIG_FILE = FileAndPathUtil.resolveInMzmineDir(CONFIG_EXTENSION);

  ParameterSet getModuleParameters(Class<? extends MZmineModule> module);

  void setModuleParameters(Class<? extends MZmineModule> module, ParameterSet parameters);

  MZminePreferences getPreferences();

  /**
   * List of last loaded projects
   *
   * @return
   */
  @NotNull List<File> getLastProjects();


  /**
   * Number of user defined threads or system if selected
   */
  int getNumOfThreads();

  /**
   * List of last loaded projects
   *
   * @return
   */
  @NotNull FileNameListSilentParameter getLastProjectsParameter();

  NumberFormat getMZFormat();

  NumberFormat getRTFormat();

  NumberFormat getMobilityFormat();

  /**
   * @return The default collision cross section format
   * @see io.github.mzmine.modules.dataprocessing.id_ccscalc.CCSCalcModule
   */
  NumberFormat getCCSFormat();

  /**
   * A format for scores, e.g., Pearson correlation, cosine scores, etc. Default 0.000.
   *
   * @return
   */
  NumberFormat getScoreFormat();

  NumberFormat getIntensityFormat();

  NumberFormat getPPMFormat();

  NumberFormat getPercentFormat();

  UnitFormat getUnitFormat();

  void loadConfiguration(File file, boolean loadPreferences) throws IOException;

  void saveConfiguration(File file) throws IOException;

  default NumberFormats getFormats(boolean export) {
    return export ? getExportFormats() : getGuiFormats();
  }

  NumberFormats getGuiFormats();

  NumberFormats getExportFormats();

  /**
   * For color blindness or "normal vision"
   *
   * @return
   */
  // public Vision getColorVision();
  SimpleColorPalette getDefaultColorPalette();

  SimpleColorPalette getDefaultPaintScalePalette();

  ChartThemeParameters getDefaultChartThemeParameters();

  EStandardChartTheme getDefaultChartTheme();

  Themes getTheme();

  StringCrypter getEncrypter();

  boolean isDarkMode();

  ImageNormalization getImageNormalization();

  PaintScaleTransform getImageTransformation();

  File getMsConvertPath();

  @NotNull File getLogFile();

  /**
   * The current hold total memory. Consider using {@link #getMaxMemoryGB()}
   */
  default double getTotalMemoryGB() {
    final double GB = 1 << 30; // 1 GB
    return Runtime.getRuntime().totalMemory() / GB;
  }

  /**
   * @return the maximum memory the JVM will attempt to use
   */
  default double getMaxMemoryGB() {
    final double GB = 1 << 30; // 1 GB
    return Runtime.getRuntime().maxMemory() / GB; // shift by 30 bits to convert bytes to GB
  }

  /**
   * @return the current used memory in GB
   */
  default double getUsedMemoryGB() {
    final double GB = 1 << 30; // 1 GB
    final double totalMemGB = Runtime.getRuntime().totalMemory() / GB;
    return totalMemGB - Runtime.getRuntime().freeMemory() / GB;
  }
}
