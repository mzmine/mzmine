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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.molstructure.Structure2DRenderConfig;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.reporting.jasper.reporttypes.ReportModule;
import io.github.mzmine.util.reporting.jasper.reporttypes.ReportTypes;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ui.RectangleInsets;

public class ReportingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(ReportingTask.class.getName());

  @NotNull
  private final FeatureList flist;
  private final boolean includeProcessingParam;
  private String desc;
  private ReportModule reportModule;

  public ReportingTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList flist, Class<? extends MZmineModule> module) {
    super(storage, moduleCallDate, parameters, module);

    this.flist = flist;
    includeProcessingParam = parameters.getValue(ReportingParameters.includeProcessingParameters);
    desc = "Reporting on feature list " + flist.getName();
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {

    final ValueWithParameters<ReportTypes> valueWithParameters = parameters.getParameter(
        ReportingParameters.reportType).getValueWithParameters();
    final ReportTypes reportType = valueWithParameters.value();
    final ParameterSet reportTypeParam = valueWithParameters.parameters();

    final ReportUtils reportUtils = createReportUtils();

    final Map<String, Object> metadata = generateMetadata((ReportingParameters) getParameters());
    reportModule = reportType.getModuleInstance().createInstance(reportTypeParam);
    try {
      final JasperPrint jasperPrint = reportModule.generateReport(flist, metadata, reportUtils);
      if (isCanceled()) {
        reportModule.cancel();
        return;
      }
      if (jasperPrint == null) {
        error("Unknown error. Could not create print file.");
        return;
      }
      desc = "Exporting report for feature list %s to pdf.".formatted(flist.getName());

      final File exportPdf = getParameters().getValue(ReportingParameters.exportFile);
      FileAndPathUtil.createDirectory(exportPdf.getParentFile());
      JasperExportManager.exportReportToPdfFile(jasperPrint,
          FileAndPathUtil.getRealFilePath(exportPdf, "pdf").getAbsolutePath());
      desc = "Exporting report for feature list %s to HTML.".formatted(flist.getName());

      final File htmlDir = new File(exportPdf.getParent(),
          FilenameUtils.removeExtension(exportPdf.getName()) + "_html");
      htmlDir.delete();
      FileAndPathUtil.createDirectory(htmlDir);
      JasperExportManager.exportReportToHtmlFile(jasperPrint,
          FileAndPathUtil.getRealFilePath(htmlDir, exportPdf.getName(), ".html").getAbsolutePath());
    } catch (JRException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  /**
   * Report utils is created with all available values from {@link ReportingParameters} and the
   * specific reports may add specific values later.
   */
  private @NotNull ReportUtils createReportUtils() {
    final ReportingStyleParameters styleParams = parameters.getValue(
        ReportingParameters.reportingStyle);

    final EStandardChartTheme theme = new EStandardChartTheme("Aligned feature report");
    styleParams.getValue(ReportingStyleParameters.chartThemeParam).applyToChartTheme(theme);
    theme.setMirrorPlotAxisOffset(new RectangleInsets(0, 0, -2, 0));

    // DPI of report is roughly 3x of screen so export structures with greater zoom
    final Structure2DRenderConfig structureRenderConfig = styleParams.getValue(
        ReportingStyleParameters.structureRendering).createConfig().multiplyZoom(2.5);

    return new ReportUtils(theme, structureRenderConfig);
  }

  @NotNull Map<String, Object> generateMetadata(@NotNull final ReportingParameters parameters) {
    Map<String, Object> param = new HashMap<>();

    param.put("META_TITLE", parameters.getValue(ReportingParameters.reportTitle));
    param.put("META_FREE_TEXT_FIELD", parameters.getValue(ReportingParameters.freeText));

    final ReportAuthorParameters vendorParam = parameters.getValue(
        ReportingParameters.reportingVendorParam);
    vendorParam.addToMetadata(param);

    final ReportingOrderParameters orderParam = parameters.getValue(
        ReportingParameters.reportingOrderParam);
    orderParam.addToMetadata(param);

    final ReportingCustomerParameters customerParam = (ReportingCustomerParameters) parameters.getEmbeddedParameterValue(
        ReportingParameters.reportingCustomerParam);
    customerParam.addToMetadata(param);

    if (includeProcessingParam) {
      final String processingBatchParameters = flist.getAppliedMethods().stream().map(
          a -> a.getModule().getName() + "\n" + Arrays.stream(a.getParameters().getParameters())
              .map(p -> AppliedMethodsToStringUtils.parameterToString(p, "\t"))
              .collect(Collectors.joining("\n"))).collect(Collectors.joining("\n\n"));
      param.put("META_PROCESSING_PARAMETERS", processingBatchParameters);
    }

    return param;
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    return reportModule != null ? reportModule.getProgress() : 0d;
  }

  @Override
  public void cancel() {
    super.cancel();
    if (reportModule != null) {
      reportModule.cancel();
    }
  }

}
