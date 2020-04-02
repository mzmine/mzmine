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

package io.github.mzmine.main;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import javax.annotation.Nonnull;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameListSilentParameter;
import io.github.mzmine.util.StringCrypter;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.color.Vision;

/**
 * MZmine configuration interface
 */
public interface MZmineConfiguration {

  public static final File CONFIG_FILE = new File(System.getProperty("user.home"), ".mzmine3.conf");

  public ParameterSet getModuleParameters(Class<? extends MZmineModule> module);

  public void setModuleParameters(Class<? extends MZmineModule> module, ParameterSet parameters);

  public MZminePreferences getPreferences();

  /**
   * List of last loaded projects
   * 
   * @return
   */
  @Nonnull
  public List<File> getLastProjects();

  /**
   * List of last loaded projects
   * 
   * @return
   */
  @Nonnull
  public FileNameListSilentParameter getLastProjectsParameter();

  public NumberFormat getMZFormat();

  public NumberFormat getRTFormat();

  public NumberFormat getIntensityFormat();

  public void loadConfiguration(File file) throws IOException;

  public void saveConfiguration(File file) throws IOException;

  public String getRexecPath();

  public Boolean getSendStatistics();

  /**
   * For color blindness or "normal vision"
   * 
   * @return
   */
//  public Vision getColorVision();
  
  public SimpleColorPalette getDefaultColorPalette();
  
  public ChartThemeParameters getDefaultChartThemeParameters();
  
  public EStandardChartTheme getDefaultChartTheme();

  public StringCrypter getEncrypter();
}
