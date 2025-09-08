package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.mzmine.reports.FeatureDetail;
import io.mzmine.reports.FeatureSummary;
import io.mzmine.reports.ReportDataFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.collections.ObservableList;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.apache.xmlbeans.impl.soap.Detail;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReportingTask extends AbstractFeatureListTask {

  @NotNull
  private final FeatureList flist;
  private String desc;
  private JRCountingBeanCollectionDataSource detailSource;
  private JRCountingBeanCollectionDataSource summarySource;

  public ReportingTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList flist, Class<? extends MZmineModule> module) {
    super(storage, moduleCallDate, parameters, module);

    this.flist = flist;
    desc = "Reporting on feature list " + flist.getName();
  }

  private static @NotNull Map<String, Object> generateMetadata() {
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
    return parameters;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(flist);
  }

  @Override
  protected void process() {

    final ReportUtils report = new ReportUtils(ProjectService.getMetadata().getSampleTypeColumn());

    final List<FeatureListRow> rows = flist.getRows();
    final List<FeatureDetail> detail = new ArrayList<>();
    final List<FeatureSummary> summary = new ArrayList<>();

    totalItems = rows.size();

    for (int i = 0; i < rows.size(); i++) {
      final FeatureListRow row = rows.get(i);
      desc = "Preparing row data for row %d/%d".formatted(i, flist.getNumberOfRows());
      detail.add(report.getFeatureReportData(row));
      summary.add(report.getSummaryData(row));
      finishedItems.getAndIncrement();

      if(isCanceled()) {
        return;
      }
    }

    final Map<String, Object> parameters = generateMetadata();

    detailSource = new JRCountingBeanCollectionDataSource(detail);
    parameters.put("DETAILED_FEATURES_DATA_SOURCE", detailSource);

    summarySource = new JRCountingBeanCollectionDataSource(summary);
    parameters.put("SUMMARY_DATA_SOURCE", summarySource);

    JREmptyDataSource mainReportDataSource = new JREmptyDataSource();

    try {
      desc = "Printing report.";
      JasperPrint jasperPrint = JasperFillManager.fillReport(
          "C:\\Users\\Steffen\\JaspersoftWorkspace\\MyReports\\testreport.jasper", parameters,
          mainReportDataSource);

      if(isCanceled()) {
        return;
      }

      desc = "Exporting to pdf.";
      JasperExportManager.exportReportToPdfFile(jasperPrint,
          "C:\\Users\\Steffen\\Desktop\\testreport_full.pdf");
    } catch (JRException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getTaskDescription() {
    return desc;
  }

  @Override
  public double getFinishedPercentage() {
    final double preparation = super.getFinishedPercentage();
    final double printDetail = detailSource != null ? detailSource.getProgress() : 0;
    final double printSummary = summarySource != null ? summarySource.getProgress() : 0;
    return preparation * 0.3 + printDetail * 0.4 + printSummary * 0.2; // leftover for actual export
  }
}
