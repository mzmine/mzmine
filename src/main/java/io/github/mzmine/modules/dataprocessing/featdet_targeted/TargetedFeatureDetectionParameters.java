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
package io.github.mzmine.modules.dataprocessing.featdet_targeted;

import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class TargetedFeatureDetectionParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter rawDataFile = new RawDataFilesParameter();
  public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
      new ScanSelection(1));
  public static final StringParameter suffix = new StringParameter(
      "Name suffix", "Suffix to be added to feature list name", "detectedPeak");
  public static final FileNameParameter featureListFile = new FileNameParameter(
      "Database file",
      "Name of the file that contains a list of peaks for targeted feature detection.",
      FileSelectionType.OPEN);
  public static final StringParameter fieldSeparator = new StringParameter(
      "Field separator",
      "Character(s) used to separate fields in the database file. Use '\\t' for tab separated files.",
      ",");
  public static final PercentParameter intTolerance = new PercentParameter(
      "Intensity tolerance",
      "Maximum allowed deviation from expected /\\ shape of a peak in chromatographic direction");
  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();
  public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
      new RTToleranceParameter());
  public static final OptionalParameter<MobilityToleranceParameter> mobilityTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter());

  public static final OptionalModuleParameter<IonLibraryParameterSet> ionLibrary = new OptionalModuleParameter<>(
      "Calculate adduct masses",
      "Ion types to search for. Either neutral mass, formula or smiles must be imported for every compound.",
      new IonLibraryParameterSet());

  private static final List<ImportType> importTypes = List.of(
      new ImportType(true, "neutral mass", new NeutralMassType()),
      new ImportType(true, "mz", new PrecursorMZType()), //
      new ImportType(true, "rt", new RTType()), //
      new ImportType(true, "formula", new FormulaType()),
      new ImportType(true, "smiles", new SmilesStructureType()),
      new ImportType(false, "adduct", new IonAdductType()),
      new ImportType(false, "inchi", new InChIStructureType()),
      new ImportType(false, "inchi key", new InChIKeyStructureType()),
      new ImportType(false, "name", new CompoundNameType()),
      new ImportType(false, "CCS", new CCSType()),
      new ImportType(false, "mobility", new MobilityType()),
      new ImportType(true, "comment", new CommentType()));

  public static final ImportTypeParameter columns = new ImportTypeParameter("Columns",
      "Select the columns you want to import from the library file.", importTypes);

  public TargetedFeatureDetectionParameters() {
    super(new Parameter[]{rawDataFile, scanSelection, suffix, featureListFile, fieldSeparator,
            columns, intTolerance, mzTolerance, rtTolerance, mobilityTolerance, ionLibrary},
        "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/targeted_featdet/targeted-featdet.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
