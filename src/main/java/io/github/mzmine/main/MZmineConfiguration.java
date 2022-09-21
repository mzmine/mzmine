/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.main;

import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.StringCrypter;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

/**
 * MZmine configuration interface
 */
public interface MZmineConfiguration {

  File CONFIG_FILE = new File(FileUtils.getUserDirectory(), ".mzmine3.conf");

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

  void loadConfiguration(File file) throws IOException;

  void saveConfiguration(File file) throws IOException;

  String getRexecPath();

  Boolean getSendStatistics();

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

  StringCrypter getEncrypter();

  boolean isDarkMode();
}
