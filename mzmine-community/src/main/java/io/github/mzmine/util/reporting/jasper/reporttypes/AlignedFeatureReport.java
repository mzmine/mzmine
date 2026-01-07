/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.util.reporting.jasper.reporttypes;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.gui.chartbasics.chartthemes.EStandardChartTheme;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.reporting.jasper.JRCountingBeanCollectionDataSource;
import io.github.mzmine.util.reporting.jasper.ReportUtils;
import io.mzmine.reports.FeatureDetail;
import io.mzmine.reports.FeatureSummary;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ui.RectangleInsets;

public class AlignedFeatureReport implements ReportModule {

  private final boolean includeSummary;
  private final boolean includeEvidence;
  private final AtomicLong totalItemsToPrepare = new AtomicLong(1);
  private final AtomicLong preparedItems = new AtomicLong();
  private final AtomicBoolean isCanceled = new AtomicBoolean(false);
  @NotNull
  private final MetadataColumn<?> groupingCol;
  @NotNull
  private final EStandardChartTheme theme;
  private JRCountingBeanCollectionDataSource detailSource = null;
  private JRCountingBeanCollectionDataSource summarySource = null;

  public AlignedFeatureReport() {
    includeSummary = true;
    includeEvidence = true;
    theme = new EStandardChartTheme("Aligned feature report");
    groupingCol = ProjectService.getMetadata().getSampleTypeColumn();
  }

  public AlignedFeatureReport(ParameterSet parameters) {
    includeSummary = parameters.getValue(AlignedFeatureReportParameters.includeSummaryTable);
    includeEvidence = parameters.getValue(AlignedFeatureReportParameters.includeEvidencePages);
    groupingCol = ProjectService.getMetadata()
        .getColumnByName(parameters.getValue(AlignedFeatureReportParameters.grouping));
    theme = new EStandardChartTheme("Aligned feature report");
    parameters.getValue(AlignedFeatureReportParameters.chartThemeParam).applyToChartTheme(theme);
    theme.setMirrorPlotAxisOffset(new RectangleInsets(0, 0, -2, 0));
  }

  @Override
  public ReportModule createInstance(@NotNull ParameterSet parameters) {
    return new AlignedFeatureReport(parameters);
  }

  @Override
  public void cancel() {
    isCanceled.set(true);
  }

  @Override
  public JasperPrint generateReport(FeatureList flist, Map<String, Object> jasperParameters)
      throws JRException {

    totalItemsToPrepare.set(flist.getNumberOfRows());

    final ReportUtils reportUtils = new ReportUtils(groupingCol, theme);

    final List<FeatureListRow> rows = flist.getRowsCopy();
    final List<FeatureDetail> detail = new ArrayList<>();
    final List<FeatureSummary> summary = new ArrayList<>();

    for (int i = 0; i < rows.size(); i++) {
      final FeatureListRow row = rows.get(i);
      detail.add(reportUtils.getFeatureReportData(row));
      summary.add(reportUtils.getSummaryData(row));
      preparedItems.getAndIncrement();

      if (isCanceled.get()) {
        return null;
      }
    }

    if (includeEvidence) {
      detailSource = new JRCountingBeanCollectionDataSource(detail);
      jasperParameters.put("DETAILED_FEATURES_DATA_SOURCE", detailSource);
    }

    if (includeSummary) {
      summarySource = new JRCountingBeanCollectionDataSource(summary);
      jasperParameters.put("SUMMARY_DATA_SOURCE", summarySource);
    }

    final File file = FileAndPathUtil.resolveInExternalToolsDir(
        "report_templates/aligned_report/aligned_report_cover.jasper");
    return JasperFillManager.fillReport(file.getAbsolutePath(), jasperParameters,
        new JREmptyDataSource());
  }

  @Override
  public double getProgress() {
    return ((double) preparedItems.get() / totalItemsToPrepare.get() * 0.3) //
        + (summarySource != null ? summarySource.getProgress() : 0) * 0.3 //
        + (detailSource != null ? detailSource.getProgress() : 0) * 0.3;
  }

  @Override
  public @NotNull String getName() {
    return "Aligned feature report";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return AlignedFeatureReportParameters.class;
  }
}
