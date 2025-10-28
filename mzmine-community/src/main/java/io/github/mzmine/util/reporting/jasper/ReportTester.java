/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.gui.chartbasics.graphicsexport.ChartExportUtil;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.AnyXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.ConfigService;
import io.mzmine.reports.FeatureDetail;
import io.mzmine.reports.ReportDataFactory;
import io.mzmine.reports.SingleFigureRow;
import io.mzmine.reports.TwoFigureRow;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import net.sf.jasperreports.engine.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class ReportTester {

  public static final double[] x = {2.906069279, 2.913792133, 2.921446085, 2.929477453, 2.936477423,
      2.9437356, 2.951177359, 2.958379269, 2.965985537, 2.97361064, 2.980741739, 2.988462448,
      2.996629238, 3.004070759, 3.01226449, 3.020072699, 3.027705908, 3.035510302, 3.043330908,
      3.051156044, 3.058974743, 3.066714287, 3.074376583, 3.081707716, 3.088978529, 3.097345352,
      3.105057716, 3.112742901, 3.12044096, 3.127851248, 3.135499239, 3.142869949};
  public static final double[] y = {0, 3.69E+07, 2.33E+08, 5.25E+08, 7.88E+08, 8.96E+08, 7.75E+08,
      5.24E+08, 2.88E+08, 1.28E+08, 4.77E+07, 2.33E+07, 1.61E+07, 1.33E+07, 1.19E+07, 1.04E+07,
      9179349.238, 8145933.619, 7168681.595, 6618835.381, 6135780.238, 5633508.452, 5376301.595,
      5190724.381, 5040104.643, 5051896.881, 4859070.095, 4803438.738, 4752277.262, 4738109.405,
      4075587.214, 0};

  public static void main(String[] args) throws JRException, FileNotFoundException {
    final Toolkit tk = Toolkit.getDefaultToolkit();
    FxThread.initJavaFxInHeadlessMode();

    testMainReport();
  }

  private static void testMainReport() throws FileNotFoundException, JRException {

    Map<String, Object> parameters = new HashMap<>();
    parameters.put("META_COMPANY", "mzio GmbH");
    parameters.put("META_TITLE", "Example report");
    parameters.put("META_LAB_DESCRIPTION", """
        Dr. Steffen Heuckeroth
        CTO, PhD. Analytical Chemistry
        A street 1337
        481XX Münster
        Germany""");
    parameters.put("META_ORDER_NUMBER", "123SSDBJKUAH213");
    parameters.put("META_CUSTOMER_NAME", "Customer Name");
    parameters.put("META_CUSTOMER_DEPARTMENT", "Customer Department");
    parameters.put("META_CUSTOMER_ADDRESS", "Customer Road 537, A town, XXXXXX");
    parameters.put("META_CUSTOMER_PROJECT", "Research Project X15");
    parameters.put("META_ORDER_REQUEST_DATE", "15.03.2025");
    parameters.put("META_ORDER_FINISHED_DATE", "15.04.2025");
    parameters.put("META_ORDER_SAMPLEIDS", "1524835, 1535483, 16832185");
    parameters.put("META_FREE_TEXT_FIELD", """
        Sehr geehrte Frau Mustermann,
        die Probe(n) wurde(n) auftragsgemäß in Anlehnung an DIN EN ISO/IEC 9001 untersucht. Die
        Analysenergebnisse beziehen sich ausschließlich auf das zur Verfügung gestellte Probenmaterial zum
        Zeitpunkt der Analyse. Die Proben können bei Bedarf abgeholt werden. Ansonsten müssen die Proben
        aus platztechnischen Gründen leider nach 14 Tagen entsorgt werden.""");
    parameters.put("META_ORDER_DESC", "Identification of Metabolites in X");
    parameters.put("META_LOGO_PATH",
        "D:\\OneDrive - mzio GmbH\\mzio\\design\\logos\\svg\\logo_mzio.svg");

    parameters.put("LAB_ADDRESS", "Altenwall 26, 28195 Bremen");

//        parameters.put("SUMMARY_SUBREPORT", jasperSummarySubreport);
//        parameters.put("FEATURE_DETAIL_SUBREPORT", jasperFeatureDetailSubreport);

    // Data source for the Summary (if passed to a subreport/table component)
//        parameters.put("SUMMARY_DATA_SOURCE", new JRBeanCollectionDataSource(summaryData));

    // Data source for the Detailed Features (if passed to a subreport/list component)
    final JRCountingBeanCollectionDataSource detail = new JRCountingBeanCollectionDataSource(
        createDetailData());
    parameters.put("DETAILED_FEATURES_DATA_SOURCE", detail);

    final JRCountingBeanCollectionDataSource summary = new JRCountingBeanCollectionDataSource(
        ReportDataFactory.getSummary());
    parameters.put("SUMMARY_DATA_SOURCE", summary);

    // Main report's data source (can be empty if it's just a container)
    // If your main report iterates over some master object, provide that here.
    // For this example, let's assume the main report has no direct data source,
    // but uses parameters to pass data to subreports.
    JREmptyDataSource mainReportDataSource = new JREmptyDataSource();

    // 4. Fill the report
    JasperPrint jasperPrint = JasperFillManager.fillReport(
        "C:\\Users\\Steffen\\JaspersoftWorkspace\\MyReports\\mzmine_reports\\aligned_report\\aligned_report_cover.jasper",
        parameters, mainReportDataSource);


    System.out.println("start");
    JasperExportManager.exportReportToPdfFile(jasperPrint,
        "C:\\Users\\Steffen\\Desktop\\testreport_full.pdf");
    System.out.println("done");
  }

  private static @NotNull List<FeatureDetail> createDetailData() {
    List<FeatureDetail> data = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      data.add(new FeatureDetail("Feature summary of " + i, String.valueOf(i),
          String.valueOf(523.2123 + 5 * i), String.valueOf(12.2 + i * 0.3),
          String.valueOf(1512 + i),
          "Exact mass: 2318\ndelta m/z: 0.005\nInternal ID: 13sijawDSA8\nCAS: 231-2365-21",
          "C:\\Users\\Steffen\\Documents\\report_figures\\structure.png", "Additional text",
          List.of(new TwoFigureRow("C:\\Users\\Steffen\\Documents\\report_figures\\eic.png",
                  "Fig 1: Eic", "C:\\Users\\Steffen\\Documents\\report_figures\\mobilogram.png",
                  "Fig 2: Mobilogram"),
              new TwoFigureRow("C:\\Users\\Steffen\\Documents\\report_figures\\libmatch.png",
                  "Fig 3: Library match",

                  "C:\\Users\\Steffen\\Documents\\report_figures\\uv.png", "Fig 4: Correlation")),
          List.of(new SingleFigureRow(svgChartString().getBytes(), "Fig 5: Svg"))));
    }
    return data;
  }

  private static SimpleXYChart<?> createChart() {

    final ColoredXYDataset ds = new ColoredXYDataset(
        new AnyXYProvider(Color.RED, "eic", x.length, i -> x[i], i -> y[i]), RunOption.THIS_THREAD);
    final ColoredXYLineRenderer r = new ColoredXYLineRenderer();
    final SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>("RT / min",
        "Intensity / a.u.");

    final EStandardChartTheme defaultChartTheme = ConfigService.getConfiguration()
        .getDefaultChartTheme();
    defaultChartTheme.apply(chart);

    chart.addDataset(ds, r);
    return chart;
  }

  private static String svgChartString() {
    final SimpleXYChart<?> chart = createChart();

    try {
      return ChartExportUtil.writeChartToSvgString(chart.getChart(), 300, 150);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
