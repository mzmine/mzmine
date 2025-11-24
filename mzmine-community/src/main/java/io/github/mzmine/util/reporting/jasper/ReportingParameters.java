package io.github.mzmine.util.reporting.jasper;

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.TextParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.util.reporting.jasper.reporttypes.ReportTypes;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class ReportingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final StringParameter reportTitle = new StringParameter("Report title",
      "The title of the report.", "Feature report", true, true);

  public static final ParameterSetParameter<ReportAuthorParameters> reportingVendorParam = new ParameterSetParameter<>(
      "Author description", "Description of your company/institute.",
      new ReportAuthorParameters(), false, ReportingVendorModule.class);

  public static final ParameterSetParameter<ReportingCustomerParameters> reportingCustomerParam = new ParameterSetParameter<>(
      "Customer description", "Customer related data.", new ReportingCustomerParameters(), true,
      ReportingCustomerModule.class);

  public static final ParameterSetParameter<ReportingOrderParameters> reportingOrderParam = new ParameterSetParameter<>(
      "Order description", "Order related fields", new ReportingOrderParameters(), true,
      ReportingOrderModule.class);

  public static final TextParameter freeText = new TextParameter("Free text",
      "Include additional text, e.g. a greeting and describing your methods.", "", false);

  public static final ModuleOptionsEnumComboParameter<ReportTypes> reportType = new ModuleOptionsEnumComboParameter<>(
      "Report type", "Select the report type you want to generate.",
      ReportTypes.ALIGNED_FEATURE_REPORT);

  public static final FileNameSuffixExportParameter exportFile = new FileNameSuffixExportParameter(
      "Report file", "Set the file to export to.", List.of(new ExtensionFilter("pdf", "*.pdf")),
      "report");

  public ReportingParameters() {
    super(flists, exportFile, reportTitle, reportingVendorParam, reportingCustomerParam, reportingOrderParam,
        freeText, reportType);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
