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


import org.jfree.chart.JFreeChart;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.FontParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxFontUtil;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * JFreeChart theme settings for {@link EStandardChartTheme}s
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class ChartThemeParameters extends SimpleParameterSet {

  public static final BooleanParameter showTitle = new BooleanParameter("Show title", "", false);
  public static final OptionalParameter<StringParameter> changeTitle =
      new OptionalParameter<StringParameter>(new StringParameter("Change title", "", ""));
  public static final BooleanParameter showLegends =
      new BooleanParameter("Show legends", "", false);

  public static final OptionalParameter<StringParameter> xlabel =
      new OptionalParameter<StringParameter>(new StringParameter("Change x", "", "x"));
  public static final OptionalParameter<StringParameter> ylabel =
      new OptionalParameter<StringParameter>(new StringParameter("Change y", "", "y"));

  public static final ColorParameter color =
      new ColorParameter("Background", "Background color", Color.WHITE);

  public static final FontParameter masterFont =
      new FontParameter("Master", "Master font changes all fonts",
          new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.NORMAL, 11.0)));
  public static final FontParameter titleFont = new FontParameter("Title", "Title font",
      new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.BOLD, 11.0)));
  public static final FontParameter captionFont = new FontParameter("Captions", "Caption font",
      new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.BOLD, 11.0)));
  public static final FontParameter labelFont = new FontParameter("Labels", "Label font",
      new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.NORMAL, 9.0)));

  public static final OptionalParameter<ColorParameter> xGridPaint =
      new OptionalParameter<ColorParameter>(new ColorParameter("X grid",
          "Enable/Disable the x grid and set the line color", Color.BLACK));
  public static final OptionalParameter<ColorParameter> yGridPaint =
      new OptionalParameter<ColorParameter>(new ColorParameter("Y grid",
          "Enable/Disable the y grid and set the line color", Color.BLACK));

  public static final BooleanParameter showXAxis = new BooleanParameter("Show x axis", "", true);
  public static final BooleanParameter showYAxis = new BooleanParameter("Show y axis", "", true);

  public ChartThemeParameters() {
    super(new Parameter[]{showLegends, showTitle, changeTitle, xlabel, ylabel, color, masterFont,
        titleFont, captionFont, labelFont, xGridPaint, yGridPaint, showXAxis, showYAxis});
    changeTitle.setValue(false);
    xlabel.setValue(false);
    ylabel.setValue(false);
    xGridPaint.setValue(false);
    yGridPaint.setValue(false);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {

    assert Platform.isFxApplicationThread();

    if ((parameters == null) || (parameters.length == 0)) {
      return ExitCode.OK;
    }
    ParameterSetupDialog dialog = new ChartThemeParametersSetupDialog(valueCheckRequired, this);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Deprecated
  public void applyToChart(JFreeChart chart) {
    // // apply chart settings
    // boolean showTitle = this.getParameter(ChartThemeParameters.showTitle).getValue();
    // boolean changeTitle = this.getParameter(ChartThemeParameters.changeTitle).getValue();
    // String title =
    // this.getParameter(ChartThemeParameters.changeTitle).getEmbeddedParameter().getValue();
    // boolean showLegends = this.getParameter(ChartThemeParameters.showLegends).getValue();
    //
    // boolean usexlabel = this.getParameter(ChartThemeParameters.xlabel).getValue();
    // boolean useylabel = this.getParameter(ChartThemeParameters.ylabel).getValue();
    // String xlabel =
    // this.getParameter(ChartThemeParameters.xlabel).getEmbeddedParameter().getValue();
    // String ylabel =
    // this.getParameter(ChartThemeParameters.ylabel).getEmbeddedParameter().getValue();
    //
    // Color gbColor = this.getParameter(ChartThemeParameters.color).getValue();
    // // chart.setBackgroundPaint(gbColor);
    // // chart.getPlot().setBackgroundPaint(gbColor);
    //
    // if (changeTitle)
    // chart.setTitle(title);
    // chart.getTitle().setVisible(showTitle);
    // ((List<Title>) chart.getSubtitles()).stream().forEach(t -> t.setVisible(showLegends));
    //
    // if (chart.getXYPlot() != null) {
    // XYPlot p = chart.getXYPlot();
    // if (usexlabel)
    // p.getDomainAxis().setLabel(xlabel);
    // if (useylabel)
    // p.getRangeAxis().setLabel(ylabel);
    //
    // boolean xgrid = this.getParameter(ChartThemeParameters.xGridPaint).getValue();
    // boolean ygrid = this.getParameter(ChartThemeParameters.yGridPaint).getValue();
    // Color cxgrid =
    // this.getParameter(ChartThemeParameters.xGridPaint).getEmbeddedParameter().getValue();
    // Color cygrid =
    // this.getParameter(ChartThemeParameters.yGridPaint).getEmbeddedParameter().getValue();
    // p.setDomainGridlinesVisible(xgrid);
    // p.setDomainGridlinePaint(FxColorUtil.fxColorToAWT(cxgrid));
    // p.setRangeGridlinesVisible(ygrid);
    // p.setRangeGridlinePaint(FxColorUtil.fxColorToAWT(cygrid));
    //
    // p.getDomainAxis().setVisible(this.getParameter(ChartThemeParameters.showXAxis).getValue());
    // p.getRangeAxis().setVisible(this.getParameter(ChartThemeParameters.showYAxis).getValue());
    // }
  }

  public void applyToChartTheme(EStandardChartTheme theme) {
    // apply chart settings
    boolean showTitle = this.getParameter(ChartThemeParameters.showTitle).getValue();
    boolean showLegends = this.getParameter(ChartThemeParameters.showLegends).getValue();
    boolean showXAxis = this.getParameter(ChartThemeParameters.showXAxis).getValue();
    boolean showYAxis = this.getParameter(ChartThemeParameters.showYAxis).getValue();
    boolean xgrid = this.getParameter(ChartThemeParameters.xGridPaint).getValue();
    boolean ygrid = this.getParameter(ChartThemeParameters.yGridPaint).getValue();
    Color cxgrid =
        this.getParameter(ChartThemeParameters.xGridPaint).getEmbeddedParameter().getValue();
    Color cygrid =
        this.getParameter(ChartThemeParameters.yGridPaint).getEmbeddedParameter().getValue();
    boolean usexlabel = this.getParameter(ChartThemeParameters.xlabel).getValue();
    boolean useylabel = this.getParameter(ChartThemeParameters.ylabel).getValue();
    String xlabel =
        this.getParameter(ChartThemeParameters.xlabel).getEmbeddedParameter().getValue();
    String ylabel =
        this.getParameter(ChartThemeParameters.ylabel).getEmbeddedParameter().getValue();
    FontSpecs master = this.getParameter(ChartThemeParameters.masterFont).getValue();
    FontSpecs titleFont = this.getParameter(ChartThemeParameters.titleFont).getValue();
    FontSpecs captionFont = this.getParameter(ChartThemeParameters.captionFont).getValue();
    FontSpecs labelFont = this.getParameter(ChartThemeParameters.labelFont).getValue();
    Color bgColor = this.getParameter(ChartThemeParameters.color).getValue();

    theme.setShowTitle(showTitle);
    theme.getShowSubtitles(showTitle);
    theme.setChartBackgroundPaint(FxColorUtil.fxColorToAWT(bgColor));
    theme.setPlotBackgroundPaint(FxColorUtil.fxColorToAWT(bgColor));

    theme.setMasterFont(FxFontUtil.fxFontToAWT(master.getFont()));
    theme.setExtraLargeFont(FxFontUtil.fxFontToAWT(titleFont.getFont()));
    theme.setLargeFont(FxFontUtil.fxFontToAWT(captionFont.getFont()));
    theme.setRegularFont(FxFontUtil.fxFontToAWT(labelFont.getFont()));
    theme.setSmallFont(FxFontUtil.fxFontToAWT(labelFont.getFont()));

    theme.setMasterFontColor(FxColorUtil.fxColorToAWT(master.getColor()));
    theme.setAxisLabelPaint(FxColorUtil.fxColorToAWT(captionFont.getColor()));
    theme.setTickLabelPaint(FxColorUtil.fxColorToAWT(labelFont.getColor()));
    theme.setTitlePaint(FxColorUtil.fxColorToAWT(titleFont.getColor()));
    theme.setItemLabelPaint(FxColorUtil.fxColorToAWT(labelFont.getColor()));
    theme.setLegendItemPaint(FxColorUtil.fxColorToAWT(captionFont.getColor()));
    theme.setAxisLinePaint(FxColorUtil.fxColorToAWT(captionFont.getColor()));

    theme.setShowXAxis(showXAxis);
    theme.setShowYAxis(showYAxis);
    theme.setShowXGrid(xgrid);
    theme.setShowYGrid(ygrid);
    theme.setDomainGridlinePaint(FxColorUtil.fxColorToAWT(cxgrid));
    theme.setRangeGridlinePaint(FxColorUtil.fxColorToAWT(cygrid));

    theme.setUseXLabel(usexlabel);
    theme.setUseYLabel(useylabel);
    theme.setXlabel(xlabel);
    theme.setYlabel(ylabel);
    theme.setClrXGrid(FxColorUtil.fxColorToAWT(cxgrid));
    theme.setClrYGrid(FxColorUtil.fxColorToAWT(cygrid));
  }
}
