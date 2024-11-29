/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.hyperlinkText;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.NormalizeIntensityComboParameter;
import io.github.mzmine.parameters.parametertypes.NormalizeIntensityOptions;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.List;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;

public class SiriusExportParameters extends SimpleParameterSet {

  public static final OptionalModuleParameter<MsMsSpectraMergeParameters> MERGE_PARAMETER = new OptionalModuleParameter<>(
      "Merge MS/MS",
      "Merge high qualitative MS/MS into one spectrum instead of exporting all MS/MS separately.",
      new MsMsSpectraMergeParameters(), true);

  // SIRIUS is compatible with scientific format and this format better captures all numbers
  public static final NormalizeIntensityComboParameter NORMALIZE = new NormalizeIntensityComboParameter(
      NormalizeIntensityOptions.values(), NormalizeIntensityOptions.ORIGINAL_SCIENTIFIC_FORMAT);
  /**
   * MZTolerance to exclude duplicates in correlated spectrum
   */
  public static final MZToleranceParameter MZ_TOL = new MZToleranceParameter("m/z tolerance",
      "m/z tolerance to exclude duplicates in correlated spectrum", 0.003, 5);

  public static final BooleanParameter NEED_ANNOTATION = new BooleanParameter(
      "Only rows with annotation",
      "Only export rows with an annotation (run MS annotate or metaMSEcorrelate)", false);
  public static final BooleanParameter EXCLUDE_MULTICHARGE = new BooleanParameter(
      "Exclude multiple charge", "Do not export multiply charged rows", false);
  public static final BooleanParameter EXCLUDE_MULTIMERS = new BooleanParameter("Exclude multimers",
      "Do not export rows that were annotated as multimers (2M) (run MS annotate or metaMSEcorrelate)",
      false);
  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("mgf format for SIRIUS that contains MS1 and MS2 data", "*.mgf") //
  );
  public static final FileNameSuffixExportParameter FILENAME = new FileNameSuffixExportParameter(
      "Filename", STR."""
      Name of the output MGF file. Use pattern "\{SiriusExportTask.MULTI_NAME_PATTERN}" in the file name
      to substitute with feature list name. (i.e. "prefix_\{SiriusExportTask.MULTI_NAME_PATTERN}_suffix.mgf" would
      become "prefix_SourceFeatureListName_suffix.mgf"). If the file already exists, it will be overwritten.""",
      extensions, "sirius" // suffix
  );

  public SiriusExportParameters() {
    super(FEATURE_LISTS, FILENAME, NORMALIZE, MERGE_PARAMETER, MZ_TOL, NEED_ANNOTATION,
        EXCLUDE_MULTICHARGE, EXCLUDE_MULTIMERS);
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        boldText("SIRIUS Module Disclaimer: "), linebreak(),
        text("If you use the SIRIUS export module, cite:"), linebreak(), boldText("mzmine paper: "),
        ArticleReferences.MZMINE3.hyperlinkText(), linebreak(), boldText("SIRIUS 4 paper: "),
        ArticleReferences.SIRIUS4.hyperlinkText(), linebreak(),
        text("Sirius can be downloaded at the following adress: "),
        hyperlinkText("https://bio.informatik.uni-jena.de/software/sirius/"), linebreak(),
        text("Sirius results can be mapped into GNPS molecular networks "),
        hyperlinkText("see here",
            "https://bix-lab.ucsd.edu/display/Public/Mass+spectrometry+data+pre-processing+for+GNPS"));
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    boolean superCheck = super.checkParameterValues(errorMessages);

    if (getValue(FEATURE_LISTS).getMatchingFeatureLists().length > 1 && !getValue(
        FILENAME).getName().contains(SiriusExportTask.MULTI_NAME_PATTERN)) {
      errorMessages.add(
          "More than one feature list selected but \"" + SiriusExportTask.MULTI_NAME_PATTERN
          + "\" pattern not found in name."
          + "Please add the name pattern to create individual files.");
      superCheck = false;
    }

    return superCheck;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
