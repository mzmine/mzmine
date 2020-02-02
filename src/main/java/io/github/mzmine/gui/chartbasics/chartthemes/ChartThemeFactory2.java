package io.github.mzmine.gui.chartbasics.chartthemes;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.color.SimpleColorPalette;

public class ChartThemeFactory2 {
  
//  private static ChartThemeParameters defChartParam;

  public static EStandardChartTheme createDefaultTheme(String name) {
    EStandardChartTheme theme = new EStandardChartTheme(name);
    
    ChartThemeParameters ctp = MZmineCore.getConfiguration().getDefaultChartThemeParameters();

    ctp.applyToChartTheme(theme);
    
    SimpleColorPalette scp = MZmineCore.getConfiguration().getDefaultColorPalette();
    
    scp.applyToChartTheme(theme);
    
    return theme;
  }
  
}
