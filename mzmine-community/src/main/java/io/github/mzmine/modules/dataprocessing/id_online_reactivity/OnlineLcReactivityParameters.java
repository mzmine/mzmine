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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonTypes;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithExampleExportParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.LegacyIonCheckComboBoxParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.io.CsvWriter;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.stage.FileChooser.ExtensionFilter;

public class OnlineLcReactivityParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();
  public static final BooleanParameter onlyGroupedRows = new BooleanParameter(
      "Only grouped features", "Check only grouped features (run metaCorrelate before)", true);
  public static final MZToleranceParameter mzTol = new MZToleranceParameter(0.003, 5);
  private static final Logger logger = Logger.getLogger(
      OnlineLcReactivityParameters.class.getName());
  private static final List<ExtensionFilter> extensions = List.of( //
      new ExtensionFilter("comma-separated values", "*.csv"), //
      new ExtensionFilter("tab-separated values", "*.tsv"), //
      new ExtensionFilter("All files", "*.*") //
  );
  public static final FileNameWithExampleExportParameter reactionsFile = new FileNameWithExampleExportParameter(
      "Reactions file", """
      This file needs to contain those columns:
      filename_contains,reaction,educt_smarts,reaction_smarts,delta_mz,type
      The raw data files should always contain a unique identifier that is listed in filename_contains (not ending or starting with numbers)
      type is either REACTION, EDUCT, PRODUCT""", extensions,
      OnlineLcReactivityParameters::exportExample);

  private static final List<IonType> adducts = IonTypes.listIons(false, IonTypes.H, IonTypes.NA);

  public static final LegacyIonCheckComboBoxParameter eductAdducts = new LegacyIonCheckComboBoxParameter(
      "Educt adducts", """
      Educt and product adducts define more combinations to check reactivity matches.
      This can be helpful if the ionization changes after the reaction, e.g.,
      Educt ionizes as [M+Na]+ and product as [M+H]+""", adducts, adducts);

  public static final LegacyIonCheckComboBoxParameter productAdducts = new LegacyIonCheckComboBoxParameter(
      "Product adducts", """
      Educt and product adducts define more combinations to check reactivity matches.
      This can be helpful if the ionization changes after the reaction, e.g.,
      Educt ionizes as [M+Na]+ and product as [M+H]+""", adducts, adducts);
  public static final OptionalParameter<MetadataGroupingParameter> uniqueSampleId = new OptionalParameter<>(
      new MetadataGroupingParameter("Unique sample ID metadata", """
          Metadata column that defines a unique sample ID.
          Go to Project/Metadata to load a metadata sheet and reload this module to select this column.
          The values should be a substring found in the filenames.
          Make sure to use a prefix or suffix before and after numbers otherwise id1 also matches id10.
          Just adding a id1_ will resolve this issue."""));
  public static final OptionalParameter<MetadataGroupingParameter> unreactedControls = new OptionalParameter<>(
      new MetadataGroupingParameter("Unreacted controls metadata", """
          Metadata column that defines all unreacted controls as true or control.
          Go to Project/Metadata to load a metadata sheet and reload this module to select this column."""));

  public OnlineLcReactivityParameters() {
    super(flists, reactionsFile, uniqueSampleId, unreactedControls, onlyGroupedRows, mzTol,
        eductAdducts, productAdducts);
  }


  private static void exportExample(File file) {
    var examples = List.of(new OnlineReaction("my_reaction",
        "unique_substring_contained_in_filenames_better_not_start_or_end_with_number_add_suffix",
        "([#6][CX3](=O)O)", "([#6][CX3](=O)O).(OC)>>[#6][CX3](=O)OC.O", 123.45));
    try {
      file = CSVUtils.ensureTsvOrCsvFormat(file, "tsv");

      CsvWriter.writeToFile(file, examples, OnlineReaction.class, WriterOptions.REPLACE);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot write example file", e);
    }
  }
}
