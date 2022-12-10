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

package io.github.mzmine.modules.dataprocessing.id_localcsvsearch;

import com.Ostermiller.util.CSVParser;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.DatabaseMatchInfo;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.RtRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CompoundAnnotationScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.MathUtils;
import java.io.File;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LocalCSVDatabaseSearchTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LocalCSVDatabaseSearchTask.class.getName());

  private final MobilityTolerance mobTolerance;
  private final Double ccsTolerance;
  private final File dataBaseFile;
  private final String fieldSeparator;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final ParameterSet parameters;
  private final List<ImportType> importTypes;
  private final IonLibraryParameterSet ionLibraryParameterSet;
  private final Boolean filterSamples;
  private final String sampleHeader;
  private final List<RawDataFile> raws;
  private IonNetworkLibrary ionNetworkLibrary;

  private String[][] databaseValues;
  private int finishedLines = 0;
  private final FeatureList flist;
  private int sampleColIndex = -1;

  LocalCSVDatabaseSearchTask(FeatureList peakList, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.flist = peakList;
    raws = flist.getRawDataFiles();
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
    sampleHeader = parameters.getParameter(LocalCSVDatabaseSearchParameters.filterSamples)
        .getEmbeddedParameter().getValue();
  }

  @Override
  public double getFinishedPercentage() {
    if (databaseValues == null) {
      return 0;
    }
    return ((double) finishedLines) / databaseValues.length;
  }

  @Override
  public String getTaskDescription() {
    return "Peak identification of " + flist + " using database " + dataBaseFile;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      ionNetworkLibrary =
          ionLibraryParameterSet != null ? new IonNetworkLibrary(ionLibraryParameterSet,
              mzTolerance) : null;
      // read database contents in memory
      FileReader dbFileReader = new FileReader(dataBaseFile);
      databaseValues = CSVParser.parse(dbFileReader,
          "\\t".equals(fieldSeparator) ? '\t' : fieldSeparator.charAt(0));

      final StringProperty error = new SimpleStringProperty();
      final List<ImportType> lineIds = CSVParsingUtils.findLineIds(importTypes, databaseValues[0],
          error);
      if (lineIds == null) {
        setErrorMessage(error.get());
        return;
      }

      // option to read more fields and append to comment as json
      final DataType<String> type = DataTypes.get(CommentType.class);
      List<ImportType> commentFields = extractCommentFields();
      if (commentFields == null) {
        setStatus(TaskStatus.ERROR);
        return;
      }

      // sample header index
      if (filterSamples) {
        sampleColIndex = getHeaderColumnIndex(databaseValues[0], sampleHeader);
        if (sampleColIndex == -1) {
          setErrorMessage("Sample header " + sampleHeader + " not found");
          setStatus(TaskStatus.ERROR);
          return;
        }
      }

      finishedLines++;
      for (; finishedLines < databaseValues.length; finishedLines++) {
        if (isCanceled()) {
          dbFileReader.close();
          return;
        }
        try {
          String[] currentLine = databaseValues[finishedLines];
          if (filterSamples && !matchSample(currentLine[sampleColIndex])) {
            // sample mismatch for this line
            continue;
          }

          processOneLine(currentLine, lineIds, commentFields);
        } catch (Exception e) {
          logger.log(Level.FINE, "Exception while processing csv line " + finishedLines, e);
        }
      }
      dbFileReader.close();

    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not read file " + dataBaseFile, e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.getMessage());
      return;
    }

    // Add task description to peakList
    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Peak identification using database " + dataBaseFile,
            LocalCSVDatabaseSearchModule.class, parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

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
        commentFields = CSVParsingUtils.findLineIds(commentFields, databaseValues[0], error);
        if (commentFields == null) {
          setErrorMessage(error.get());
        }
      }
    }
    return commentFields;
  }

  private boolean matchSample(final String sample) {
    return raws.stream().anyMatch(raw -> raw.getName().contains(sample));
  }

  private void processOneLine(@NotNull String[] values, @NotNull List<ImportType> linesWithIndices,
      @NotNull final List<ImportType> commentFields) {

    final CompoundDBAnnotation baseAnnotation = getCompoundFromLine(values, linesWithIndices,
        commentFields);
    final List<CompoundDBAnnotation> annotations = new ArrayList<>();
    if (ionNetworkLibrary != null) {
      annotations.addAll(
          CompoundDBAnnotation.buildCompoundsWithAdducts(baseAnnotation, ionNetworkLibrary));
    } else {
      annotations.add(baseAnnotation);
    }

    for (CompoundDBAnnotation annotation : annotations) {
      for (FeatureListRow peakRow : flist.getRows()) {
        final Float score = annotation.calculateScore(peakRow, mzTolerance, rtTolerance,
            mobTolerance, ccsTolerance);
        if (score != null && score > 0) {
          final CompoundDBAnnotation clone = annotation.clone();
          clone.put(CompoundAnnotationScoreType.class, score);
          clone.put(MzPpmDifferenceType.class,
              (float) MathUtils.getPpmDiff(Objects.requireNonNullElse(clone.getPrecursorMZ(), 0d),
                  peakRow.getAverageMZ()));
          if (annotation.get(CCSType.class) != null && peakRow.getAverageCCS() != null) {
            clone.put(CCSRelativeErrorType.class,
                PercentTolerance.getPercentError(annotation.get(CCSType.class),
                    peakRow.getAverageCCS()));
          }
          if (annotation.get(RTType.class) != null && peakRow.getAverageRT() != null) {
            clone.put(RtRelativeErrorType.class,
                PercentTolerance.getPercentError(annotation.get(RTType.class),
                    peakRow.getAverageRT()));
          }

          peakRow.addCompoundAnnotation(clone);
          peakRow.getCompoundAnnotations()
              .sort(Comparator.comparingDouble(a -> Objects.requireNonNullElse(a.getScore(), 0f)));
        }
      }
    }
  }

  @NotNull
  private CompoundDBAnnotation getCompoundFromLine(@NotNull String[] values,
      @NotNull List<ImportType> linesWithIndices, @NotNull final List<ImportType> commentFields) {
    var formulaType = DataTypes.get(FormulaType.class);
    var compoundNameType = DataTypes.get(CompoundNameType.class);
    var commentType = DataTypes.get(CommentType.class);
    var precursorMz = DataTypes.get(PrecursorMZType.class);
    var rtType = DataTypes.get(RTType.class);
    var mobType = DataTypes.get(MobilityType.class);
    var ccsType = DataTypes.get(CCSType.class);
    var smilesType = DataTypes.get(SmilesStructureType.class);
    var inchiType = DataTypes.get(InChIStructureType.class);
    var inchiKeyType = DataTypes.get(InChIKeyStructureType.class);
    var adductType = DataTypes.get(IonTypeType.class);
    var neutralMassType = DataTypes.get(NeutralMassType.class);
    var ionTypeType = DataTypes.get(IonTypeType.class);
    var pubchemIdType = new PubChemIdType();

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
    final Float lineRT = (entry.get(rtType) != null) ? Float.parseFloat(entry.get(rtType)) : null;
    final Float lineMob =
        (entry.get(mobType) != null) ? Float.parseFloat(entry.get(mobType)) : null;
    final Float lineCCS =
        (entry.get(ccsType) != null) ? Float.parseFloat(entry.get(ccsType)) : null;
    final Double neutralMass =
        entry.get(neutralMassType) != null ? Double.parseDouble(entry.get(neutralMassType)) : null;
    final String smiles = entry.get(smilesType);
    final String inchi = entry.get(inchiType);
    final String inchiKey = entry.get(inchiKeyType);
    final String pubchemId = entry.get(pubchemIdType);

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
    doIfNotNull(IonType.parseFromString(lineAdduct),
        () -> a.put(ionTypeType, IonType.parseFromString(lineAdduct)));
    doIfNotNull(pubchemId, () -> a.put(new DatabaseMatchInfoType(),
        new DatabaseMatchInfo(OnlineDatabases.PubChem, pubchemId)));
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
