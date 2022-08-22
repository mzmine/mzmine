package io.github.mzmine.modules.tools.timstofmaldiacq;

import com.google.common.collect.Range;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.util.RangeUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class TimsTOFAcquisitionUtils {

  private static final Logger logger = Logger.getLogger(TimsTOFAcquisitionUtils.class.getName());

  public static Range<Float> adjustMobilityRange(Float mobility, Range<Float> initial,
      Double minMobilityWidth, Double maxMobilityWidth) {

    final Float initialLength = RangeUtils.rangeLength(initial);

    if (initialLength <= maxMobilityWidth && initialLength >= minMobilityWidth) {
      return initial;
    } else if (initialLength < minMobilityWidth) {
      return Range.closed((float) (mobility - minMobilityWidth / 2),
          (float) (mobility + minMobilityWidth / 2));
    } else if (initialLength > maxMobilityWidth) {
      return Range.closed((float) (mobility - maxMobilityWidth / 2),
          (float) (mobility + maxMobilityWidth / 2));
    }

    logger.fine(
        () -> String.format("Unexpected mobility range length: %.3f. Min = %.3f, Max = %.3f",
            initialLength, minMobilityWidth, maxMobilityWidth));
    return initial;
  }


  public static File createPrecursorCsv(List<MaldiTimsPrecursor> precursorList, String spot,
      int counter, File savePathDir) {
    final File csv = new File(savePathDir,
        "maldi_tims_precursors_" + spot + "_msms_" + counter + ".csv");

    try {
      csv.delete();
      csv.createNewFile();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot create maldi_tims_precursors.csv file", e);
      return null;
    }

    try (var writer = new FileWriter(csv)) {
      BufferedWriter w = new BufferedWriter(writer);
      w.write("1");  // activate CE settings from the MALDI MS/MS tab
      w.newLine();

      // make sure the precursors are sorted
      precursorList.sort(Comparator.comparingDouble(p -> p.oneOverK0().lowerEndpoint()));

      for (final MaldiTimsPrecursor precursor : precursorList) {
        w.write(
            String.format("%.4f,%.3f,%.3f", precursor.mz(), precursor.oneOverK0().lowerEndpoint(),
                precursor.oneOverK0().upperEndpoint()));
        w.newLine();
      }

      w.flush();
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return csv;
  }

  public static void appendToCommandFile(@NotNull File commandFile, final String spot,
      final List<MaldiTimsPrecursor> precursorList, final Integer initialOffsetY,
      final Integer incrementOffsetX, final Integer laserOffsetX, final Integer laserOffsetY,
      int precursorListCounter, int spotIncrement, final File savePathDir, String name,
      File currentCeFile, boolean enableCeStepping) throws IOException {

    var precursorCsv = createPrecursorCsv(precursorList, spot, precursorListCounter, savePathDir);
    if (precursorCsv == null) {
      throw new RuntimeException("Cannot create precursor list.");
    }

    final List<String> cmdLine = createArgumentList(spot, initialOffsetY, incrementOffsetX,
        spotIncrement, savePathDir, name, currentCeFile, enableCeStepping, laserOffsetX,
        laserOffsetY, precursorCsv);

    if (!commandFile.exists()) {
      commandFile.createNewFile();
    }

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(commandFile, true))) {
      for (String arg : cmdLine) {
        writer.write(arg + " ");
      }
      writer.newLine();
      writer.flush();
    }
  }

  public static boolean acquire(final File acqControl, final File commandFile, boolean exportOnly) {

    List<String> cmdLine = List.of("--commands", commandFile.getAbsolutePath());

    if (!exportOnly) {
      final ProcessBuilder builder = new ProcessBuilder(cmdLine).inheritIO();
      final Process process;

      try {
        process = builder.start();
        process.waitFor();
      } catch (IOException | InterruptedException e) {
        logger.log(Level.WARNING, "Could not acquire. Process finished irregularly.", e);
        return false;
      }
    }
    return true;
  }

  @NotNull
  private static List<String> createArgumentList(String spot, Integer initialOffsetY,
      Integer incrementOffsetX, int spotIncrement, File savePathDir, String name,
      File currentCeFile, boolean enableCeStepping, Integer laserOffsetX, Integer laserOffsetY,
      File precursorList) {

    final List<String> cmdLine = new ArrayList<>();

    cmdLine.add("--spot");
    cmdLine.add(spot);

    cmdLine.add("--xoffset");
    cmdLine.add(String.valueOf(incrementOffsetX != null ? incrementOffsetX * spotIncrement : 0));
    cmdLine.add("--yoffset");
    cmdLine.add(String.valueOf(initialOffsetY != null ? initialOffsetY : 0));

    cmdLine.add("--path");
    cmdLine.add(savePathDir.toString().replace(File.separatorChar, '/'));

    cmdLine.add("--name");
    cmdLine.add(name);

    cmdLine.add("--acqtype");
    cmdLine.add("single");

    cmdLine.add("--laseroffsetx");
    cmdLine.add(String.valueOf(laserOffsetX != null ? laserOffsetX : 0));
    cmdLine.add("--laseroffsety");
    cmdLine.add(String.valueOf(laserOffsetY != null ? laserOffsetY : 0));

    if (enableCeStepping && currentCeFile != null && currentCeFile.exists()) {
      cmdLine.add("--cetable");
      cmdLine.add(currentCeFile.toPath().toString());
    }

    cmdLine.add("--precursorlist");
    cmdLine.add(precursorList.getAbsolutePath());

    return cmdLine;
  }
}
