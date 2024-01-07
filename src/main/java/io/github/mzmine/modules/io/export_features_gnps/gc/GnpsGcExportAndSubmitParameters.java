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

package io.github.mzmine.modules.io.export_features_gnps.gc;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportParameters;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportParameters.MzMode;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class GnpsGcExportAndSubmitParameters extends SimpleParameterSet {

  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mgf Mascot file (spectra)", "*.mgf"), //
      new ExtensionFilter("All files", "*.*") //
  );


  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter(1);

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Base name of the output files (.MGF and .CSV).", extensions, FileSelectionType.SAVE);

  public static final ComboParameter<MzMode> REPRESENTATIVE_MZ = new ComboParameter<AdapMgfExportParameters.MzMode>(
      "Representative m/z",
      "Choose the representative m/z of a an ADAP spectral cluster. This m/z is used as the PEPMASS in the mgf file.",
      MzMode.values(), MzMode.AS_IN_FEATURE_TABLE);

  public static final ComboParameter<AbundanceMeasure> FEATURE_INTENSITY = new ComboParameter<>(
      "Feature intensity", "Intensity in the quantification table (csv).",
      AbundanceMeasure.values(), AbundanceMeasure.Area);

//  public static final OptionalModuleParameter<GnpsGcSubmitParameters> SUBMIT = new OptionalModuleParameter<>(
//      "Submit to GNPS GC-MS", "Directly submits a GNPS-GC job", new GnpsGcSubmitParameters());

  public static final BooleanParameter OPEN_FOLDER = new BooleanParameter("Open folder",
      "Opens the export folder", false);

  public GnpsGcExportAndSubmitParameters() {
    super(FEATURE_LISTS,
        // parameters
        FILENAME, REPRESENTATIVE_MZ, FEATURE_INTENSITY,
        // SUBMIT,
        OPEN_FOLDER);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    String message = "<html><strong>Export/Submit to GNPS-GC:</strong>"
                     + "<p>The GNPS Export module was designed for the <strong>GC</strong> workflow on GNPS <a href=\"http://gnps.ucsd.edu\">http://gnps.ucsd.edu</a>.<br>"
                     + "See the <a href=\"https://ccms-ucsd.github.io/GNPSDocumentation/gc-ms-documentation/\"><strong>GNPS-GC-MS documentation here</strong></a> and <strong>please cite</strong>:<br>"
                     + "<ul>"
                     + "<li>the preprint <strong>GC-MS</strong> on GNPS: Aksenov et al.: <a href=\"https://www.biorxiv.org/content/10.1101/812404v1\">bioRxiv 812404 (2019)</a>.</li>"
                     + "<li>the <strong>GNPS</strong> paper: Wang et al.:<a href=\"https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.html\">Nature Biotechnology 34.8 (2016): 828-837</a></li>"
                     + "<li>and the <strong>MZmine</strong> paper: Pluskal et al.: <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">BMC Bioinformatics, 11, 395 (2010)</a></li>"
                     + "</ul></p>";
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

}
