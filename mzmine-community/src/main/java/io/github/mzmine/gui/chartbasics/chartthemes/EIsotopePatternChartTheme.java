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

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import org.jfree.chart.ChartColor;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.XYPlot;

import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeFactory.THEME;

/**
 * Chart theme used to display isotope pattern in prieviews
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class EIsotopePatternChartTheme extends EStandardChartTheme {

  protected boolean showXGrid = true, showYGrid = true;

  public EIsotopePatternChartTheme() {
    super(THEME.BNW_PRINT, "Isotope pattern this");
  }

  public void initialize() {
    // Fonts
    this.setExtraLargeFont(new Font("Arial", Font.BOLD, 16));
    this.setLargeFont(new Font("Arial", Font.BOLD, 11));
    this.setRegularFont(new Font("Arial", Font.PLAIN, 11));
    this.setSmallFont(new Font("Arial", Font.PLAIN, 11));

    // paints
    this.setTitlePaint(Color.black);
    this.setSubtitlePaint(Color.black);
    this.setLegendItemPaint(Color.black);
    this.setPlotOutlinePaint(Color.black);
    this.setBaselinePaint(Color.black);
    this.setCrosshairPaint(Color.black);
    this.setLabelLinkPaint(Color.black);
    this.setTickLabelPaint(Color.black);
    this.setAxisLabelPaint(Color.black);
    this.setShadowPaint(Color.black);
    this.setItemLabelPaint(Color.black);

    this.setLegendBackgroundPaint(Color.white);
    this.setChartBackgroundPaint(Color.white);
    this.setPlotBackgroundPaint(Color.white);

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

    this.setDrawingSupplier(
        new DefaultDrawingSupplier(colors, DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
            DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE));

    this.setRangeGridlinePaint(Color.GRAY);
    this.setDomainGridlinePaint(Color.GRAY);

    this.setAxisLinePaint(Color.black);
  }

  @Override
  public void apply(JFreeChart chart) {
    super.apply(chart);
    if (chart.getPlot() instanceof XYPlot) {
      chart.getXYPlot().setDomainGridlinesVisible(true);
      chart.getXYPlot().setRangeGridlinesVisible(true);
      chart.getXYPlot().getRenderer().setSeriesPaint(0, Color.red);
      chart.getXYPlot().getRenderer().setSeriesPaint(0, Color.green);
    }
  }
}
