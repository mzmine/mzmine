package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.reporting.jasper.reporttypes.ReportModule;
import io.github.mzmine.util.reporting.jasper.reporttypes.ReportTypes;
import io.mzmine.reports.FeatureDetail;
import io.mzmine.reports.FeatureSummary;
import io.mzmine.reports.ReportDataFactory;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  private static final Logger logger = Logger.getLogger(ReportingTask.class.getName());

  @NotNull
  private final FeatureList flist;
  private String desc;
  private ReportModule reportModule;

  public ReportingTask(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Instant moduleCallDate, @Nullable MemoryMapStorage storage,
      @NotNull FeatureList flist, Class<? extends MZmineModule> module) {
    super(storage, moduleCallDate, parameters, module);

    this.flist = flist;
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

    final Map<String, Object> parameters = generateMetadata((ReportingParameters) getParameters());
    reportModule = reportType.getModuleInstance().createInstance(reportTypeParam);
    try {
      final JasperPrint jasperPrint = reportModule.generateReport(flist, parameters);
      if (isCanceled()) {
        reportModule.cancel();
        return;
      }
      if(jasperPrint == null) {
        error("Unknown error. Could not create print file.");
        return;
      }
      desc = "Exporting report for feature list %s to pdf.".formatted(flist.getName());
      JasperExportManager.exportReportToPdfFile(jasperPrint,
          getParameters().getValue(ReportingParameters.exportFile).getAbsolutePath());
    } catch (JRException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }

  @NotNull Map<String, Object> generateMetadata(@NotNull final ReportingParameters parameters) {
    Map<String, Object> param = new HashMap<>();

    param.put("META_TITLE", parameters.getValue(ReportingParameters.reportTitle));
    param.put("META_FREE_TEXT_FIELD", parameters.getValue(ReportingParameters.freeText));

    final ReportingVendorParameters vendorParam = parameters.getValue(
        ReportingParameters.reportingVendorParam);
    vendorParam.addToMetadata(param);

    final ReportingOrderParameters orderParam = parameters.getValue(
        ReportingParameters.reportingOrderParam);
    orderParam.addToMetadata(param);

    final ReportingCustomerParameters customerParam = parameters.getValue(
        ReportingParameters.reportingCustomerParam);
    customerParam.addToMetadata(param);

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
    if(reportModule != null) {
      reportModule.cancel();
    }
  }

}
