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

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.hyperlinkText;
import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.InputSpectraSelectParameters.SelectInputScans;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizerComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import java.util.Map;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser.ExtensionFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GnpsFbmnExportAndSubmitParameters extends SimpleParameterSet {

  public static final SpectraMergeSelectParameter spectraMergeSelect = SpectraMergeSelectParameter.createGnpsSingleScanDefault();

  public static final FeatureListsParameter FEATURE_LISTS = new FeatureListsParameter();
  public static final OptionalModuleParameter<GnpsFbmnSubmitParameters> SUBMIT = new OptionalModuleParameter<>(
      "Submit to GNPS", "Directly submits a GNPS job", new GnpsFbmnSubmitParameters());
  public static final ComboParameter<FeatureListRowsFilter> FILTER = new ComboParameter<>(
      "Filter rows",
      "Limit the exported rows to those with MS/MS data or annotated rows (with ion identity)",
      FeatureListRowsFilter.values(), FeatureListRowsFilter.MS2_OR_ION_IDENTITY);
  public static final BooleanParameter OPEN_FOLDER = new BooleanParameter("Open folder",
      "Opens the export folder", false);

  // scientific format untested on GNPS FBMN
  public static final IntensityNormalizerComboParameter NORMALIZER = IntensityNormalizerComboParameter.createWithoutScientific();

  public static final ComboParameter<FeatureTableExportType> CSV_TYPE = new ComboParameter<>(
      "CSV export",
      "Either the new comprehensive export of mzmine or the legacy export from MZmine 2",
      FeatureTableExportType.values(), FeatureTableExportType.SIMPLE);
  public static final ComboParameter<AbundanceMeasure> FEATURE_INTENSITY = new ComboParameter<>(
      "Feature intensity", "Intensity in the quantification table (csv).",
      AbundanceMeasure.values(), AbundanceMeasure.Area);
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("MGF mascot file (spectra)", "*.mgf"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final FileNameSuffixExportParameter FILENAME = new FileNameSuffixExportParameter(
      "Filename", "Base name of the output files (.MGF and .CSV). "
                  + "Use pattern \"{}\" in the file name to substitute with feature list name. "
                  + "(i.e. \"blah{}blah.mgf\" would become \"blahSourceFeatureListNameblah.mgf\"). "
                  + "If the file already exists, it will be overwritten.", extensions, "iimn_fbmn");

  // legacy parameter replaced by other parameter only here for loading of old batches
  private final OptionalModuleParameter<MsMsSpectraMergeParameters> LEGACY_MERGE_PARAMETER = new OptionalModuleParameter<>(
      "Merge MS/MS (experimental)",
      "Merge high-quality MS/MS instead of exporting just the most intense one.",
      new MsMsSpectraMergeParameters(), true);

  public GnpsFbmnExportAndSubmitParameters() {
    super(new Parameter[]{FEATURE_LISTS, FILENAME, FILTER, spectraMergeSelect, NORMALIZER,
            FEATURE_INTENSITY, CSV_TYPE, SUBMIT, OPEN_FOLDER},
        "https://mzmine.github.io/mzmine_documentation/module_docs/GNPS_export/gnps_export.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    Region message = FxTextFlows.newTextFlowInAccordion("About the module/How to cite",
        boldText("About the GNPS Export/Submit Module:\n"),
        text("The GNPS Export module was designed for the"),
        boldText("Feature-Based Molecular Networking (FBMN)"),
        boldText("and the advanced Ion Identity Molecular Networking (IIMN)"), text("workflow on"),
        hyperlinkText("GNPS", "https://gnps.ucsd.edu"), text("See the"),
        hyperlinkText("FBMN documentation here",
            "https://ccms-ucsd.github.io/GNPSDocumentation/featurebasedmolecularnetworking/"),
        text("or a "), hyperlinkText("youtube playlist",
            "https://www.youtube.com/watch?v=vFcGG7T_44E&list=PL4L2Xw5k8ITzd9hx5XIP94vFPxj1sSafB&index=4&t=146s"),
        text("and"), boldText("please cite:\n"), boldText("IIMN paper: "),
        ArticleReferences.IIMN.hyperlinkText(), linebreak(), boldText("FBMN paper: "),
        ArticleReferences.FBMN.hyperlinkText(), linebreak(), boldText("GNPS paper: "),
        ArticleReferences.GNPS.hyperlinkText(), linebreak(), boldText("mzmine paper: "),
        ArticleReferences.MZMINE3.hyperlinkText());
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }


  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put(LEGACY_MERGE_PARAMETER.getName(), LEGACY_MERGE_PARAMETER);
    return map;
  }

  @Override
  public void handleLoadedParameters(final Map<String, Parameter<?>> loadedParams) {
    if (loadedParams.containsKey(LEGACY_MERGE_PARAMETER.getName())) {
      boolean merge = LEGACY_MERGE_PARAMETER.getValue();
      if (!merge) {
        getParameter(spectraMergeSelect).setUseInputScans(
            SelectInputScans.MOST_INTENSE_ACROSS_SAMPLES);
      } else {
        final var mergeParams = LEGACY_MERGE_PARAMETER.getEmbeddedParameters();
        MZTolerance mzTol = mergeParams.getValue(MsMsSpectraMergeParameters.MASS_ACCURACY);
        // one merged scan
        getParameter(spectraMergeSelect).setSimplePreset(
            SpectraMergeSelectPresets.SINGLE_MERGED_SCAN, mzTol);
      }
    }
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(final int version) {
    return switch (version) {
      case 3 -> """
          From mzmine version > 4.4.3 the scan selection and merging has been harmonized across modules.
          Please check and configure the %s parameter.""".formatted(spectraMergeSelect.getName());
      default -> null;
    };
  }

  @Override
  public int getVersion() {
    return 3;
  }
}
