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

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
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
      "Author", "Description of your company/institute.", new ReportAuthorParameters(), false,
      ReportingVendorModule.class);

  public static final ParameterSetParameter<ReportingCustomerParameters> reportingCustomerParam = new ParameterSetParameter<>(
      "Customer", "Customer related data.", new ReportingCustomerParameters(), true,
      ReportingCustomerModule.class);

  public static final ParameterSetParameter<ReportingOrderParameters> reportingOrderParam = new ParameterSetParameter<>(
      "Order", "Order related fields", new ReportingOrderParameters(), true,
      ReportingOrderModule.class);

  public static final TextParameter freeText = new TextParameter("Free text",
      "Include additional text, e.g. a greeting and describing your methods.", "", false);

  public static final ModuleOptionsEnumComboParameter<ReportTypes> reportType = new ModuleOptionsEnumComboParameter<>(
      "Report type", "Select the report type you want to generate.",
      ReportTypes.ALIGNED_FEATURE_REPORT);

  public static final FileNameSuffixExportParameter exportFile = new FileNameSuffixExportParameter(
      "Report file", "Set the file to export to.", List.of(new ExtensionFilter("pdf", "*.pdf")),
      "report");

  public static final BooleanParameter includeProcessingParameters = new BooleanParameter(
      "Include processing parameters",
      "Appends the processing parameters for each step to the end of the report (recommended).",
      true);

  public ReportingParameters() {
    super(flists, exportFile, reportTitle, reportingVendorParam, reportingCustomerParam,
        reportingOrderParam, freeText, includeProcessingParameters, reportType);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
