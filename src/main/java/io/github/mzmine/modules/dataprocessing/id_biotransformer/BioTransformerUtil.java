package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import com.Ostermiller.util.CSVParser;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BioTransformerUtil {

  private static final Logger logger = Logger.getLogger(BioTransformerUtil.class.getName());

  @Nullable
  public static List<String> buildCommandLineArguments(String smiles, ParameterSet param,
      File outputFile) {

    if (smiles == null) {
      return null;
    }

    final List<String> cmdList = new ArrayList<>();
    cmdList.add("java");

    final String path = param.getValue(BioTransformerParameters.bioPath).getAbsolutePath();
    final String name = new File(path).getName();
//    cmdList.add("-cp " + path.toString() + "\\executable\\Bio"
//        + "");
    cmdList.add("-jar");
    cmdList.add(name);
    cmdList.add("-k");
    cmdList.add("pred");

    final String transformation = param.getValue(BioTransformerParameters.transformationType);
    cmdList.add("-b");
    cmdList.add(transformation);

    final Integer steps = param.getValue(BioTransformerParameters.steps);
    cmdList.add("-s");
    cmdList.add(String.valueOf(steps));

    cmdList.add("-ismi");
    cmdList.add("\"" + smiles + "\"");
    cmdList.add("-ocsv");
    cmdList.add("\"" + outputFile.getAbsolutePath() + "\"");

//    final String cmdOptions = param.getValue(BioTransformerParameters.cmdOptions);
//    if (!cmdOptions.trim().isEmpty()) {
//      cmdList.add(" " + cmdOptions);
//    }

    return cmdList;
  }

  public static List<BioTransformerAnnotation> parseLibrary(final File file,
      final IonType[] ionTypes, @NotNull final AtomicBoolean canceled,
      @NotNull final AtomicInteger parsedLines) throws IOException {

    final FileReader dbFileReader = new FileReader(file);
    final CSVParser parser = new CSVParser(dbFileReader, ',');

    final List<BioTransformerAnnotation> annotations = new ArrayList<>();

    parser.getLine();
    String[] line = null;
    while ((line = parser.getLine()) != null && !canceled.get()) {
      for (final IonType ionType : ionTypes) {
        annotations.add(BioTransformerAnnotation.fromCsvLine(line, ionType));
      }
      parsedLines.getAndIncrement();
    }

    return annotations;
  }

  public static void matchLibraryToRow(List<BioTransformerAnnotation> annotations,
      FeatureListRow row, @Nullable final MZTolerance mzTolerance) {
    for (BioTransformerAnnotation annotation : annotations) {
      LocalCSVDatabaseSearchTask.checkMatchAnnotateRow(annotation, row, mzTolerance, null, null,
          null);
    }
  }

  public static boolean runCommandAndWait(File dir, List<String> cmd) {
    try {

      /*final File batchfile = new File(dir, "batchfile.bat");
      StringBuilder sb = new StringBuilder();
      try (BufferedWriter w = new BufferedWriter(new FileWriter(batchfile))) {
        for (String s : cmd) {
          w.write(s);
          w.write(" ");
          sb.append(s).append(" ");
        }
        w.newLine();
        w.write("exit");
        w.flush();
      }

      logger.info(sb.toString());

      final Process process = Runtime.getRuntime()
          .exec("cmd /c start " + batchfile.getName(), null, dir);
      batchfile.deleteOnExit();*/

      ProcessBuilder b = new ProcessBuilder();
      b.directory(dir);
      b.command(cmd);
      Process process = b.start();
      StringBuilder output = new StringBuilder();
      BufferedReader errorReader = new BufferedReader(
          new InputStreamReader(process.getErrorStream()));
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

      String line = null, error = null;
      while ((line = reader.readLine()) != null || (error = errorReader.readLine()) != null) {
        if (output != null) {
          output.append(line + "\n");
        }
        if (error != null) {
          output.append("ERROR: ").append(error).append("\n");
        }
      }

      int exitVal = process.waitFor();
      process.getOutputStream().close();
      logger.info(output.toString());
      if (exitVal != 0) {
        logger.warning(() -> "Error " + exitVal + " while running bio transformer command " + cmd);
        return false;
      }
    } catch (IOException | InterruptedException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      return false;
    }
    return true;
  }
}
