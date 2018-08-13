/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.chartbasics.chartthemes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import org.jfree.chart.ChartColor;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.ui.RectangleInsets;

/**
 * Creates {@link EStandardChartTheme}s
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartThemeFactory {

  public enum THEME {
    // main themes
    BNW_PRINT, KARST, DARKNESS,
    // separate options
    FOR_PRINT, FOR_PRESENTATION;

    public static THEME getTheme(String ident) {
      // integer?
      try {
        int i = Integer.parseInt(ident);
        switch (i) {
          case 0:
            return BNW_PRINT;
          case 1:
            return KARST;
          case 2:
            return DARKNESS;
          case 50:
            return FOR_PRINT;
          case 51:
            return FOR_PRESENTATION;
        }
      } catch (Exception ex) {
      }
      // else value of
      return valueOf(ident);
    }

  }

  // the standard to be applied to all new charts
  protected static THEME standardTheme = THEME.BNW_PRINT;

  public static EStandardChartTheme createChartTheme(THEME theme) {
    switch (theme) {
      case BNW_PRINT:
        return createBlackNWhiteTheme();
      case DARKNESS:
        return createDarknessTheme();
      case KARST:
        return createKarstTheme();
    }
    return createBlackNWhiteTheme();
  }

  public static EStandardChartTheme changeChartThemeForPrintOrPresentation(
      EStandardChartTheme theme, boolean forPrint) {
    if (forPrint) {
      // Fonts
      theme.setExtraLargeFont(new Font("Arial", Font.BOLD, 16));
      theme.setLargeFont(new Font("Arial", Font.BOLD, 11));
      theme.setRegularFont(new Font("Arial", Font.PLAIN, 11));
      theme.setSmallFont(new Font("Arial", Font.PLAIN, 11));
    } else { // for presentation larger fonts
      // Fonts
      theme.setExtraLargeFont(new Font("Arial", Font.BOLD, 30));
      theme.setLargeFont(new Font("Arial", Font.BOLD, 20));
      theme.setRegularFont(new Font("Arial", Font.PLAIN, 16));
      theme.setSmallFont(new Font("Arial", Font.PLAIN, 16));
    }
    return theme;
  }

  public static EStandardChartTheme createBlackNWhiteTheme() {
    EStandardChartTheme theme = new EStandardChartTheme(THEME.BNW_PRINT, "BnW");
    // Fonts
    theme.setExtraLargeFont(new Font("Arial", Font.BOLD, 16));
    theme.setLargeFont(new Font("Arial", Font.BOLD, 11));
    theme.setRegularFont(new Font("Arial", Font.PLAIN, 11));
    theme.setSmallFont(new Font("Arial", Font.PLAIN, 11));

    // Paints
    theme.setTitlePaint(Color.black);
    theme.setSubtitlePaint(Color.black);
    theme.setLegendItemPaint(Color.black);
    theme.setPlotOutlinePaint(Color.black);
    theme.setBaselinePaint(Color.black);
    theme.setCrosshairPaint(Color.black);
    theme.setLabelLinkPaint(Color.black);
    theme.setTickLabelPaint(Color.black);
    theme.setAxisLabelPaint(Color.black);
    theme.setShadowPaint(Color.black);
    theme.setItemLabelPaint(Color.black);

    theme.setLegendBackgroundPaint(Color.white);
    theme.setChartBackgroundPaint(Color.white);
    theme.setPlotBackgroundPaint(Color.white);

    // paint sequence: add black
    Paint[] colors = new Paint[] {Color.BLACK, new Color(0xFF, 0x55, 0x55),
        new Color(0x55, 0x55, 0xFF), new Color(0x55, 0xFF, 0x55), new Color(0xFF, 0xFF, 0x55),
        new Color(0xFF, 0x55, 0xFF), new Color(0x55, 0xFF, 0xFF), Color.pink, Color.gray,
        ChartColor.DARK_RED, ChartColor.DARK_BLUE, ChartColor.DARK_GREEN, ChartColor.DARK_YELLOW,
        ChartColor.DARK_MAGENTA, ChartColor.DARK_CYAN, Color.darkGray, ChartColor.LIGHT_RED,
        ChartColor.LIGHT_BLUE, ChartColor.LIGHT_GREEN, ChartColor.LIGHT_YELLOW,
        ChartColor.LIGHT_MAGENTA, ChartColor.LIGHT_CYAN, Color.lightGray, ChartColor.VERY_DARK_RED,
        ChartColor.VERY_DARK_BLUE, ChartColor.VERY_DARK_GREEN, ChartColor.VERY_DARK_YELLOW,
        ChartColor.VERY_DARK_MAGENTA, ChartColor.VERY_DARK_CYAN, ChartColor.VERY_LIGHT_RED,
        ChartColor.VERY_LIGHT_BLUE, ChartColor.VERY_LIGHT_GREEN, ChartColor.VERY_LIGHT_YELLOW,
        ChartColor.VERY_LIGHT_MAGENTA, ChartColor.VERY_LIGHT_CYAN};

    theme.setDrawingSupplier(
        new DefaultDrawingSupplier(colors, DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
    theme.setErrorIndicatorPaint(Color.black);
    theme.setGridBandPaint(new Color(255, 255, 255, 20));
    theme.setGridBandAlternatePaint(new Color(255, 255, 255, 40));

    // axis
    Color transp = new Color(0, 0, 0, 200);
    theme.setRangeGridlinePaint(transp);
    theme.setDomainGridlinePaint(transp);

    theme.setAxisLinePaint(Color.black);

    // axis offset
    theme.setAxisOffset(new RectangleInsets(0, 0, 0, 0));

    return theme;
  }


  /**
   * Creates and returns a theme called "Darkness". In this theme, the charts have a black
   * background and white lines and labels
   *
   * @return The "Darkness" theme.
   */
  public static EStandardChartTheme createDarknessTheme() {
    EStandardChartTheme theme = new EStandardChartTheme(THEME.DARKNESS, "Darkness");
    // Fonts
    theme.setExtraLargeFont(new Font("Arial", Font.BOLD, 20));
    theme.setLargeFont(new Font("Arial", Font.BOLD, 11));
    theme.setRegularFont(new Font("Arial", Font.PLAIN, 11));
    theme.setSmallFont(new Font("Arial", Font.PLAIN, 11));
    //
    theme.setTitlePaint(Color.white);
    theme.setSubtitlePaint(Color.white);
    theme.setLegendBackgroundPaint(Color.black);
    theme.setLegendItemPaint(Color.white);
    theme.setChartBackgroundPaint(Color.black);
    theme.setPlotBackgroundPaint(Color.black);
    theme.setPlotOutlinePaint(Color.yellow);
    theme.setBaselinePaint(Color.white);
    theme.setCrosshairPaint(Color.red);
    theme.setLabelLinkPaint(Color.lightGray);
    theme.setTickLabelPaint(Color.white);
    theme.setAxisLabelPaint(Color.white);
    theme.setShadowPaint(Color.darkGray);
    theme.setItemLabelPaint(Color.white);
    theme.setDrawingSupplier(new DefaultDrawingSupplier(
        new Paint[] {Color.WHITE, Color.decode("0xFFFF00"), Color.decode("0x0036CC"),
            Color.decode("0xFF0000"), Color.decode("0xFFFF7F"), Color.decode("0x6681CC"),
            Color.decode("0xFF7F7F"), Color.decode("0xFFFFBF"), Color.decode("0x99A6CC"),
            Color.decode("0xFFBFBF"), Color.decode("0xA9A938"), Color.decode("0x2D4587")},
        new Paint[] {Color.decode("0xFFFF00"), Color.decode("0x0036CC")},
        new Stroke[] {new BasicStroke(2.0f)}, new Stroke[] {new BasicStroke(0.5f)},
        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
    theme.setErrorIndicatorPaint(Color.lightGray);
    theme.setGridBandPaint(new Color(255, 255, 255, 20));
    theme.setGridBandAlternatePaint(new Color(255, 255, 255, 40));

    // axis
    Color transp = new Color(255, 255, 255, 200);
    theme.setRangeGridlinePaint(transp);
    theme.setDomainGridlinePaint(transp);

    theme.setAxisLinePaint(Color.white);

    theme.setMasterFontColor(Color.WHITE);
    // axis offset
    theme.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
    return theme;
  }

  /**
   * Creates the theme called "Karst". In this theme, the charts have a blue background and yellow
   * lines and labels.
   *
   * @return The "Karst" theme.
   */
  public static EStandardChartTheme createKarstTheme() {
    EStandardChartTheme theme = new EStandardChartTheme(THEME.KARST, "Karst");
    // Fonts
    theme.setExtraLargeFont(new Font("Arial", Font.BOLD, 20));
    theme.setLargeFont(new Font("Arial", Font.BOLD, 11));
    theme.setRegularFont(new Font("Arial", Font.PLAIN, 11));
    theme.setSmallFont(new Font("Arial", Font.PLAIN, 11));
    //
    Paint bg = new Color(50, 50, 202);
    //
    theme.setTitlePaint(Color.green);
    theme.setSubtitlePaint(Color.yellow);
    theme.setLegendBackgroundPaint(bg);
    theme.setLegendItemPaint(Color.yellow);
    theme.setChartBackgroundPaint(bg);
    theme.setPlotBackgroundPaint(bg);
    theme.setPlotOutlinePaint(Color.yellow);
    theme.setBaselinePaint(Color.white);
    theme.setCrosshairPaint(Color.red);
    theme.setLabelLinkPaint(Color.lightGray);
    theme.setTickLabelPaint(Color.yellow);
    theme.setAxisLabelPaint(Color.yellow);
    theme.setShadowPaint(Color.darkGray);
    theme.setItemLabelPaint(Color.yellow);
    theme.setDrawingSupplier(new DefaultDrawingSupplier(
        new Paint[] {Color.decode("0xFFFF00"), Color.decode("0x0036CC"), Color.decode("0xFF0000"),
            Color.decode("0xFFFF7F"), Color.decode("0x6681CC"), Color.decode("0xFF7F7F"),
            Color.decode("0xFFFFBF"), Color.decode("0x99A6CC"), Color.decode("0xFFBFBF"),
            Color.decode("0xA9A938"), Color.decode("0x2D4587")},
        new Paint[] {Color.decode("0xFFFF00"), Color.decode("0x0036CC")},
        new Stroke[] {new BasicStroke(2.0f)}, new Stroke[] {new BasicStroke(0.5f)},
        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));
    theme.setErrorIndicatorPaint(Color.lightGray);
    theme.setGridBandPaint(new Color(255, 255, 255, 20));
    theme.setGridBandAlternatePaint(new Color(255, 255, 255, 40));

    // axis
    Color transp = new Color(255, 255, 255, 200);
    theme.setRangeGridlinePaint(transp);
    theme.setDomainGridlinePaint(transp);

    theme.setAxisLinePaint(Color.yellow);
    theme.setMasterFontColor(Color.yellow);

    // axis offset
    theme.setAxisOffset(new RectangleInsets(0, 0, 0, 0));
    return theme;
  }


  public static EStandardChartTheme getStandardTheme() {
    return createChartTheme(standardTheme);
  }

  public static void setStandardTheme(THEME theme) {
    standardTheme = theme;
  }
}
