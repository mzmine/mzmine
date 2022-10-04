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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureMeasurementType;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class GnpsFbmnExportAndSubmitParameters extends SimpleParameterSet {

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();
  public static final OptionalModuleParameter<GnpsFbmnSubmitParameters> SUBMIT = new OptionalModuleParameter<>(
      "Submit to GNPS", "Directly submits a GNPS job", new GnpsFbmnSubmitParameters());
  public static final ComboParameter<FeatureListRowsFilter> FILTER = new ComboParameter<>(
      "Filter rows",
      "Limit the exported rows to those with MS/MS data or annotated rows (with ion identity)",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.MS2_OR_ION_IDENTITY);
  public static final BooleanParameter OPEN_FOLDER = new BooleanParameter("Open folder",
      "Opens the export folder", false);
  public static final OptionalModuleParameter<MsMsSpectraMergeParameters> MERGE_PARAMETER = new OptionalModuleParameter<>(
      "Merge MS/MS (experimental)",
      "Merge high-quality MS/MS instead of exporting just the most intense one.",
      new MsMsSpectraMergeParameters(), true);
  public static final ComboParameter<FeatureTableExportType> CSV_TYPE = new ComboParameter<>(
      "CSV export",
      "Either the new comprehensive export of MZmine 3 or the legacy export from MZmine 2",
      FeatureTableExportType.values(), FeatureTableExportType.SIMPLE);
  public static final ComboParameter<FeatureMeasurementType> FEATURE_INTENSITY = new ComboParameter<>(
      "Feature intensity", "Intensity in the quantification table (csv).",
      FeatureMeasurementType.values(), FeatureMeasurementType.AREA);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("MGF mascot file (spectra)", "*.mgf"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Base name of the output files (.MGF and .CSV). "
          + "Use pattern \"{}\" in the file name to substitute with feature list name. "
          + "(i.e. \"blah{}blah.mgf\" would become \"blahSourceFeatureListNameblah.mgf\"). "
          + "If the file already exists, it will be overwritten.", extensions,
      FileSelectionType.SAVE);


  public GnpsFbmnExportAndSubmitParameters() {
    super(new Parameter[]{FEATURE_LISTS, FILENAME, MERGE_PARAMETER, FILTER, FEATURE_INTENSITY,
        CSV_TYPE, SUBMIT, OPEN_FOLDER}, "https://mzmine.github.io/mzmine_documentation/module_docs/GNPS_export/gnps_export.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html><strong>About the GNPS Export/Submit Module:</strong>"
        + "<p>The GNPS Export module was designed for the <strong>Feature-Based Molecular Networking</strong> (FBMN) and the advanced Ion Identity Molecular Networking workflow on GNPS <a href=\"http://gnps.ucsd.edu\">http://gnps.ucsd.edu</a>.<br>"
        + "See the <a href=\"https://ccms-ucsd.github.io/GNPSDocumentation/featurebasedmolecularnetworking/\"><strong>FBMN documentation here</strong></a> (or a youtube <a href=\"https://www.youtube.com/watch?v=vFcGG7T_44E&list=PL4L2Xw5k8ITzd9hx5XIP94vFPxj1sSafB&index=4&t=146s\">playlist here</a>) and <strong>please cite</strong>:<br>"
        + "<ul>"
        + "<li>the <strong>IIMN</strong> paper: Schmid, Petras, Nothias et al.: <a href=\"https://www.nature.com/articles/s41467-021-23953-9\">Nature Communications 12, 3832 (2021)</a></li>"
        + "<li>the <strong>FBMN</strong> paper: Nothias, Petras, Schmid et al.: <a href=\"https://www.nature.com/articles/s41592-020-0933-6\">Nature Methods 17, 905–908 (2020)</a></li>"
        + "<li>the <strong>GNPS</strong> paper: Wang et al.:<a href=\"https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.html\">Nature Biotechnology 34.8 (2016): 828-837</a></li>"
        + "<li>and the <strong>MZmine</strong> paper: Pluskal et al.: <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">BMC Bioinformatics, 11, 395 (2010)</a></li>"
        + "</ul></p>";
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
