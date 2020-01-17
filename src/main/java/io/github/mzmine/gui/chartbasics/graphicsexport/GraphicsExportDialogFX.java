package io.github.mzmine.gui.chartbasics.graphicsexport;

import java.awt.Color;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeFactory;
import io.github.mzmine.gui.chartbasics.chartthemes.ChartThemeParameters;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.FontParameter;
import io.github.mzmine.parameters.parametertypes.FontSpecs;
import io.github.mzmine.parameters.parametertypes.OptionalParameterComponent;
import io.github.mzmine.util.color.Colors;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

public class GraphicsExportDialogFX extends ParameterSetupDialog {

  private static final Logger logger = Logger.getLogger(GraphicsExportDialogFX.class.getName());

  protected EStandardChartTheme theme;
  protected BorderPane pnChartPreview;
  protected JFreeChart chart;
  protected EChartViewer chartPanel;
  protected ChartThemeParameters chartParam;
  protected Color[] colors;

  private Button btnRenewPreview;
  private Button btnApply;
  private Button btnSave;


  public GraphicsExportDialogFX(boolean valueCheckRequired, ParameterSet parameterSet,
      JFreeChart chart) {
    super(valueCheckRequired, parameterSet);

    chartParam = (ChartThemeParameters) parameterSet
        .getParameter(GraphicsExportParameters.chartParameters).getValue();

    colors = Colors.getSevenColorPalette(MZmineCore.getConfiguration().getColorVision(), true);

    try {
      this.chart = (JFreeChart) chart.clone();
    } catch (CloneNotSupportedException e1) {
      e1.printStackTrace();
      logger.severe("Chart could not be cloned.");
    }

    theme = ChartThemeFactory.createBlackNWhiteTheme();
    chartParam.applyToChartTheme(theme);
    pnChartPreview = new BorderPane();

    pnChartPreview.setMinWidth(400);
    pnChartPreview.setMinHeight(300);
    mainPane.setRight(pnChartPreview);
    chartPanel = new EChartViewer(this.chart);
    pnChartPreview.setCenter(chartPanel);

    // add buttons
    btnRenewPreview = new Button("Renew Preview");
    btnRenewPreview.setOnAction(e -> renewPreview());
    pnlButtons.getButtons().add(btnRenewPreview);

    btnApply = new Button("Apply Theme");
    btnApply.setOnAction(e -> applyTheme());
    pnlButtons.getButtons().add(btnApply);

    btnSave = new Button("Save");
    btnSave.setOnAction(e -> saveGraphicsAs());
    pnlButtons.getButtons().add(btnSave);

    setMinWidth(900.0);
    setMinHeight(400.0);

    centerOnScreen();
  }

  protected void applyTheme() {
    // update param
    updateParameterSetFromComponents();
    // apply settings
    chartParam.applyToChartTheme(theme);
    chartParam.applyToChart(chartPanel.getChart());
    setStandardColors();
    theme.apply(chartPanel.getChart());
    disableCrosshair();
    // renewPreview();
  }

  protected void disableCrosshair() {
    chart.getXYPlot().setRangeCrosshairVisible(false);
    chart.getXYPlot().setDomainCrosshairVisible(false);
  }

  protected void setStandardColors() {
    DrawingSupplier ds = new DefaultDrawingSupplier(colors, colors, colors,
        DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
        DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
    
    theme.setDrawingSupplier(ds);
  }


  /**
   * renew chart preview with specified size
   */
  protected void renewPreview() {
    // set dimensions to chartpanel
    try {
      // update param
      updateParameterSetFromComponents();

      GraphicsExportParameters parameterSet = (GraphicsExportParameters) this.parameterSet;
      //
      if (parameterSet.isUseOnlyWidth()) {
        double height = (ChartLogicsFX.calcHeightToWidth((ChartViewer) chartPanel,
            parameterSet.getWidthPixel()/* , false */));

        DoubleParameter p =
            parameterSet.getParameter(GraphicsExportParameters.height).getEmbeddedParameter();
        DoubleComponent c =
            ((OptionalParameterComponent<DoubleComponent>) parametersAndComponents.get(p.getName()))
                .getEmbeddedComponent();
        p.setValueToComponent(c, height);
        p.setValueFromComponent(c);

        chartPanel.setMinSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setPrefSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setMaxSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        // getPnChartPreview().repaint(); TODO
      } else {
        chartPanel.setMinSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setPrefSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        chartPanel.setMaxSize((int) parameterSet.getWidthPixel(),
            (int) parameterSet.getHeightPixel());
        // chartPanel.repaint(); TODO
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      logger.log(Level.SEVERE, "Error while renewing preview of graphics export dialog ", ex);
    }
  }

  protected void saveGraphicsAs() {
    updateParameterSetFromComponents();
    //
    GraphicsExportParameters parameterSet = (GraphicsExportParameters) this.parameterSet;

    if (parameterSet.checkParameterValues(null)) {
      File path = parameterSet.getFullpath();
      try {
        logger.info("Writing image to file: " + path.getAbsolutePath());
        ChartExportUtil.writeChartToImageFX(chartPanel, parameterSet);
        logger.info("Success" + path);
      } catch (Exception e) {
        e.printStackTrace();
        logger.log(Level.SEVERE, "File not written (" + path + ")", e);
        // DialogLoggerUtil.showErrorDialog(this, "File not written. ", e); TODO
      }
    }
  }

  /**
   * changes the components of all fonts to the master font
   *
   * @param font
   */
  private void handleMasterFontChanged(FontSpecs font) {
    String master = ChartThemeParameters.masterFont.getName();
    for (Parameter<?> p : chartParam.getParameters()) {
      if (!(p instanceof FontParameter) || master.equals(p.getName()))
        continue;
      FontParameter up = (FontParameter) p;
//      FontSpecs component = (FontSpecs) parametersAndComponents.get(p.getName());
      // up.setValueToComponent(component, font);
    }
  }

  public BorderPane getPnChartPreview() {
    return pnChartPreview;
  }
}
