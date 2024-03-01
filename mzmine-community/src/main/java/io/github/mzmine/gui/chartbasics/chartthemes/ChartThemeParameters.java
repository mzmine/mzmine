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


import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.FontParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxColorUtil;
import io.github.mzmine.util.javafx.FxFontUtil;
import java.awt.BasicStroke;
import java.text.DecimalFormat;
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

  public static final BooleanParameter showTitle = new BooleanParameter("Show title", "", true);
  public static final OptionalParameter<StringParameter> changeTitle = new OptionalParameter<>(
      new StringParameter("Change title", "", ""));
  public static final BooleanParameter showSubtitles = new BooleanParameter("Show subtitle", "",
      true);
  public static final BooleanParameter showLegends = new BooleanParameter("Show legends", "", true);

  public static final OptionalParameter<StringParameter> xlabel = new OptionalParameter<>(
      new StringParameter("Change x", "", "x"));
  public static final OptionalParameter<StringParameter> ylabel = new OptionalParameter<>(
      new StringParameter("Change y", "", "y"));

  public static final ColorParameter color = new ColorParameter("Background", "Background color",
      Color.WHITE);

  public static final FontParameter masterFont = new FontParameter("Master",
      "Master font changes all fonts",
      new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.NORMAL, 11.0)));
  public static final FontParameter titleFont = new FontParameter("Title", "Title font",
      new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.BOLD, 11.0)));
  public static final FontParameter subTitleFont = new FontParameter("Subtitles", "Subtitle font",
      new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.BOLD, 11.0)));
  public static final FontParameter axisLabelFont = new FontParameter("Axis Labels",
      "Axis label font", new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.NORMAL, 9.0)));
  public static final FontParameter itemLabelFont = new FontParameter("Item Labels",
      "Item label font", new FontSpecs(Color.BLACK, Font.font("Arial", FontWeight.NORMAL, 9.0)));

  public static final OptionalParameter<ColorParameter> xGridPaint = new OptionalParameter<>(
      new ColorParameter("X grid", "Enable/Disable the x grid and set the line color",
          Color.BLACK));
  public static final OptionalParameter<ColorParameter> yGridPaint = new OptionalParameter<>(
      new ColorParameter("Y grid", "Enable/Disable the y grid and set the line color",
          Color.BLACK));

  public static final BooleanParameter showXAxis = new BooleanParameter("Show x axis", "", true);
  public static final BooleanParameter showYAxis = new BooleanParameter("Show y axis", "", true);


  public static final DoubleParameter dataLineWidth = new DoubleParameter("Data line width",
      "The line width is used by some but not all charts, e.g., the TIC and spectra plots.",
      new DecimalFormat("0.00"), 1d, 0.00001, 1000d);

  public ChartThemeParameters() {
    super(new Parameter[]{
        // chart specific - e.g., for export
        showTitle, changeTitle, showSubtitles, showLegends, xlabel, ylabel,
        // general
        dataLineWidth, color, masterFont, titleFont, subTitleFont, axisLabelFont, itemLabelFont,
        xGridPaint, yGridPaint, showXAxis, showYAxis});
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

  public void applyToChartTheme(EStandardChartTheme theme) {
    // apply chart settings
    boolean showTitle = this.getParameter(ChartThemeParameters.showTitle).getValue();
    boolean showSubtitles = this.getParameter(ChartThemeParameters.showSubtitles).getValue();
    boolean showLegends = this.getParameter(ChartThemeParameters.showLegends).getValue();
    boolean showXAxis = this.getParameter(ChartThemeParameters.showXAxis).getValue();
    boolean showYAxis = this.getParameter(ChartThemeParameters.showYAxis).getValue();
    boolean xgrid = this.getParameter(ChartThemeParameters.xGridPaint).getValue();
    boolean ygrid = this.getParameter(ChartThemeParameters.yGridPaint).getValue();
    boolean changeTitle = this.getParameter(ChartThemeParameters.changeTitle).getValue();
    String newTitle = this.getParameter(ChartThemeParameters.changeTitle).getEmbeddedParameter()
        .getValue();

    Color cxgrid = this.getParameter(ChartThemeParameters.xGridPaint).getEmbeddedParameter()
        .getValue();
    Color cygrid = this.getParameter(ChartThemeParameters.yGridPaint).getEmbeddedParameter()
        .getValue();
    boolean usexlabel = this.getParameter(ChartThemeParameters.xlabel).getValue();
    boolean useylabel = this.getParameter(ChartThemeParameters.ylabel).getValue();
    String xlabel = this.getParameter(ChartThemeParameters.xlabel).getEmbeddedParameter()
        .getValue();
    String ylabel = this.getParameter(ChartThemeParameters.ylabel).getEmbeddedParameter()
        .getValue();
    FontSpecs master = this.getParameter(ChartThemeParameters.masterFont).getValue();
    FontSpecs titleFont = this.getParameter(ChartThemeParameters.titleFont).getValue();
    FontSpecs subtitleFont = this.getParameter(ChartThemeParameters.subTitleFont).getValue();
    FontSpecs axisLabels = this.getParameter(ChartThemeParameters.axisLabelFont).getValue();
    FontSpecs itemLabels = this.getParameter(ChartThemeParameters.itemLabelFont).getValue();
    Color bgColor = this.getParameter(ChartThemeParameters.color).getValue();
    double dataLineWidth = this.getValue(ChartThemeParameters.dataLineWidth);

    theme.setShowTitle(showTitle);
    theme.getShowSubtitles(showSubtitles);
    theme.setShowLegend(showLegends);
    theme.setTitle(newTitle);
    theme.setChangeTitle(changeTitle);
    theme.setChartBackgroundPaint(FxColorUtil.fxColorToAWT(bgColor));
    theme.setPlotBackgroundPaint(FxColorUtil.fxColorToAWT(bgColor));

    theme.setMasterFont(FxFontUtil.fxFontToAWT(master.getFont()));
    theme.setExtraLargeFont(FxFontUtil.fxFontToAWT(titleFont.getFont()));
    theme.setLargeFont(FxFontUtil.fxFontToAWT(subtitleFont.getFont()));
    theme.setRegularFont(FxFontUtil.fxFontToAWT(axisLabels.getFont()));
    theme.setSmallFont(FxFontUtil.fxFontToAWT(axisLabels.getFont()));
    theme.setItemLabelFont(FxFontUtil.fxFontToAWT(itemLabels.getFont()));

    theme.setMasterFontColor(FxColorUtil.fxColorToAWT(master.getColor()));
    theme.setAxisLabelPaint(FxColorUtil.fxColorToAWT(subtitleFont.getColor()));
    theme.setTickLabelPaint(FxColorUtil.fxColorToAWT(axisLabels.getColor()));
    theme.setTitlePaint(FxColorUtil.fxColorToAWT(titleFont.getColor()));
    theme.setItemLabelPaint(FxColorUtil.fxColorToAWT(axisLabels.getColor()));
    theme.setLegendItemPaint(FxColorUtil.fxColorToAWT(subtitleFont.getColor()));
    theme.setAxisLinePaint(FxColorUtil.fxColorToAWT(subtitleFont.getColor()));
    theme.setSubtitlePaint(FxColorUtil.fxColorToAWT(titleFont.getColor()));

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

    theme.setDefaultDataStroke(new BasicStroke((float) dataLineWidth));
  }
}
