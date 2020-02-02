package io.github.mzmine.gui.chartbasics.chartthemes;

import io.github.mzmine.main.MZmineCore;

public class ChartThemeFactory2 {
  
//  private static ChartThemeParameters defChartParam;

  public static EStandardChartTheme createDefaultTheme(String name) {
    EStandardChartTheme theme = new EStandardChartTheme(name);
    
    ChartThemeParameters cp = MZmineCore.getConfiguration().getDefaultChartThemeParameters();

    cp.applyToChartTheme(theme);
  }
  
}
