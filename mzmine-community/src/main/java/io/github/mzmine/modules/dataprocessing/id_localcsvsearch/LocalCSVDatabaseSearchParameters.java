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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import static io.github.mzmine.util.StringUtils.inQuotes;

import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundNameIdentifier;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireParentType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSubclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.MolecularClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierPathwayType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.PubChemIdType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.identifiers.CASType;
import io.github.mzmine.datamodel.features.types.identifiers.InternalIdType;
import io.github.mzmine.datamodel.features.types.identifiers.IupacNameType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.Q3QuantMzType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithStringInputParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.ComboWithStringInputValue;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.EmbeddedComponentOptions;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import io.github.mzmine.util.ParsingUtils;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
public class LocalCSVDatabaseSearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "Name of file that contains information for peak identification",
      ExtensionFilters.CSV_TSV_IMPORT, FileSelectionType.OPEN);

  public static final StringParameter fieldSeparator = new StringParameter("Field separator",
      "Character(s) used to separate fields in the database file. Use '\\t' for tab seperated files.",
      ",");

  public static final OptionalParameter<StringParameter> filterSamples = new OptionalParameter<>(
      new StringParameter("Filter filename header",
          "Column header to filter matches to only occur in the given sample. Used for library generation workflows.",
          "raw_filename"), false);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
      new RTToleranceParameter(), false);

  public static final OptionalParameter<MobilityToleranceParameter> mobTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter(new MobilityTolerance(0.01f)), false);

  public static final OptionalParameter<PercentParameter> ccsTolerance = new OptionalParameter<>(
      new PercentParameter("CCS tolerance (%)",
          "Maximum allowed difference (in per cent) for two ccs values.", 0.05), false);

  public static final OptionalModuleParameter<IonLibraryParameterSet> ionLibrary = new OptionalModuleParameter<>(
      "Use adducts",
      "If enabled, m/z values for multiple adducts will be calculated and matched against the feature list.",
      EmbeddedComponentOptions.VIEW_IN_WINDOW, new IonLibraryParameterSet());

  public static final OptionalModuleParameter<IsotopePatternMatcherParameters> isotopePatternMatcher = new OptionalModuleParameter<>(
      "Use isotope matcher",
      "Matches predicted and detected isotope pattern. Make sure to run isotope finder before on the feature list.",
      new IsotopePatternMatcherParameters());

  private static final Function<@Nullable String, @Nullable Float> wildcardFloatMapper = s -> {
    final Float v = ParsingUtils.stringToFloat(s);
    if (v == null || v >= 0) {
      return v;
    }
    return null;
  };

  public static final List<ImportType<?>> importTypes = List.of(
      new ImportType<>(true, "neutral_mass", new NeutralMassType()),
      new ImportType<>(true, "mz", new PrecursorMZType()), //
      new ImportType<>(true, "rt", new RTType(), wildcardFloatMapper), //
      new ImportType<>(true, "formula", new FormulaType()),
      new ImportType<>(true, "smiles", new SmilesStructureType()),
      new ImportType<>(false, "inchi", new InChIStructureType()),
      new ImportType<>(false, "inchi_key", new InChIKeyStructureType()),
      new ImportType<>(true, "name", new CompoundNameType()),
      new ImportType<>(false, "CCS", new CCSType(), wildcardFloatMapper),
      new ImportType<>(false, "mobility", new MobilityType(), wildcardFloatMapper),
      new ImportType<>(true, "comment", new CommentType()),
      new ImportType<>(false, "adduct", new IonTypeType()),
      new ImportType<>(false, "PubChemCID", new PubChemIdType()),
      new ImportType<>(false, "molecular_class", new MolecularClassType()),
      new ImportType<>(false, "classyfire_superclass", new ClassyFireSuperclassType()), //
      new ImportType<>(false, "classyfire_class", new ClassyFireClassType()),//
      new ImportType<>(false, "classyfire_subclass", new ClassyFireSubclassType()),//
      new ImportType<>(false, "classyfire_direct_parent", new ClassyFireParentType()),//
      new ImportType<>(false, "npclassifier_superclass", new NPClassifierSuperclassType()),//
      new ImportType<>(false, "npclassifier_class", new NPClassifierClassType()),//
      new ImportType<>(false, "npclassifier_pathway", new NPClassifierPathwayType()),//
      new ImportType<>(false, new Q3QuantMzType().getUniqueID(), new Q3QuantMzType()),//
      new ImportType<>(false, new IupacNameType()), //
      new ImportType<>(false, new CASType()), //
      new ImportType<>(false, new InternalIdType()));

  public static final ImportTypeParameter columns = new ImportTypeParameter("Columns",
      "Select the columns you want to import from the library file.", importTypes);

  public static final ComboWithStringInputParameter<HandleExtraColumnsOptions> extraColumns = new ComboWithStringInputParameter<>(
      new StringParameter("Additional columns",
          "Specify how additional columns in the database shall be handled, that are not specified/selected in the %s parameter.".formatted(
              columns.getName())), HandleExtraColumnsOptions.values(),
      HandleExtraColumnsOptions.IMPORT_SPECIFIC,
      new ComboWithStringInputValue<>(HandleExtraColumnsOptions.IGNORE, null));


  // old parameter
  private final StringParameter commentFields = new StringParameter("Append comment fields",
      "Multiple fields separated by comma that are appended to the comment. Like: Pathway,Synonyms",
      "", false);

  public LocalCSVDatabaseSearchParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_prec_local_cmpd_db/local-cmpd-db-search.html",
        peakLists, dataBaseFile, fieldSeparator, columns, mzTolerance, rtTolerance, mobTolerance,
        ccsTolerance, isotopePatternMatcher, ionLibrary, filterSamples, extraColumns);
  }

  @Override
  public boolean checkParameterValues(Collection<String> errorMessages) {
    final boolean superCheck = super.checkParameterValues(errorMessages);

    final List<ImportType<?>> selectedTypes = getParameter(columns).getValue().stream()
        .filter(ImportType::isSelected).toList();

    final boolean anyIdentifierSelected = isAnyIdentifierSelected(errorMessages, selectedTypes);

    boolean canDetermineMz = false;
    if (importTypeListContainsType(selectedTypes, new NeutralMassType()) && getValue(ionLibrary)) {
      canDetermineMz = true;
    } else if (importTypeListContainsType(selectedTypes, new PrecursorMZType())) {
      canDetermineMz = true;
    } else if (importTypeListContainsType(selectedTypes, new FormulaType()) && getValue(
        ionLibrary)) {
      canDetermineMz = true;
    } else if (importTypeListContainsType(selectedTypes, new SmilesStructureType()) && getValue(
        ionLibrary)) {
      canDetermineMz = true;
    }

    if (!canDetermineMz) {
      errorMessages.add("""
          Cannot determine precursor mz with currently selected data types. This requires either the "%s" or
          selection of the "%s" parameter paired with either of "%s", "%s", "%s".""".formatted(
          new PrecursorMZType().getHeaderString(), ionLibrary.getName(),
          new FormulaType().getHeaderString(), new NeutralMassType().getHeaderString(),
          new SmilesStructureType().getHeaderString()));
    }

    return superCheck && anyIdentifierSelected && canDetermineMz;
  }

  private boolean isAnyIdentifierSelected(Collection<String> errorMessages,
      List<ImportType<?>> selectedTypes) {
    boolean compoundIdentifierSelected = false;

    for (CompoundNameIdentifier identifierType : CompoundNameIdentifier.values()) {
      if (importTypeListContainsType(selectedTypes, identifierType.getDataType())) {
        compoundIdentifierSelected = true;
        break;
      }
    }

    if (!compoundIdentifierSelected) {
      errorMessages.add(
          "No compound identifier selected. Either of %s must be selected to provide an identifier for a compound.".formatted(
              inQuotes(Arrays.stream(CompoundNameIdentifier.values())
                  .map(CompoundNameIdentifier::getDataType).map(DataType::getHeaderString)
                  .collect(Collectors.joining(", ")))));
    }

    return compoundIdentifierSelected;
  }

  private boolean importTypeListContainsType(List<ImportType<?>> importTypes, DataType<?> type) {
    return importTypes.stream().anyMatch(importType -> importType.getDataType().equals(type));
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    var map = super.getNameParameterMap();
    map.put(commentFields.getName(), commentFields);
    return map;
  }

  @Override
  public void handleLoadedParameters(Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {
    if (loadedVersion == 1) {
      // before version 2, the parameters were not optional, if we imported rt, mob, or ccs, we always matched against those.
      final List<ImportType<?>> importTypes = getValue(columns);

      final boolean rtFilterEnabled = ImportType.isDataTypeSelectedInImportTypes(importTypes,
          RTType.class);
      setParameter(rtTolerance, rtFilterEnabled);

      final boolean mobFilterEnabled = ImportType.isDataTypeSelectedInImportTypes(importTypes,
          MobilityType.class);
      setParameter(mobTolerance, mobFilterEnabled);

      final boolean ccsFilterEnabled = ImportType.isDataTypeSelectedInImportTypes(importTypes,
          CCSType.class);
      setParameter(ccsTolerance, ccsFilterEnabled);
    }

    if (!loadedParams.containsKey(extraColumns.getName())) {
      // if the parameter was not loaded, we set to ignore. Otherwise we would not have clear
      // behaviour when loading old batches.
      final String current = getParameter(extraColumns).getValue().embeddedValue();
      setParameter(extraColumns,
          new ComboWithStringInputValue<>(HandleExtraColumnsOptions.IGNORE, current));
    }

    if (loadedParams.containsKey(commentFields.getName())) {
      final String commentFieldValues = commentFields.getValue();
      if (!commentFieldValues.isBlank()) {
        setParameter(extraColumns,
            new ComboWithStringInputValue<>(HandleExtraColumnsOptions.IMPORT_SPECIFIC,
                commentFieldValues));
      }
    }
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 -> """
          RT, mobility, and CCS tolerances were made optional. The parameters were enabled/disabled based on the types imported from the csv database.
          The parameter "Append comment fields" was replaced by the "Additional columns" parameter, which allows import of all or specific columns
          from the database and also maps those into a generated spectral library as JSON.
          """;
      default -> null;
    };
  }
}
