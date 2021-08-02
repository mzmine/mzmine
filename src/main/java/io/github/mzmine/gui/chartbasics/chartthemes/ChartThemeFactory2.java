/*
 * Copyright 2006-2021 The MZmine Development Team
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

package io.github.mzmine.gui.chartbasics.chartthemes;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.color.SimpleColorPalette;
import org.jfree.chart.ui.RectangleInsets;

/**
 * This is a dumped down version of the original ChartThemeFactory. It is to be seen if this is
 * needed. Ideally, this and the old version should be removed and everything should use the
 * preferences.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ChartThemeFactory2 {

  public static EStandardChartTheme createDefaultTheme(String name) {
    EStandardChartTheme theme = new EStandardChartTheme(name);
    ChartThemeParameters ctp = MZmineCore.getConfiguration().getDefaultChartThemeParameters();
    ctp.applyToChartTheme(theme);
    SimpleColorPalette scp = MZmineCore.getConfiguration().getDefaultColorPalette();
    scp.applyToChartTheme(theme);
    return theme;
  }

  /**
   * Creates a chart theme with no axis offset better suited for exporting due to a cleaner look.
   *
   * @param name
   * @return
   */
  public static EStandardChartTheme createExportChartTheme(String name) {
    EStandardChartTheme theme = createDefaultTheme(name);
    theme.setDefaultAxisOffset(RectangleInsets.ZERO_INSETS);
    theme.setMirrorPlotAxisOffset(RectangleInsets.ZERO_INSETS);
    return theme;
  }

}
