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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireParentType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSubclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.MolecularClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierPathwayType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import java.io.File;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.transformation.SortedList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalCSVDatabaseSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LocalCSVDatabaseSearchTask.class.getName());

  // all data types that we need
  private final FormulaType formulaType = DataTypes.get(FormulaType.class);
  private final CompoundNameType compoundNameType = DataTypes.get(CompoundNameType.class);
  private final CommentType commentType = DataTypes.get(CommentType.class);
  private final PrecursorMZType precursorMz = DataTypes.get(PrecursorMZType.class);
  private final RTType rtType = DataTypes.get(RTType.class);
  private final MobilityType mobType = DataTypes.get(MobilityType.class);
  private final CCSType ccsType = DataTypes.get(CCSType.class);
  private final SmilesStructureType smilesType = DataTypes.get(SmilesStructureType.class);
  private final InChIStructureType inchiType = DataTypes.get(InChIStructureType.class);
  private final InChIKeyStructureType inchiKeyType = DataTypes.get(InChIKeyStructureType.class);
  private final IonTypeType adductType = DataTypes.get(IonTypeType.class);
  private final NeutralMassType neutralMassType = DataTypes.get(NeutralMassType.class);
  private final IonTypeType ionTypeType = DataTypes.get(IonTypeType.class);
  private final PubChemIdType pubchemIdType = DataTypes.get(PubChemIdType.class);
  private final MolecularClassType molecularClassType = DataTypes.get(MolecularClassType.class);
  private final ClassyFireSuperclassType classyFireSuperclassType = DataTypes.get(
      ClassyFireSuperclassType.class);
  private final ClassyFireClassType classyFireClassType = DataTypes.get(ClassyFireClassType.class);
  private final ClassyFireSubclassType classyFireSubclassType = DataTypes.get(
      ClassyFireSubclassType.class);
  private final ClassyFireParentType classyFireParentType = DataTypes.get(
      ClassyFireParentType.class);
  private final NPClassifierSuperclassType npclassyfierSuperclassType = DataTypes.get(
      NPClassifierSuperclassType.class);
  private final NPClassifierClassType npclassyfierClassType = DataTypes.get(
      NPClassifierClassType.class);
  private final NPClassifierPathwayType npclassyfierPathwayType = DataTypes.get(
      NPClassifierPathwayType.class);
  private final DatabaseNameType databaseType = DataTypes.get(DatabaseNameType.class);

  // vars
  private final FeatureList[] featureLists;
  private final MobilityTolerance mobTolerance;
  private final Double ccsTolerance;
  private final File dataBaseFile;
  private final String fieldSeparator;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final IsotopePatternMatcherParameters isotopePatternMatcherParameters;
  private final MZTolerance isotopeMzTolerance;
  private final double minRelativeIsotopeIntensity;
  private final double minIsotopeScore;
  private final ParameterSet parameters;
  private final List<ImportType> importTypes;
  private final IonLibraryParameterSet ionLibraryParameterSet;
  private final Boolean filterSamples;
  private final String sampleHeader;
  private final List<RawDataFile> allRawDataFiles;
  private IonNetworkLibrary ionNetworkLibrary;

  private List<String[]> databaseValues;
  private int finishedLines = 0;
  private int sampleColIndex = -1;

  LocalCSVDatabaseSearchTask(FeatureList[] featureLists, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.featureLists = featureLists;
    this.parameters = parameters;

    dataBaseFile = parameters.getParameter(LocalCSVDatabaseSearchParameters.dataBaseFile)
        .getValue();
    fieldSeparator = parameters.getParameter(LocalCSVDatabaseSearchParameters.fieldSeparator)
        .getValue();
    importTypes = parameters.getParameter(LocalCSVDatabaseSearchParameters.columns).getValue();
    mzTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.rtTolerance).getValue();
    mobTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.mobTolerance)
        .getValue();
    ccsTolerance = parameters.getParameter(LocalCSVDatabaseSearchParameters.ccsTolerance)
        .getValue();

    Boolean calcMz = parameters.getValue(LocalCSVDatabaseSearchParameters.ionLibrary);
    ionLibraryParameterSet = calcMz != null && calcMz ? parameters.getParameter(
        LocalCSVDatabaseSearchParameters.ionLibrary).getEmbeddedParameters() : null;
    filterSamples = parameters.getValue(LocalCSVDatabaseSearchParameters.filterSamples);

    // all raw data files for a name check if selected
    allRawDataFiles = Arrays.stream(featureLists).map(FeatureList::getRawDataFiles)
        .flatMap(Collection::stream).distinct().toList();
    sampleHeader = parameters.getParameter(LocalCSVDatabaseSearchParameters.filterSamples)
        .getEmbeddedParameter().getValue();

    final boolean isotopePatternMatcher = parameters.getValue(
        LocalCSVDatabaseSearchParameters.isotopePatternMatcher);
    if (isotopePatternMatcher) {
      isotopePatternMatcherParameters = parameters.getParameter(
          LocalCSVDatabaseSearchParameters.isotopePatternMatcher).getEmbeddedParameters();
      isotopeMzTolerance = isotopePatternMatcherParameters.getParameter(
          IsotopePatternMatcherParameters.isotopeMzTolerance).getValue();
      minRelativeIsotopeIntensity = isotopePatternMatcherParameters.getParameter(
          IsotopePatternMatcherParameters.minIntensity).getValue();
      minIsotopeScore = isotopePatternMatcherParameters.getParameter(
          IsotopePatternMatcherParameters.minIsotopeScore).getValue();
    } else {
      isotopePatternMatcherParameters = null;
      isotopeMzTolerance = null;
      minRelativeIsotopeIntensity = 0d;
      minIsotopeScore = 0d;
    }
  }

  @Nullable
  private static Float replaceWildcardLowerEq0WithNull(final DataType<Float> type,
      final Map<DataType<?>, String> map) {
    float value = Float.parseFloat(map.getOrDefault(type, "-1"));
    return value > 0 ? value : null;
  }

  @Override
  public double getFinishedPercentage() {
    if (databaseValues == null) {
      return 0;
    }
    return ((double) finishedLines) / databaseValues.size();
  }

  @Override
  public String getTaskDescription() {
    return "Local CSV identification using database " + dataBaseFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      // read database contents in memory
      databaseValues = CSVParsingUtils.readData(dataBaseFile, fieldSeparator);
    } catch (NoSuchFileException e) {
      error("File %s does not exist.".formatted(
          Objects.requireNonNullElse(dataBaseFile, "File does not exist.")));
      return;
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      error(e.getMessage(), e);
      return;
    }

    try {
      ionNetworkLibrary =
          ionLibraryParameterSet != null ? new IonNetworkLibrary(ionLibraryParameterSet,
              mzTolerance) : null;

      final StringProperty error = new SimpleStringProperty();
      final List<ImportType> lineIds = CSVParsingUtils.findLineIds(importTypes,
          databaseValues.getFirst(), error);
      if (lineIds == null) {
        this.error(error.get());
        return;
      }

      // option to read more fields and append to comment as json
      List<ImportType> commentFields = extractCommentFields();
      if (commentFields == null) {
        error("Comment fields not found in CSV annotation");
        return;
      }

      // sample header index
      if (filterSamples) {
        sampleColIndex = getHeaderColumnIndex(databaseValues.getFirst(), sampleHeader);
        if (sampleColIndex == -1) {
          error("Sample header " + sampleHeader + " not found");
          return;
        }
      }

      // extract rows and sort by mz for binary search
      var mzSortedRows = Arrays.stream(featureLists)
          .map(flist -> flist.getRows().sorted(FeatureListRowSorter.MZ_ASCENDING)).toList();

      for (String[] currentLine : databaseValues) {
        if (finishedLines == 0) {
          finishedLines++;
          continue; // skip header
        }
        if (isCanceled()) {
          return;
        }
        try {
          // check already once for all raw data files
          if (filterSamples && !matchSample(allRawDataFiles, currentLine[sampleColIndex])) {
            // sample mismatch for this line
            continue;
          }

          processOneLine(mzSortedRows, currentLine, lineIds, commentFields);
        } catch (Exception e) {
          logger.log(Level.FINE, "Exception while processing csv line " + finishedLines, e);
        }
        finishedLines++;
      }

      for (final SortedList<FeatureListRow> flist : mzSortedRows) {
        for (final FeatureListRow row : flist) {
          var matches = row.getCompoundAnnotations().stream().sorted()
              .collect(Collectors.toCollection(ArrayList::new));
          if (matches.isEmpty()) {
            continue;
          }
          row.setCompoundAnnotations(matches);
        }
      }
      if (isotopePatternMatcherParameters != null) {
        for (FeatureList flist : featureLists) {
          refineAnnotationsByIsotopes(flist);
        }
      }


    } catch (Exception e) {
      error(e.getMessage(), e);
      return;
    }

    // Add task description to peakList
    for (var flist : featureLists) {
      flist.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod("Peak identification using database " + dataBaseFile,
              LocalCSVDatabaseSearchModule.class, parameters, getModuleCallDate()));
    }

    setStatus(TaskStatus.FINISHED);

  }

  private void refineAnnotationsByIsotopes(FeatureList flist) {
    DatabaseIsotopeRefinerScanBased.refineAnnotationsByIsotopesDifferentResolutions(flist.getRows(),
        isotopeMzTolerance, minRelativeIsotopeIntensity, minIsotopeScore);
  }

  /**
   * @return The list of comment fields if the fields were found successfully. Empty list if no
   * extra comments were selected. Null on error.
   */
  @Nullable
  private List<ImportType> extractCommentFields() {
    List<ImportType> commentFields = new ArrayList<>();
    final String appendComments = parameters.getValue(
        LocalCSVDatabaseSearchParameters.commentFields);
    if (appendComments != null && !appendComments.isBlank()) {
      final DataType<String> type = DataTypes.get(CommentType.class);
      commentFields = Arrays.stream(appendComments.split(",")).map(s -> s.trim().toLowerCase())
          .map(s -> new ImportType(true, s, type)).toList();
      if (!commentFields.isEmpty()) {
        final SimpleStringProperty error = new SimpleStringProperty();
        commentFields = CSVParsingUtils.findLineIds(commentFields, databaseValues.getFirst(),
            error);
        if (commentFields == null) {
          setErrorMessage(error.get());
        }
      }
    }
    return commentFields;
  }

  private boolean matchSample(final List<RawDataFile> raws, final String sample) {
    return raws.stream()
        .anyMatch(raw -> raw.getName().toLowerCase().contains(sample.toLowerCase()));
  }

  /**
   * @param mzSortedRows     rows per feature list sorted by mz
   * @param values           csv values to be parsed into annotation
   * @param linesWithIndices columns
   * @param commentFields    comment fields that are put together into the comment
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void processOneLine(final List<SortedList<FeatureListRow>> mzSortedRows,
      @NotNull String[] values, @NotNull List<ImportType> linesWithIndices,
      @NotNull final List<ImportType> commentFields) {

    final List<CompoundDBAnnotation> annotations = getCompoundDBAnnotations(values,
        linesWithIndices, commentFields);

    IntStream indexStream = IntStream.range(0, featureLists.length);
    if (featureLists.length > 1000) {
      indexStream.parallel();
    }
    // not all feature lists have all samples
    indexStream.forEach(i -> {
      var rawFiles = featureLists[i].getRawDataFiles();
      //  if active, check sample name contains id - this time for the feature list
      if (!filterSamples || matchSample(rawFiles, values[sampleColIndex])) {
        var sortedRows = mzSortedRows.get(i);

        for (CompoundDBAnnotation annotation : annotations) {
          List<FeatureListRow> candidates = binarySearchCandidates(sortedRows, annotation);

          for (FeatureListRow row : candidates) {
            checkMatchAndAnnotate(annotation, row, mzTolerance, rtTolerance, mobTolerance,
                ccsTolerance);
          }
        }
      }
    });
  }

  @NotNull
  private List<FeatureListRow> binarySearchCandidates(final SortedList<FeatureListRow> mzSortedRows,
      final CompoundDBAnnotation annotation) {
    // ranges are build with prechecks - so if there is no mobility use Range.all() to deactivate the filter
    Double mz = annotation.getPrecursorMZ();
    assert mz != null;
    final Float rt = annotation.getRT();
    final Float mobility = annotation.getMobility();
    var mzRange = mzTolerance.getToleranceRange(mz);
    Range<Float> rtRange =
        rtTolerance != null && rt != null ? rtTolerance.getToleranceRange(rt) : Range.all();
    Range<Float> mobilityRange =
        mobTolerance != null && mobility != null ? mobTolerance.getToleranceRange(mobility)
            : Range.all();

    // get all canditates with binary search
    // CCS is still missing here but will be tested later
    return FeatureListUtils.getCandidatesWithinRanges(mzRange, rtRange, mobilityRange, mzSortedRows,
        true);
  }

  @NotNull
  private List<CompoundDBAnnotation> getCompoundDBAnnotations(final @NotNull String[] values,
      final @NotNull List<ImportType> linesWithIndices,
      final @NotNull List<ImportType> commentFields) {
    final CompoundDBAnnotation baseAnnotation = getCompoundFromLine(values, linesWithIndices,
        commentFields);
    baseAnnotation.put(databaseType, dataBaseFile.getName());
    final List<CompoundDBAnnotation> annotations = new ArrayList<>();
    if (ionNetworkLibrary != null) {
      annotations.addAll(
          CompoundDBAnnotation.buildCompoundsWithAdducts(baseAnnotation, ionNetworkLibrary));
    } else {
      annotations.add(baseAnnotation);
    }
    return annotations;
  }

  private void checkMatchAndAnnotate(CompoundDBAnnotation annotation, FeatureListRow row,
      MZTolerance mzTolerance, RTTolerance rtTolerance, MobilityTolerance mobTolerance,
      Double percCcsTolerance) {

    final CompoundDBAnnotation clone = annotation.checkMatchAndCalculateDeviation(row, mzTolerance,
        rtTolerance, mobTolerance, percCcsTolerance);
    if (clone != null) {
      row.addCompoundAnnotation(clone);
    }
  }

  @NotNull
  private CompoundDBAnnotation getCompoundFromLine(@NotNull String[] values,
      @NotNull List<ImportType> linesWithIndices, @NotNull final List<ImportType> commentFields) {

    final Map<DataType<?>, String> entry = new HashMap<>();

    for (int i = 0; i < linesWithIndices.size(); i++) {
      var type = linesWithIndices.get(i);
      if (values[type.getColumnIndex()] != null && !values[type.getColumnIndex()].isEmpty()) {
        entry.put(type.getDataType(), values[type.getColumnIndex()]);
      }
    }

    final String lineName = entry.get(compoundNameType);
    final String lineFormula = entry.get(formulaType);
    final String lineAdduct = entry.get(adductType);
    final Double lineMZ =
        (entry.get(precursorMz) != null) ? Double.parseDouble(entry.get(precursorMz)) : null;

    // make sure to replace <=0 with null as this is defined as wildcards that match every value
    Float lineRT = replaceWildcardLowerEq0WithNull(rtType, entry);
    Float lineMob = replaceWildcardLowerEq0WithNull(mobType, entry);
    Float lineCCS = replaceWildcardLowerEq0WithNull(ccsType, entry);
    final Double neutralMass =
        entry.get(neutralMassType) != null ? Double.parseDouble(entry.get(neutralMassType)) : null;
    final String smiles = entry.get(smilesType);
    final String inchi = entry.get(inchiType);
    final String inchiKey = entry.get(inchiKeyType);
    final String pubchemId = entry.get(pubchemIdType);
    final String molecularClass = entry.get(molecularClassType);
    final String classyFireSuperclass = entry.get(classyFireSuperclassType);
    final String classyFireClass = entry.get(classyFireClassType);
    final String classyFireSubclass = entry.get(classyFireSubclassType);
    final String classyFireParent = entry.get(classyFireParentType);
    final String npclassifierSuperclass = entry.get(npclassyfierSuperclassType);
    final String npclassifierclass = entry.get(npclassyfierClassType);
    final String npclassifierPathway = entry.get(npclassyfierPathwayType);

    final String lineComment;
    if (!commentFields.isEmpty()) {
      String comment = entry.get(commentType);
      lineComment = (comment == null ? "" : comment + " ") + "{added:{" + commentFields.stream()
          .map(field -> field.getCsvColumnName() + ":" + values[field.getColumnIndex()])
          .collect(Collectors.joining(", ")) + "}}";
    } else {
      lineComment = entry.get(commentType);
    }

    CompoundDBAnnotation a = new SimpleCompoundDBAnnotation();
    doIfNotNull(lineName, () -> a.put(compoundNameType, lineName));
    doIfNotNull(lineFormula, () -> a.put(formulaType, lineFormula));
    doIfNotNull(lineComment, () -> a.put(commentType, lineComment));
    doIfNotNull(lineRT, () -> a.put(rtType, lineRT));
    doIfNotNull(lineMob, () -> a.put(mobType, lineMob));
    doIfNotNull(lineCCS, () -> a.put(ccsType, lineCCS));
    doIfNotNull(smiles, () -> a.put(smilesType, smiles));
    doIfNotNull(inchi, () -> a.put(inchiType, inchi));
    doIfNotNull(inchiKey, () -> a.put(inchiKeyType, inchiKey));
    doIfNotNull(lineMZ, () -> a.put(precursorMz, lineMZ));
    doIfNotNull(neutralMass, () -> a.put(neutralMassType, neutralMass));
    a.putIfNotNull(ionTypeType, IonTypeParser.parse(lineAdduct));
    a.putIfNotNull(molecularClassType, molecularClass);
    a.putIfNotNull(classyFireSuperclassType, classyFireSuperclass);
    a.putIfNotNull(classyFireClassType, classyFireClass);
    a.putIfNotNull(classyFireSubclassType, classyFireSubclass);
    a.putIfNotNull(classyFireParentType, classyFireParent);
    a.putIfNotNull(npclassyfierSuperclassType, npclassifierSuperclass);
    a.putIfNotNull(npclassyfierClassType, npclassifierclass);
    a.putIfNotNull(npclassyfierPathwayType, npclassifierPathway);
    return a;
  }

  private int getHeaderColumnIndex(final String[] firstLine, final String colHeader) {
    int colIndex = -1;
    for (int i = 0; i < firstLine.length; i++) {
      String columnName = firstLine[i];
      if (columnName.trim().equalsIgnoreCase(colHeader.trim())) {
        if (colIndex != -1) {
          return -1;
        }
        colIndex = i;
      }
    }
    return colIndex;
  }

  private void doIfNotNull(Object something, Runnable r) {
    if (something != null) {
      r.run();
    }
  }
}
