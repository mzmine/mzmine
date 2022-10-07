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

package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class SiriusExportParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mgf format for SIRIUS", "*.mgf") //
  );


  public static final OptionalModuleParameter<MsMsSpectraMergeParameters> MERGE_PARAMETER =
      new OptionalModuleParameter<>("Merge MS/MS",
          "Merge high qualitative MS/MS into one spectrum instead of exporting all MS/MS separately.",
          new MsMsSpectraMergeParameters(), true);
  /**
   * MZTolerance to exclude duplicates in correlated spectrum
   */
  public static final MZToleranceParameter MZ_TOL = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance to exclude duplicates in correlated spectrum", 0.001, 5);
  public static final BooleanParameter RENUMBER_ID =
      new BooleanParameter("Renumber IDs", "Resets the IDs (uses the row ID otherwise)", false);

  public static final BooleanParameter NEED_ANNOTATION =
      new BooleanParameter("Only rows with annotation",
          "Only export rows with an annotation (run MS annotate or metaMSEcorrelate)", false);
  public static final BooleanParameter EXCLUDE_EMPTY_MSMS = new BooleanParameter(
      "Exclude empty MS/MS spectra",
      "Do not export empty MS/MS spectra (only features with MS/MS spectrum with at least 1 signal are exported)",
      false);
  public static final BooleanParameter EXCLUDE_MULTICHARGE =
      new BooleanParameter("Exclude multiple charge", "Do not export multiply charged rows", false);
  public static final BooleanParameter EXCLUDE_MULTIMERS = new BooleanParameter("Exclude multimers",
      "Do not export rows that were annotated as multimers (2M) (run MS annotate or metaMSEcorrelate)",
      false);
  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();
  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the output MGF file. "
      + "Use pattern \"{}\" in the file name to substitute with feature list name. "
      + "(i.e. \"blah{}blah.mgf\" would become \"blahSourceFeatureListNameblah.mgf\"). "
      + "If the file already exists, it will be overwritten.",
      extensions, FileSelectionType.SAVE);

  public SiriusExportParameters() {
    super(new Parameter[]{FEATURE_LISTS, FILENAME, MERGE_PARAMETER, MZ_TOL, RENUMBER_ID,
        NEED_ANNOTATION, EXCLUDE_EMPTY_MSMS, EXCLUDE_MULTICHARGE, EXCLUDE_MULTIMERS});
  }

  // public static final BooleanParameter FRACTIONAL_MZ = new
  // BooleanParameter(
  // "Fractional m/z values", "If checked, write fractional m/z values",
  // true);

  /*
   * public static final BooleanParameter INCLUDE_MSSCAN = new BooleanParameter( "include MS1",
   * "For each MS/MS scan include also the corresponding MS scan (additionally to possibly detected isotope patterns). MS1 scans might contain valuable informations that can be processed by SIRIUS. But they increase file size significantly"
   * , true );
   */

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html>SIRIUS Module Disclaimer:" + "<ul>"
                     + "<li>If you use the SIRIUS export module, cite <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following article:<br>"
                     + "<a href=\"http://dx.doi.org/10.1038/s41592-019-0344-8\">K. Duhrkop, et al., Sirius 4: a rapid tool for turning tandem mass spectra into metabolite structure information, Nature Methods, 2019.</a>"
                     + "<li>Sirius can be downloaded at the following address: <a href=\"https://bio.informatik.uni-jena.de/software/sirius/\">https://bio.informatik.uni-jena.de/software/sirius/</a>"
                     + "<li>Sirius results can be mapped into <a href=\"http://gnps.ucsd.edu/\">GNPS</a> molecular networks. <a href=\"https://bix-lab.ucsd.edu/display/Public/Mass+spectrometry+data+pre-processing+for+GNPS\">See the documentation</a>."
                     + "</ul>";
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
