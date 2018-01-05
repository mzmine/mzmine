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

package net.sf.mzmine.chartbasics;

import java.awt.Color;
import java.awt.Font;
import java.util.List;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.Title;
import net.sf.mzmine.chartbasics.chartthemes.EStandardChartTheme;
import net.sf.mzmine.framework.fontspecs.FontSpecs;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ColorParameter;
import net.sf.mzmine.parameters.parametertypes.FontParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

/**
 * JFreeChart theme settings for {@link EStandardChartTheme}s
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartParameters extends SimpleParameterSet {

  public static final BooleanParameter showTitle = new BooleanParameter("Show title", "", false);
  public static final OptionalParameter<StringParameter> changeTitle =
      new OptionalParameter<StringParameter>(new StringParameter("Change title", "", ""));
  public static final BooleanParameter showLegends =
      new BooleanParameter("Show legends", "", false);

  public static final OptionalParameter<StringParameter> xlabel =
      new OptionalParameter<StringParameter>(new StringParameter("Change x", "", "x"));
  public static final OptionalParameter<StringParameter> ylabel =
      new OptionalParameter<StringParameter>(new StringParameter("Change y", "", "y"));


  public static final FontParameter masterFont =
      new FontParameter("Master", "Master font changes all fonts",
          new FontSpecs(Color.BLACK, new Font("Arial", Font.PLAIN, 11)));
  public static final FontParameter titleFont = new FontParameter("Title", "Title font",
      new FontSpecs(Color.BLACK, new Font("Arial", Font.BOLD, 11)));
  public static final FontParameter captionFont = new FontParameter("Captions", "Caption font",
      new FontSpecs(Color.BLACK, new Font("Arial", Font.BOLD, 11)));
  public static final FontParameter labelFont = new FontParameter("Labels", "Label font",
      new FontSpecs(Color.BLACK, new Font("Arial", Font.PLAIN, 9)));

  public static final OptionalParameter<ColorParameter> xGridPaint =
      new OptionalParameter<ColorParameter>(new ColorParameter("X grid",
          "Enable/Disable the x grid and set the line color", Color.black));
  public static final OptionalParameter<ColorParameter> yGridPaint =
      new OptionalParameter<ColorParameter>(new ColorParameter("Y grid",
          "Enable/Disable the y grid and set the line color", Color.black));

  public static final BooleanParameter showXAxis = new BooleanParameter("Show x axis", "", true);
  public static final BooleanParameter showYAxis = new BooleanParameter("Show y axis", "", true);


  public ChartParameters() {
    super(new Parameter[] {showLegends, showTitle, changeTitle, xlabel, ylabel, masterFont,
        titleFont, captionFont, labelFont, xGridPaint, yGridPaint, showXAxis, showYAxis});
    changeTitle.setValue(false);
    xlabel.setValue(false);
    ylabel.setValue(false);
    xGridPaint.setValue(false);
    yGridPaint.setValue(false);
  }


  public void applyToChart(JFreeChart chart) {
    // apply chart settings
    boolean showTitle = this.getParameter(ChartParameters.showTitle).getValue();
    boolean changeTitle = this.getParameter(ChartParameters.changeTitle).getValue();
    String title = this.getParameter(ChartParameters.changeTitle).getEmbeddedParameter().getValue();
    boolean showLegends = this.getParameter(ChartParameters.showLegends).getValue();

    boolean usexlabel = this.getParameter(ChartParameters.xlabel).getValue();
    boolean useylabel = this.getParameter(ChartParameters.ylabel).getValue();
    String xlabel = this.getParameter(ChartParameters.xlabel).getEmbeddedParameter().getValue();
    String ylabel = this.getParameter(ChartParameters.ylabel).getEmbeddedParameter().getValue();

    if (changeTitle)
      chart.setTitle(title);
    chart.getTitle().setVisible(showTitle);
    ((List<Title>) chart.getSubtitles()).stream().forEach(t -> t.setVisible(showLegends));

    if (chart.getXYPlot() != null) {
      XYPlot p = chart.getXYPlot();
      if (usexlabel)
        p.getDomainAxis().setLabel(xlabel);
      if (useylabel)
        p.getRangeAxis().setLabel(ylabel);


      boolean xgrid = this.getParameter(ChartParameters.xGridPaint).getValue();
      boolean ygrid = this.getParameter(ChartParameters.yGridPaint).getValue();
      Color cxgrid =
          this.getParameter(ChartParameters.xGridPaint).getEmbeddedParameter().getValue();
      Color cygrid =
          this.getParameter(ChartParameters.yGridPaint).getEmbeddedParameter().getValue();
      p.setDomainGridlinesVisible(xgrid);
      p.setDomainGridlinePaint(cxgrid);
      p.setRangeGridlinesVisible(ygrid);
      p.setRangeGridlinePaint(cygrid);

      p.getDomainAxis().setVisible(this.getParameter(ChartParameters.showXAxis).getValue());
      p.getRangeAxis().setVisible(this.getParameter(ChartParameters.showYAxis).getValue());
    }
  }

  public void applyToChartTheme(EStandardChartTheme theme) {
    // apply chart settings
    boolean showTitle = this.getParameter(ChartParameters.showTitle).getValue();
    boolean showLegends = this.getParameter(ChartParameters.showLegends).getValue();
    boolean showXAxis = this.getParameter(ChartParameters.showXAxis).getValue();
    boolean showYAxis = this.getParameter(ChartParameters.showYAxis).getValue();
    boolean xgrid = this.getParameter(ChartParameters.xGridPaint).getValue();
    boolean ygrid = this.getParameter(ChartParameters.yGridPaint).getValue();
    Color cxgrid = this.getParameter(ChartParameters.xGridPaint).getEmbeddedParameter().getValue();
    Color cygrid = this.getParameter(ChartParameters.yGridPaint).getEmbeddedParameter().getValue();

    theme.setShowTitle(showTitle);
    theme.getShowSubtitles(showLegends);

    FontSpecs master = this.getParameter(ChartParameters.masterFont).getValue();
    FontSpecs large = this.getParameter(ChartParameters.titleFont).getValue();
    FontSpecs medium = this.getParameter(ChartParameters.captionFont).getValue();
    FontSpecs small = this.getParameter(ChartParameters.labelFont).getValue();

    theme.setMasterFont(master.getFont());
    theme.setExtraLargeFont(large.getFont());
    theme.setLargeFont(medium.getFont());
    theme.setRegularFont(small.getFont());
    theme.setSmallFont(small.getFont());

    theme.setMasterFontColor(master.getColor());
    theme.setAxisLabelPaint(medium.getColor());
    theme.setTickLabelPaint(small.getColor());
    theme.setTitlePaint(large.getColor());
    theme.setItemLabelPaint(small.getColor());
    theme.setLegendItemPaint(medium.getColor());

    theme.setAxisLinePaint(medium.getColor());

    theme.setShowXAxis(showXAxis);
    theme.setShowYAxis(showYAxis);
    theme.setShowXGrid(xgrid);
    theme.setShowYGrid(ygrid);
    theme.setDomainGridlinePaint(cxgrid);
    theme.setRangeGridlinePaint(cygrid);

  }
}
