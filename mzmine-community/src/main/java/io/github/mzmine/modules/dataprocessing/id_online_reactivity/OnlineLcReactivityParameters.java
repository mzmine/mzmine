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

import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameWithExampleExportParameter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonCheckComboBoxParameter;
import io.github.mzmine.parameters.parametertypes.metadata.MetadataGroupingParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import io.github.mzmine.util.io.CSVUtils;
import io.github.mzmine.util.io.CsvWriter;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OnlineLcReactivityParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();
  public static final BooleanParameter onlyGroupedRows = new BooleanParameter(
      "Only grouped features", "Check only grouped features (run metaCorrelate before)", true);
  public static final MZToleranceParameter mzTol = new MZToleranceParameter(0.003, 5);
  private static final Logger logger = Logger.getLogger(
      OnlineLcReactivityParameters.class.getName());

  public static final FileNameWithExampleExportParameter reactionsFile = new FileNameWithExampleExportParameter(
      "Reactions file", """
      This file needs to contain those columns:
      filename_contains,reaction,educt_smarts,reaction_smarts,delta_mz,type
      The raw data files should always contain a unique identifier that is listed in filename_contains (not ending or starting with numbers)
      type is either REACTION, EDUCT, PRODUCT""", ExtensionFilters.CSV_TSV_IMPORT,
      OnlineLcReactivityParameters::exportExample);

  // currently only allows single charge ions
  private static final List<IonModification> adducts = List.of(IonModification.H,
      IonModification.NA, IonModification.H_H2O_1);
  // currently only allows single charge ions
  private static final List<IonModification> defaultSelectedAdducts = List.of(IonModification.H,
      IonModification.NA);

  public static final IonCheckComboBoxParameter eductAdducts = new IonCheckComboBoxParameter(
      "Educt adducts", """
      Educt and product adducts define more combinations to check reactivity matches.
      This can be helpful if the ionization changes after the reaction, e.g.,
      Educt ionizes as [M+Na]+ and product as [M+H]+""", adducts, defaultSelectedAdducts);

  public static final IonCheckComboBoxParameter productAdducts = new IonCheckComboBoxParameter(
      "Product adducts", """
      Educt and product adducts define more combinations to check reactivity matches.
      This can be helpful if the ionization changes after the reaction, e.g.,
      Educt ionizes as [M+Na]+ and product as [M+H]+""", adducts, defaultSelectedAdducts);
  public static final OptionalParameter<MetadataGroupingParameter> uniqueSampleId = new OptionalParameter<>(
      new MetadataGroupingParameter("Unique sample ID", """
          Metadata column that defines a unique sample ID.
          Go to Project/Metadata to load a metadata sheet and reload this module to select this column.
          The values should be a substring found in the filenames.
          Make sure to use a prefix or suffix before and after numbers otherwise id1 also matches id10.
          Just adding a id1_ will resolve this issue."""));
  public static final OptionalParameter<MetadataGroupingParameter> reactionSampleType = new OptionalParameter<>(
      new MetadataGroupingParameter("Reaction sample type", """
          Metadata column that defines all samples as either control or reacted.
          Control is unreacted and reacted is after applying the reaction.
          Go to Project/Metadata to load a metadata sheet and reload this module to select this column."""));

  public OnlineLcReactivityParameters() {
    super(flists, reactionsFile, uniqueSampleId, reactionSampleType, onlyGroupedRows, mzTol,
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

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    final Map<String, Parameter<?>> map = super.getNameParameterMap();
    map.put("Unreacted controls metadata", getParameter(reactionSampleType));
    map.put("Unique sample ID metadata", getParameter(uniqueSampleId));
    return map;
  }
}
