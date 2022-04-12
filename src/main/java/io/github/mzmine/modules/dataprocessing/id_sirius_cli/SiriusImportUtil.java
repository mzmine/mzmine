package io.github.mzmine.modules.dataprocessing.id_sirius_cli;

import com.Ostermiller.util.CSVParser;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.DatabaseMatchInfo;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius.SiriusConfidenceScoreType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius.SiriusFingerIdScoreType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius.SiriusIdType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius.SiriusRankType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.sirius.SiriusScoreType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.modules.dataprocessing.id_onlinecompounddb.OnlineDatabases;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusImportUtil {

  private static final Logger logger = Logger.getLogger(SiriusImportUtil.class.getName());

  private static final String featureDirRegex = "([\\d]+)_([_a-zA-Z0-9.()\\s]" + "+)_([\\d]+)";
  private static final Pattern featureDirPattern = Pattern.compile(featureDirRegex);
  private static final int ROW_ID_GROUP = 3;

  private static final List<ImportType> fingerIdColumns = List.of(
      new ImportType(true, "rank", new SiriusRankType()), //
      new ImportType(true, "ConfidenceScore", new SiriusConfidenceScoreType()), //
      new ImportType(true, "CSI:FingerIDScore", new SiriusFingerIdScoreType()), //
      new ImportType(true, "ZodiacScore", new SiriusScoreType()), //
      new ImportType(true, "smiles", new SmilesStructureType()), //
      new ImportType(true, "pubchemids", new DatabaseMatchInfoType()), //
      new ImportType(true, "name", new CompoundNameType()), //
      new ImportType(true, "adduct", new IonTypeType()), //
      new ImportType(true, "molecularFormula", new FormulaType()),
      new ImportType(true, "id", new SiriusIdType()));

  private static final List<DataType<?>> requiredTypesBestId = List.of(
      new SiriusConfidenceScoreType(), new SiriusFingerIdScoreType(), new IonTypeType(),
      new SmilesStructureType(), new FormulaType(), new SiriusIdType());

  private static final List<DataType<?>> requiredTypesCandidates = List.of(
      new SiriusFingerIdScoreType(), new IonTypeType(), new SmilesStructureType(),
      new FormulaType()); // id type added via the folder name

  @Nullable
  public static File[] getFeatureDirectories(File siriusProjectDir) {
    checkProjectDirAndThrow(siriusProjectDir);
    return siriusProjectDir.listFiles(f -> f.getName().matches(featureDirRegex));
  }

  public static Map<Integer, CompoundDBAnnotation> readBestCompoundIdentifications(
      @NotNull final File siriusProjectDir) {
    checkProjectDirAndThrow(siriusProjectDir);
    final File fingerIdFile = new File(siriusProjectDir, "compound_identifications.tsv");
    checkFileAndThrow(fingerIdFile);

    final Map<Integer, CompoundDBAnnotation> annotationsMap = new HashMap<>();
    final List<CompoundDBAnnotation> compoundDBAnnotations = readAnnotationsFromFile(fingerIdFile);

    // map annotations to the feature list row id
    for (CompoundDBAnnotation annotation : compoundDBAnnotations) {
      if (annotation.hasValueForTypes(requiredTypesBestId)) {
        final String siriusId = annotation.get(SiriusIdType.class);
        final Matcher matcher = featureDirPattern.matcher(siriusId);
        if(!matcher.matches()) {
          logger.warning("Folder name does not match expected pattern " + siriusId);
          continue;
        }
        final String rowIdStr = matcher.group(ROW_ID_GROUP);
        annotationsMap.put(Integer.parseInt(rowIdStr), annotation);
      }
    }

    return annotationsMap;
  }

  public static Map<Integer, List<CompoundDBAnnotation>> readAllStructureCandidatesFromProject(
      @NotNull final File siriusProjectDir) {

    final @Nullable File[] featureDirectories = getFeatureDirectories(siriusProjectDir);
    if (featureDirectories == null) {
      throw new IllegalStateException(
          "No feature identifications found in " + siriusProjectDir + ". Did you run sirius?");
    }

    Map<Integer, List<CompoundDBAnnotation>> siriusAnnotations = new HashMap<>();
    for (File dir : featureDirectories) {
      if (dir == null) {
        continue;
      }

      final File candidates = new File(dir, "structure_candidates.tsv");
      checkFileAndThrow(candidates);
      // read candidates and keep only annotations with minimum types.
      final List<CompoundDBAnnotation> compoundDBAnnotations = readAnnotationsFromFile(
          candidates).stream().filter(a -> a.hasValueForTypes(requiredTypesCandidates)).toList();

      if (compoundDBAnnotations.isEmpty()) {
        continue;
      }

      final Matcher matcher = featureDirPattern.matcher(dir.getName());
      if (!matcher.matches()) {
        logger.warning("Feature dir did not match name pattern anymore. This is unexpected.");
        continue;
      }

      Integer rowId = Integer.parseInt(matcher.group(ROW_ID_GROUP));
      siriusAnnotations.put(rowId, compoundDBAnnotations);
    }
    return siriusAnnotations;
  }

  /**
   * Reads a sirius compound_identifications.tsv file and creates a list of {@link
   * CompoundDBAnnotation}'s with the values specified in {@link SiriusImportUtil#fingerIdColumns}.
   * If a column is not found, no entry is created in the {@link CompoundDBAnnotation}.
   *
   * @param compoundsFile The compound_identifications.tsv file.
   * @return A list of annotations. Empty if no annotations were found.
   */
  @NotNull
  private static List<CompoundDBAnnotation> readAnnotationsFromFile(
      @NotNull final File compoundsFile) {
    List<CompoundDBAnnotation> annotations = new ArrayList<>();
    try (FileInputStream fileInputStream = new FileInputStream(compoundsFile)) {
      final CSVParser parser = new CSVParser(fileInputStream, '\t');
      final String[] header = parser.getLine();
      final List<ImportType> lineIds = CSVParsingUtils.findLineIds(fingerIdColumns, header);

      final Map<ImportType, String> values = new HashMap<>();
      String[] line = null;
      while ((line = parser.getLine()) != null) {
        values.clear();
        for (ImportType lineId : lineIds) {
          values.put(lineId, line[lineId.getColumnIndex()]);
        }

        if (values.isEmpty()) {
          continue;
        }

        final CompoundDBAnnotation annotation = convertValueMapToAnnotation(values);
        annotations.add(annotation);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot parse sirius file " + compoundsFile, e);
    }
    return annotations;
  }

  /**
   * Converts a map of {@link ImportType}s and the read value for that import type to a {@link
   * CompoundDBAnnotation}.
   *
   * @param values The map to convert.
   * @return A Compound annotation. Null/empty values are not converted and not added to the map.
   */
  @NotNull
  private static CompoundDBAnnotation convertValueMapToAnnotation(Map<ImportType, String> values) {
    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    for (Entry<ImportType, String> entry : values.entrySet()) {
      final DataType dt = entry.getKey().getDataType();
      final String str = entry.getValue();
      final Object value;
      if (dt.getClass().equals(DatabaseMatchInfoType.class)) {
        value = new DatabaseMatchInfo(OnlineDatabases.PubChem, str.split(";")[0]);
      } else {
        value = dt.valueFromString(str);
      }
      if (value != null) {
        annotation.put(dt, value);
      }
    }
    return annotation;
  }

  /**
   * Checks if the file exists, throws an exception otherwise.
   *
   * @param fingerIdFile The file.
   */
  private static void checkFileAndThrow(File fingerIdFile) {
    if (isFileValid(fingerIdFile)) {
      throw new IllegalStateException(
          fingerIdFile.toString() + " does not exist or is not a file.");
    }
  }

  private static boolean isFileValid(File fingerIdFile) {
    return !fingerIdFile.exists() || fingerIdFile.isDirectory();
  }

  /**
   * Checks if the directory exists, throws an exception otherwise.
   *
   * @param siriusProjectDir The directory.
   */
  private static void checkProjectDirAndThrow(File siriusProjectDir) {
    if (isProjectDirValid(siriusProjectDir)) {
      throw new IllegalStateException("Given sirius project dir " + siriusProjectDir.toString()
          + " does not exist or is not a directory.");
    }
  }

  private static boolean isProjectDirValid(File siriusProjectDir) {
    return !siriusProjectDir.exists() || !siriusProjectDir.isDirectory();
  }
}
