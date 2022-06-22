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
package io.github.mzmine.gui.chartbasics.chartthemes;

import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeFactory.THEME;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleChart;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotRenderer;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.PeakRenderer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Stroke;
import java.io.Serial;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
/*
import io.github.mzmine.util.MirrorChartFactory;
*/

/**
 * More options for the StandardChartTheme
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class EStandardChartTheme extends StandardChartTheme {

  public static final Logger logger = Logger.getLogger(EStandardChartTheme.class.getName());
  public static final String XML_DESC = "ChartTheme";
  @Serial
  private static final long serialVersionUID = 1L;
  private static final Color DEFAULT_GRID_COLOR = Color.BLACK;
  private static final Color DEFAULT_CROSS_HAIR_COLOR = Color.BLACK;
  private static final boolean DEFAULT_CROSS_HAIR_VISIBLE = true;
  private static final Stroke DEFAULT_CROSS_HAIR_STROKE = new BasicStroke(1.0F,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[]{5.0F, 3.0F}, 0.0F);
  // master font
  protected Font masterFont;
  protected Color masterFontColor;
  // Chart appearance
  protected boolean isAntiAliased = true;
  protected boolean showTitle = false;
  protected boolean showLegend = true;
  // orientation : 0 - 2 (90 CW)
  protected boolean showSubtitle = true;
  protected boolean changeTitle = false;
  protected String title = "";
  protected Paint axisLinePaint = Color.black;
  protected boolean showXGrid = false, showYGrid = false;
  protected boolean showXAxis = true, showYAxis = true;
  // protected THEME themeID;
  protected boolean useXLabel, useYLabel;
  protected String xlabel, ylabel;
  protected Color clrXGrid, clrYGrid;
  // not final because we want themes without offsets for the export.
  private RectangleInsets DEFAULT_AXIS_OFFSET = new RectangleInsets(0, 0, 0, 0);
  private RectangleInsets MIRROR_PLOT_AXIS_OFFSET = new RectangleInsets(0, 4, 0, 4);
  private Font itemLabelFont;
  private BasicStroke defaultDataStroke;


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

  /**
   * Fixes the legend item's colour after the colours of the datasets/series in the plot were
   * changed.
   *
   * @param chart The chart.
   */
  public static void fixLegend(JFreeChart chart) {
    Plot plot = chart.getPlot();
    LegendTitle oldLegend = chart.getLegend();
    if (oldLegend == null) {
      return;
    }

    RectangleEdge pos = oldLegend.getPosition();
    chart.removeLegend();

    LegendTitle newLegend;

    // TODO:
    /*
    if (plot instanceof CombinedDomainXYPlot && (oldLegend.getSources()[0].getLegendItems()
        .getItemCount() == MirrorChartFactory.tags.length
        || oldLegend.getSources()[0].getLegendItems()
            .getItemCount() == MirrorChartFactory.tags.length * 2)) {

      newLegend = MirrorChartFactory.createLibraryMatchingLegend((CombinedDomainXYPlot) plot);
    } else {
      newLegend = new LegendTitle(plot);
    }
    */
    // REMOVE
    newLegend = new LegendTitle(plot);
    // REMOVE

    newLegend.setPosition(pos);
    newLegend.setItemFont(oldLegend.getItemFont());
    newLegend.setItemPaint(oldLegend.getItemPaint());
    chart.addLegend(newLegend);
    newLegend.setVisible(oldLegend.isVisible());
    newLegend.setFrame(BlockBorder.NONE);
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
  public void apply(@NotNull JFreeChart chart) {
    final boolean oldNotify = chart.isNotify();
    chart.setNotify(false);

    super.apply(chart);
    Plot p = chart.getPlot();

    // Cross hair and axis visibility colors
    applyToCrosshair(chart);
    applyToAxes(chart);

    // apply bg
    chart.setBackgroundPaint(this.getChartBackgroundPaint());
    chart.getPlot().setBackgroundPaint(this.getPlotBackgroundPaint());

    //
    chart.setAntiAlias(isAntiAliased());
    p.setBackgroundAlpha(isNoBackground() ? 0 : 1);

    applyToTitles(chart);
    applyToLegend(chart);

    // to get the correct font specified in this theme by the item label font, we need to reapply
    // it. (the normal theme sets the default font, too)
    final XYPlot plot = chart.getXYPlot();
    if (plot != null) {
      int rendererCount = plot.getRendererCount();
      for (int i = 0; i < rendererCount; i++) {
        XYItemRenderer r = plot.getRenderer(i);
        if (r instanceof AbstractRenderer renderer) {
          applyToAbstractRenderer(renderer);
        }
      }
    }

    chart.setNotify(oldNotify);
    if (oldNotify) {
      chart.fireChartChanged();
    }
  }

  public void apply(@NotNull EChartViewer chartViewer) {
    apply(chartViewer.getChart());

    if (chartViewer instanceof SimpleChart simpleChart) {
      simpleChart.setLegendItemsVisible(isShowLegend());
    }
  }

  public void applyToCrosshair(@NotNull JFreeChart chart) {
    Plot p = chart.getPlot();
    if (p instanceof XYPlot xyp) {
      xyp.setDomainCrosshairPaint(DEFAULT_CROSS_HAIR_COLOR);
      xyp.setRangeCrosshairPaint(DEFAULT_CROSS_HAIR_COLOR);
      xyp.setDomainCrosshairStroke(DEFAULT_CROSS_HAIR_STROKE);
      xyp.setRangeCrosshairStroke(DEFAULT_CROSS_HAIR_STROKE);
      xyp.setDomainCrosshairVisible(DEFAULT_CROSS_HAIR_VISIBLE);
      xyp.setRangeCrosshairVisible(DEFAULT_CROSS_HAIR_VISIBLE);
    }
  }

  public void applyToAxes(@NotNull JFreeChart chart) {
    Plot p = chart.getPlot();

    // Only apply to XYPlot
    if (!(p instanceof XYPlot xyp)) {
      return;
    }

    Axis domainAxis = xyp.getDomainAxis();
    Axis rangeAxis = xyp.getRangeAxis();

    xyp.setRangeGridlinesVisible(isShowYGrid());
    xyp.setRangeGridlinePaint(getClrYGrid());
    xyp.setDomainGridlinesVisible(isShowXGrid());
    xyp.setDomainGridlinePaint(getClrXGrid());
    xyp.setAxisOffset(DEFAULT_AXIS_OFFSET);

    // only apply labels to the main axes
    if (domainAxis != null && isUseXLabel()) {
      domainAxis.setLabel(getXlabel());
    }
    if (rangeAxis != null && isUseYLabel()) {
      rangeAxis.setLabel(getYlabel());
    }

    // all axes
    for (int i = 0; i < xyp.getDomainAxisCount(); i++) {
      NumberAxis a = (NumberAxis) xyp.getDomainAxis(i);
      if (a == null) {
        continue;
      }
      a.setTickMarkPaint(axisLinePaint);
      a.setAxisLinePaint(axisLinePaint);
      // visible?
      a.setVisible(showXAxis);
    }
    for (int i = 0; i < xyp.getRangeAxisCount(); i++) {
      NumberAxis a = (NumberAxis) xyp.getRangeAxis(i);
      if (a == null) {
        continue;
      }
      a.setTickMarkPaint(axisLinePaint);
      a.setAxisLinePaint(axisLinePaint);
      // visible?
      a.setVisible(showYAxis);
    }

    // mirror plots (CombinedDomainXYPlot) have subplots with their own range axes
    if (p instanceof CombinedDomainXYPlot mirrorPlot) {
      mirrorPlot.setGap(0);
      mirrorPlot.setAxisOffset(MIRROR_PLOT_AXIS_OFFSET);
      for (XYPlot subplot : (List<XYPlot>) mirrorPlot.getSubplots()) {
        Axis ra = subplot.getRangeAxis();
        subplot.setAxisOffset(MIRROR_PLOT_AXIS_OFFSET);
        if (rangeAxis != null) {
          ra.setVisible(isShowYAxis());
          subplot.setRangeGridlinesVisible(isShowYGrid());
          subplot.setRangeGridlinePaint(getClrYGrid());
          subplot.setDomainGridlinesVisible(isShowXGrid());
          subplot.setDomainGridlinePaint(getClrXGrid());
          if (isUseYLabel()) {
            ra.setLabel(getYlabel());
          }
        }
      }
    }
  }

  public void applyToLegend(@NotNull JFreeChart chart) {

    if (chart.getLegend() != null) {
      chart.getLegend().setBackgroundPaint(this.getChartBackgroundPaint());
    }

    fixLegend(chart);
  }

  public void applyToTitles(@NotNull JFreeChart chart) {
    TextTitle title = chart.getTitle();
    if (title != null) {
      title.setVisible(isShowTitle());
      if (isChangeTitle()) {
        title.setText(getTitle());
      }
    }

    chart.getSubtitles().forEach(s -> {
      if (s != chart.getTitle() && s instanceof TextTitle textTitle) {
        // ((TextTitle) s).setFont(getRegularFont());
        // ((TextTitle) s).setMargin(TITLE_TOP_MARGIN, 0d, 0d, 0d);
        textTitle.setVisible(isShowSubtitles());
        // ((TextTitle) s).setPaint(subtitleFontColor); // should be set by the theme itself.
        // subtitle color is set by the chart theme parameters

        // if (PaintScaleLegend.class.isAssignableFrom(s.getClass())) {
        // ((PaintScaleLegend) s)
        // .setBackgroundPaint(this.getChartBackgroundPaint());
        // }
      }
      if (s instanceof LegendTitle legendTitle) {
        legendTitle.setVisible(isShowLegend());
      }
    });
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

  @Override
  public void applyToAbstractRenderer(AbstractRenderer renderer) {
    super.applyToAbstractRenderer(renderer);
    // apply to all
    if (itemLabelFont != null) {
      renderer.setDefaultItemLabelFont(itemLabelFont);
    }
    // only apply to those renderers that we know should behave like this
    if (renderer instanceof PeakRenderer || renderer instanceof ColoredXYLineRenderer
        || renderer instanceof TICPlotRenderer) {
      renderer.setAutoPopulateSeriesStroke(false);
      renderer.setAutoPopulateSeriesOutlineStroke(false);
      renderer.setDefaultStroke(defaultDataStroke);
      renderer.setDefaultOutlineStroke(defaultDataStroke);
    }
  }

  // GETTERS AND SETTERS
  public boolean isShowLegend() {
    return showLegend;
  }

  public void setShowLegend(boolean showLegend) {
    this.showLegend = showLegend;
  }

  public boolean isShowSubtitle() {
    return showSubtitle;
  }

  public void setShowSubtitle(boolean showSubtitle) {
    this.showSubtitle = showSubtitle;
  }

  public Paint getAxisLinePaint() {
    return axisLinePaint;
  }

  public void setAxisLinePaint(Paint axisLinePaint) {
    this.axisLinePaint = axisLinePaint;
  }

  public boolean isShowTitle() {
    return showTitle;
  }

  public void setShowTitle(boolean showTitle) {
    this.showTitle = showTitle;
  }

  public boolean isAntiAliased() {
    return isAntiAliased;
  }

  public void setAntiAliased(boolean isAntiAliased) {
    this.isAntiAliased = isAntiAliased;
  }

  public boolean isShowXGrid() {
    return showXGrid;
  }

  public void setShowXGrid(boolean showXGrid) {
    this.showXGrid = showXGrid;
  }

  public boolean isShowYGrid() {
    return showYGrid;
  }

  public void setShowYGrid(boolean showYGrid) {
    this.showYGrid = showYGrid;
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

  public void setMasterFont(Font masterFont) {
    this.masterFont = masterFont;
  }

  public Color getMasterFontColor() {
    return masterFontColor;
  }

  public void setMasterFontColor(Color masterFontColor) {
    this.masterFontColor = masterFontColor;
  }

  public void getShowSubtitles(boolean subtitleVisible) {
    this.showSubtitle = subtitleVisible;
  }

  public boolean isShowSubtitles() {
    return showSubtitle;
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

  public RectangleInsets getDefaultAxisOffset() {
    return DEFAULT_AXIS_OFFSET;
  }

  /**
   * Should be set to 0 for exports
   *
   * @param defaultAxisOffset
   */
  public void setDefaultAxisOffset(RectangleInsets defaultAxisOffset) {
    DEFAULT_AXIS_OFFSET = defaultAxisOffset;
  }


  public RectangleInsets getMirrorPlotAxisOffset() {
    return MIRROR_PLOT_AXIS_OFFSET;
  }

  public void setMirrorPlotAxisOffset(RectangleInsets mirrorPlotAxisOffset) {
    MIRROR_PLOT_AXIS_OFFSET = mirrorPlotAxisOffset;
  }

  public boolean isChangeTitle() {
    return changeTitle;
  }

  public void setChangeTitle(boolean changeTitle) {
    this.changeTitle = changeTitle;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Font getItemLabelFont() {
    return this.itemLabelFont;
  }

  public void setItemLabelFont(Font font) {
    this.itemLabelFont = font;
  }

  /**
   * The default stroke used by many charts like the {@link TICPlot}
   *
   * @return a basic stroke with the width set by user
   */
  public BasicStroke getDefaultDataStroke() {
    return defaultDataStroke;
  }

  public void setDefaultDataStroke(BasicStroke defaultDataStroke) {
    this.defaultDataStroke = defaultDataStroke;
  }
}
