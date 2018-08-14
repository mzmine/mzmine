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

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.title.PaintScaleLegend;
import net.sf.mzmine.chartbasics.chartthemes.ChartThemeFactory.THEME;


/**
 * More options for the StandardChartTheme
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class EStandardChartTheme extends StandardChartTheme {
  private static final long serialVersionUID = 1L;

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
  protected THEME themeID;

  protected boolean showXGrid = false, showYGrid = false;
  protected boolean showXAxis = true, showYAxis = true;


  public EStandardChartTheme(THEME themeID, String name) {
    super(name);
    this.themeID = themeID;

    setBarPainter(new StandardBarPainter());
    setXYBarPainter(new StandardXYBarPainter());

    // in theme
    setAntiAliased(false);
    setNoBackground(false);
    // general

    isAntiAliased = true;

    masterFont = new Font("Arial", Font.PLAIN, 11);
    masterFontColor = Color.black;
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
  public void apply(JFreeChart chart) {
    // TODO Auto-generated method stub
    super.apply(chart);
    //
    chart.getXYPlot().setDomainGridlinesVisible(showXGrid);
    chart.getXYPlot().setRangeGridlinesVisible(showYGrid);
    // all axes
    for (int i = 0; i < chart.getXYPlot().getDomainAxisCount(); i++) {
      NumberAxis a = (NumberAxis) chart.getXYPlot().getDomainAxis(i);
      a.setTickMarkPaint(axisLinePaint);
      a.setAxisLinePaint(axisLinePaint);
      // visible?
      a.setVisible(showXAxis);
    }
    for (int i = 0; i < chart.getXYPlot().getRangeAxisCount(); i++) {
      NumberAxis a = (NumberAxis) chart.getXYPlot().getRangeAxis(i);
      a.setTickMarkPaint(axisLinePaint);
      a.setAxisLinePaint(axisLinePaint);
      // visible?
      a.setVisible(showYAxis);
    }
    // apply bg
    chart.setBackgroundPaint(this.getChartBackgroundPaint());
    chart.getPlot().setBackgroundPaint(this.getPlotBackgroundPaint());

    for (int i = 0; i < chart.getSubtitleCount(); i++) {
      // visible?
      chart.getSubtitle(i).setVisible(subtitleVisible);
      //
      if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
        ((PaintScaleLegend) chart.getSubtitle(i))
            .setBackgroundPaint(this.getChartBackgroundPaint());
    }
    if (chart.getLegend() != null)
      chart.getLegend().setBackgroundPaint(this.getChartBackgroundPaint());

    //
    chart.setAntiAlias(isAntiAliased());
    chart.getTitle().setVisible(isShowTitle());
    chart.getPlot().setBackgroundAlpha(isNoBackground() ? 0 : 1);
  }

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

  public THEME getID() {
    return themeID;
  }

  public void setID(THEME themeID) {
    this.themeID = themeID;
  }

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
}
