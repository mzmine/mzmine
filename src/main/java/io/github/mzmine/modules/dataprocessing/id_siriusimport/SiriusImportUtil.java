package io.github.mzmine.modules.dataprocessing.id_siriusimport;

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

  private static final String featureDirRegex = "([\\d+])_([_a-zA-Z0-9.()\\s]" + "+)_(\\d+)";
  private static final Pattern featureDirPattern = Pattern.compile(featureDirRegex);

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

  private static final List<DataType<?>> requiredTypes = List.of(new SiriusConfidenceScoreType(),
      new SiriusFingerIdScoreType(), new IonTypeType(), new SmilesStructureType(),
      new FormulaType(), new SiriusIdType());

  @Nullable
  public static File[] getFeatureDirectories(File siriusProjectDir) {
    checkProjectDir(siriusProjectDir);
    return siriusProjectDir.listFiles(f -> f.getName().matches(featureDirRegex));
  }

  public static Map<Integer, CompoundDBAnnotation> readBestCompoundIdentifications(
      File siriusProjectDir) {
    checkProjectDir(siriusProjectDir);
    final File fingerIdFile = new File(siriusProjectDir, "compound_identifications.tsv");
    checkFile(fingerIdFile);

    final Map<Integer, CompoundDBAnnotation> annotationsMap = new HashMap<>();
    final List<CompoundDBAnnotation> compoundDBAnnotations = readAnnotationsFromFile(fingerIdFile);

    // map annotations to the feature list row id
    for (CompoundDBAnnotation annotation : compoundDBAnnotations) {
      if (annotation.hasValueForTypes(requiredTypes)) {
        final String siriusId = annotation.get(SiriusIdType.class);
        final Matcher matcher = featureDirPattern.matcher(siriusId);
        assert matcher.matches();
        final String rowIdStr = matcher.group(3);
        annotationsMap.put(Integer.parseInt(rowIdStr), annotation);
      }
    }

    return annotationsMap;
  }

  private static List<CompoundDBAnnotation> readAnnotationsFromFile(final File compoundsFile) {
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

  private static void checkFile(File fingerIdFile) {
    if (!fingerIdFile.exists() || fingerIdFile.isDirectory()) {
      throw new IllegalStateException(
          fingerIdFile.toString() + " does not exist or is not a file.");
    }
  }

  private static void checkProjectDir(File siriusProjectDir) {
    if (!siriusProjectDir.exists() || !siriusProjectDir.isDirectory()) {
      throw new IllegalStateException("Given sirius project dir " + siriusProjectDir.toString()
          + " does not exist or is not a directory.");
    }
  }
}
