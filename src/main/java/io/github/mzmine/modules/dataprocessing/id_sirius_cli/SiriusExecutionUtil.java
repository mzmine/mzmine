package io.github.mzmine.modules.dataprocessing.id_sirius_cli;

import com.google.common.util.concurrent.AtomicDouble;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusExecutionUtil {

  private static final Logger logger = Logger.getLogger(SiriusExecutionUtil.class.getName());

  public static Map<String, CompoundDBAnnotation> compileDatabase(@NotNull final FeatureList flist,
      @Nullable final AtomicDouble progress) {
    final Map<String, CompoundDBAnnotation> annotationMap = new HashMap<>();
    final DataType<?> smilesType = DataTypes.get(SmilesStructureType.class);

    final double numRows = flist.getNumberOfRows();
    double processed = 0d;

    for (final FeatureListRow row : flist.getRows()) {
      final List<CompoundDBAnnotation> annotations = row.getCompoundAnnotations();
      for (CompoundDBAnnotation a : annotations) {
        final String smiles = a.getSmiles();
        if (smiles == null || smiles.isBlank()) {
          continue;
        }
        annotationMap.put(smiles, a);
      }
      processed++;
      if (progress != null) {
        progress.getAndSet(processed / numRows);
      }
    }
    return annotationMap;
  }

  public static boolean writeCustomDatabase(final Map<String, CompoundDBAnnotation> db,
      final File file) {
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    try (var fileWriter = new FileWriter(file, false)) {
      final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
      CSVWriterBuilder builder = new CSVWriterBuilder(bufferedWriter).withSeparator('\t');
      final ICSVWriter writer = builder.build();

      for (Entry<String, CompoundDBAnnotation> entry : db.entrySet()) {
        final String smiles = entry.getKey();
        final CompoundDBAnnotation annotation = entry.getValue();
        final String name = annotation.getCompoundName();
        writer.writeNext(new String[]{smiles, name}, false);
      }
      bufferedWriter.flush();
      bufferedWriter.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return false;
    }
    return true;
  }

  public static File generateCustomDatabase(final File database, final File pathToSirius) {
    if (!pathToSirius.exists() || !pathToSirius.isFile()) {
      throw new IllegalArgumentException("Sirius not found at " + pathToSirius.toString());
    }
    if (!database.isFile()) {
      throw new IllegalArgumentException("No database found at " + database.toString());
    }

    final List<String> cmdList = new ArrayList<>();

    cmdList.add(pathToSirius.getAbsolutePath());
    cmdList.add("-i");
    cmdList.add("\"" + database.getAbsolutePath() + "\"");
    cmdList.add("custom-db");
//    cmdList.add("--name");
//    cmdList.add(database.getName().substring(0, database.getName().lastIndexOf(".")));
    cmdList.add("--location");
    cmdList.add("\"" + database.getParentFile() + File.separator + database.getName()
        .substring(0, database.getName().lastIndexOf(".")) + "\"");

    final ProcessBuilder b = new ProcessBuilder();
    b.inheritIO();
    b.directory(pathToSirius.getParentFile());
    b.command(cmdList);

    try {
      final Process process = b.start();
      final int exitVal = process.waitFor();
      logger.info("Sirius database prediction finished with value " + exitVal);
    } catch (IOException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }

    final File dbFolder = new File(database.getParentFile(),
        database.getName().substring(0, database.getName().lastIndexOf(".")));
    if (dbFolder.exists()) {
      return dbFolder;
    }
    throw new IllegalStateException(
        "Error while generating custom database. Expected folder does not exist "
            + dbFolder.toString());
  }

  public static void runFingerId(final File mgf, final File database, final File outputDir,
      final File pathToSirius) {
    if (!pathToSirius.exists() || !pathToSirius.isFile()) {
      throw new IllegalArgumentException("Sirius not found at " + pathToSirius.toString());
    }
    final List<String> cmdList = new ArrayList<>();
    cmdList.add(pathToSirius.getAbsolutePath());
    cmdList.add("-i");
    cmdList.add("\"" + mgf + "\"");
    cmdList.add("--output");
    cmdList.add("\"" + outputDir.getAbsolutePath() + "\"");
    cmdList.add("formula");
    cmdList.add("-d");
    cmdList.add("\"" + database.getAbsolutePath() + "\"");
    cmdList.add("fingerprint");
    cmdList.add("structure");
    cmdList.add("-d");
    cmdList.add("\"" + database.getAbsolutePath() + "\"");
    cmdList.add("write-summaries");

    final ProcessBuilder b = new ProcessBuilder(cmdList).directory(pathToSirius.getParentFile())
        .inheritIO();

    try {
      final Process process = b.start();
      final int i = process.waitFor();
      logger.info("Sirius finger id finished with code " + i);
    } catch (IOException | InterruptedException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
  }
}
