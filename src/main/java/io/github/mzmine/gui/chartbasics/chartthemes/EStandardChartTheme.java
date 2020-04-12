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

package io.github.mzmine.gui.chartbasics.chartthemes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.util.List;
import javax.annotation.Nonnull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeFactory.THEME;

/**
 * More options for the StandardChartTheme
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class EStandardChartTheme extends StandardChartTheme {
  private static final long serialVersionUID = 1L;

  private static final Color DEFAULT_GRID_COLOR = Color.BLACK;
  private static final Color DEFAULT_CROSS_HAIR_COLOR = Color.BLACK;

  private static final boolean DEFAULT_CROSS_HAIR_VISIBLE = true;
  private static final Stroke DEFAULT_CROSS_HAIR_STROKE = new BasicStroke(1.0F,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5.0F, 3.0F}, 0.0F);

  private static final RectangleInsets DEFAULT_AXIS_OFFSET =
      new RectangleInsets(5.0, 5.0, 5.0, 5.0);
  private static final double TITLE_TOP_MARGIN = 5.0;

  public static final String XML_DESC = "ChartTheme";
  // master font
  protected Font masterFont;
  protected Color masterFontColor;

  // Chart appearance
  protected boolean isAntiAliased = true;
  // orientation : 0 - 2 (90 CW)

  protected boolean isShowTitle = false;
  protected boolean subtitleVisible = false;

  protected Paint axisLinePaint = Color.black;
  // protected THEME themeID;

  protected boolean showXGrid = false, showYGrid = false;
  protected boolean showXAxis = true, showYAxis = true;

  protected boolean useXLabel, useYLabel;
  protected String xlabel, ylabel;
  protected Color clrXGrid, clrYGrid;


  public EStandardChartTheme(String name) {
    super(name);
    // this.themeID = themeID;

    setBarPainter(new StandardBarPainter());
    setXYBarPainter(new StandardXYBarPainter());

    // in theme
    setAntiAliased(false);
    setNoBackground(false);
    // general

    isAntiAliased = true;

    masterFont = new Font("Arial", Font.PLAIN, 11);
    masterFontColor = Color.black;

    setUseXLabel(false);
    setUseYLabel(false);

    setClrYGrid(DEFAULT_GRID_COLOR);
    setClrXGrid(DEFAULT_GRID_COLOR);
  }

  public EStandardChartTheme(THEME id, String name) {
    this(name);
  }

  public void setAll(boolean antiAlias, boolean showTitle, boolean noBG, Color cBG, Color cPlotBG,
      boolean showXGrid, boolean showYGrid, boolean showXAxis, boolean showYAxis, Font fMaster,
      Color cMaster, Font fAxesT, Color cAxesT, Font fAxesL, Color cAxesL, Font fTitle,
      Color cTitle) {
    this.setAntiAliased(antiAlias);
    this.setShowTitle(showTitle);
    this.setNoBackground(noBG);
    this.setShowXGrid(showXGrid);
    this.setShowYGrid(showYGrid);
    this.setShowXAxis(showXAxis);
    this.setShowYAxis(showYAxis);
    //

    this.setExtraLargeFont(fTitle);
    this.setLargeFont(fAxesT);
    this.setRegularFont(fAxesL);
    this.setAxisLabelPaint(cAxesT);
    this.setTickLabelPaint(cAxesL);
    this.setTitlePaint(cTitle);

    this.setChartBackgroundPaint(cBG);
    this.setPlotBackgroundPaint(cPlotBG);
    this.setLegendBackgroundPaint(cBG);

    masterFont = fMaster;
    masterFontColor = cMaster;
  }

  @Override
  public void apply(@Nonnull JFreeChart chart) {
    assert chart != null;

    super.apply(chart);
    XYPlot p = chart.getXYPlot();

    // Cross hair and axis visibility colors
    applyToCrosshair(chart);
    applyToAxes(chart);

    // applyToTitles(chart);

    // apply bg
    chart.setBackgroundPaint(this.getChartBackgroundPaint());
    chart.getPlot().setBackgroundPaint(this.getPlotBackgroundPaint());

    //
    chart.setAntiAlias(isAntiAliased());
    p.setBackgroundAlpha(isNoBackground() ? 0 : 1);

    applyToLegend(chart);

  }


  public void applyToCrosshair(@Nonnull JFreeChart chart) {
    XYPlot p = chart.getXYPlot();
    p.setDomainCrosshairPaint(DEFAULT_CROSS_HAIR_COLOR);
    p.setRangeCrosshairPaint(DEFAULT_CROSS_HAIR_COLOR);
    p.setDomainCrosshairStroke(DEFAULT_CROSS_HAIR_STROKE);
    p.setRangeCrosshairStroke(DEFAULT_CROSS_HAIR_STROKE);
    p.setDomainCrosshairVisible(DEFAULT_CROSS_HAIR_VISIBLE);
    p.setRangeCrosshairVisible(DEFAULT_CROSS_HAIR_VISIBLE);
  }

  public void applyToAxes(@Nonnull JFreeChart chart) {
    XYPlot p = chart.getXYPlot();
    Axis domainAxis = p.getDomainAxis();
    Axis rangeAxis = p.getRangeAxis();

    if (domainAxis != null) {
      domainAxis.setVisible(isShowXAxis());
      p.setDomainGridlinesVisible(isShowXGrid());
      p.setDomainGridlinePaint(getClrXGrid());
      if (isUseXLabel()) {
        domainAxis.setLabel(getXlabel());
      }
    }
    if (rangeAxis != null) {
      rangeAxis.setVisible(isShowYAxis());
      p.setRangeGridlinesVisible(isShowYGrid());
      p.setRangeGridlinePaint(getClrYGrid());
      if (isUseYLabel()) {
        rangeAxis.setLabel(getYlabel());
      }
    }

    // all axes
    for (int i = 0; i < p.getDomainAxisCount(); i++) {
      NumberAxis a = (NumberAxis) p.getDomainAxis(i);
      if (a == null) {
        continue;
      }
      a.setTickMarkPaint(axisLinePaint);
      a.setAxisLinePaint(axisLinePaint);
      // visible?
      a.setVisible(showXAxis);
    }
    for (int i = 0; i < p.getRangeAxisCount(); i++) {
      NumberAxis a = (NumberAxis) p.getRangeAxis(i);
      if (a == null) {
        continue;
      }
      a.setTickMarkPaint(axisLinePaint);
      a.setAxisLinePaint(axisLinePaint);
      // visible?
      a.setVisible(showYAxis);
    }

    // mirror plots (CombinedDomainXYPlot) have subplots with their own range axes
    if (p instanceof CombinedDomainXYPlot) {
      for (XYPlot subplot : (List<XYPlot>) ((CombinedDomainXYPlot) p).getSubplots()) {
        Axis ra = subplot.getRangeAxis();
        if (rangeAxis != null) {
          ra.setVisible(isShowYAxis());
          subplot.setRangeGridlinesVisible(isShowYGrid());
          subplot.setRangeGridlinePaint(getClrYGrid());
          if (isUseYLabel()) {
            ra.setLabel(getYlabel());
          }
        }
      }
    }

    p.setAxisOffset(DEFAULT_AXIS_OFFSET);
  }

  public void applyToLegend(@Nonnull JFreeChart chart) {

    if (chart.getLegend() != null)
      chart.getLegend().setBackgroundPaint(this.getChartBackgroundPaint());

    fixLegend(chart);
  }

  // public void applyToTitles(@Nonnull JFreeChart chart){
  // chart.getSubtitles().forEach(s -> {
  // if(s instanceof TextTitle && s != chart.getTitle()){
  // ((TextTitle) s).setFont(getRegularFont());
  // ((TextTitle) s).setMargin(TITLE_TOP_MARGIN, 0d, 0d, 0d);
  // ((TextTitle) s).setVisible(isShowTitle());
  //// ((TextTitle) s).setPaint(subtitleFontColor); // should be set by the theme itself.
  //// subtitle color is set by the chart theme parameters
  //
  // if (PaintScaleLegend.class.isAssignableFrom(s.getClass())) {
  // ((PaintScaleLegend) s)
  // .setBackgroundPaint(this.getChartBackgroundPaint());
  // }
  // }
  // });
  // }

  public boolean isNoBackground() {
    return ((Color) this.getPlotBackgroundPaint()).getAlpha() == 0;
  }

  public void setNoBackground(boolean state) {
    Color c = ((Color) this.getPlotBackgroundPaint());
    Color cchart = ((Color) this.getChartBackgroundPaint());
    this.setPlotBackgroundPaint(new Color(c.getRed(), c.getGreen(), c.getBlue(), state ? 0 : 255));
    this.setChartBackgroundPaint(
        new Color(cchart.getRed(), cchart.getGreen(), cchart.getBlue(), state ? 0 : 255));
    this.setLegendBackgroundPaint(
        new Color(cchart.getRed(), cchart.getGreen(), cchart.getBlue(), state ? 0 : 255));
  }



  /**
   * Fixes the legend item's colour after the colours of the datasets/series in the plot were
   * changed.
   *
   * @param chart The chart.
   */
  public static void fixLegend(JFreeChart chart) {
    XYPlot plot = chart.getXYPlot();
    LegendTitle oldLegend = chart.getLegend();
    RectangleEdge pos = oldLegend.getPosition();
    chart.removeLegend();

    LegendTitle newLegend = new LegendTitle(plot);
    newLegend.setPosition(pos);
    newLegend.setItemFont(oldLegend.getItemFont());
    chart.addLegend(newLegend);
    newLegend.setVisible(oldLegend.isVisible());
    newLegend.setFrame(BlockBorder.NONE);
  }

  // GETTERS AND SETTERS
  public Paint getAxisLinePaint() {
    return axisLinePaint;
  }

  public boolean isShowTitle() {
    return isShowTitle;
  }

  public boolean isAntiAliased() {
    return isAntiAliased;
  }

  public void setAntiAliased(boolean isAntiAliased) {
    this.isAntiAliased = isAntiAliased;
  }

  public void setShowTitle(boolean showTitle) {
    isShowTitle = showTitle;
  }

  public void setAxisLinePaint(Paint axisLinePaint) {
    this.axisLinePaint = axisLinePaint;
  }

  // public THEME getID() {
  // return themeID;
  // }
  //
  // public void setID(THEME themeID) {
  // this.themeID = themeID;
  // }

  public void setShowXGrid(boolean showXGrid) {
    this.showXGrid = showXGrid;
  }

  public void setShowYGrid(boolean showYGrid) {
    this.showYGrid = showYGrid;
  }

  public boolean isShowXGrid() {
    return showXGrid;
  }

  public boolean isShowYGrid() {
    return showYGrid;
  }

  public boolean isShowXAxis() {
    return showXAxis;
  }

  public void setShowXAxis(boolean showXAxis) {
    this.showXAxis = showXAxis;
  }

  public boolean isShowYAxis() {
    return showYAxis;
  }

  public void setShowYAxis(boolean showYAxis) {
    this.showYAxis = showYAxis;
  }

  public Font getMasterFont() {
    return masterFont;
  }

  public Color getMasterFontColor() {
    return masterFontColor;
  }

  public void setMasterFont(Font masterFont) {
    this.masterFont = masterFont;
  }

  public void setMasterFontColor(Color masterFontColor) {
    this.masterFontColor = masterFontColor;
  }

  public void getShowSubtitles(boolean subtitleVisible) {
    this.subtitleVisible = subtitleVisible;
  }

  public boolean isShowSubtitles() {
    return subtitleVisible;
  }

  public boolean isUseXLabel() {
    return useXLabel;
  }

  public void setUseXLabel(boolean useXLabel) {
    this.useXLabel = useXLabel;
  }

  public boolean isUseYLabel() {
    return useYLabel;
  }

  public void setUseYLabel(boolean useYLabel) {
    this.useYLabel = useYLabel;
  }

  public String getXlabel() {
    return xlabel;
  }

  public void setXlabel(String xlabel) {
    this.xlabel = xlabel;
  }

  public String getYlabel() {
    return ylabel;
  }

  public void setYlabel(String ylabel) {
    this.ylabel = ylabel;
  }

  public Color getClrXGrid() {
    return clrXGrid;
  }

  public void setClrXGrid(Color clrXGrid) {
    this.clrXGrid = clrXGrid;
  }

  public Color getClrYGrid() {
    return clrYGrid;
  }

  public void setClrYGrid(Color clrYGrid) {
    this.clrYGrid = clrYGrid;
  }

}
