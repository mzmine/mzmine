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
