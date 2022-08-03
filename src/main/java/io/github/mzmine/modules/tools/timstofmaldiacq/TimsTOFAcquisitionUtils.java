package io.github.mzmine.modules.tools.timstofmaldiacq;

import com.google.common.collect.Range;
import com.google.common.io.Files;
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

  public static void replacePrecursorCsv(List<MaldiTimsPrecursor> precursorList, boolean createCopy,
      String spot, int counter, File savePathDir, boolean exportOnly) {

    assert !(exportOnly && savePathDir == null);

    // if we only export, just create the export file
    final File csv =
        !exportOnly ? new File("C:\\BDALSystemData\\timsTOF\\maldi\\maldi_tims_precursors.csv")
            : new File(savePathDir, "maldi_tims_precursors_" + spot + "_msms_" + counter + ".csv");
    if (csv.exists()) {
      final boolean deleted = csv.delete();
      if (!deleted) {
        throw new IllegalStateException("Cannot delete maldi_tims_precursors.csv file.");
      }
    }

    try {
      csv.createNewFile();
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot create maldi_tims_precursors.csv file", e);
      return;
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

    // only copy the file, if it was an actual acquisition
    if (createCopy && !exportOnly) {
      try {
        Files.copy(csv,
            new File(savePathDir, "maldi_tims_precursors_" + spot + "_msms_" + counter + ".csv"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static boolean acquire(final File acqControl, final String spot,
      final List<MaldiTimsPrecursor> precursorList, final int initialOffsetY,
      final int incrementOffsetX, int precursorListCounter, int spotIncrement,
      final File savePathDir, String name, File currentCeFile, boolean enableCeStepping,
      boolean exportOnly) {
    List<String> cmdLine = new ArrayList<>();

    cmdLine.add(acqControl.toString());
    cmdLine.add("--spot");
    cmdLine.add(spot);

    cmdLine.add("--xoffset");
    cmdLine.add(String.valueOf(incrementOffsetX * spotIncrement));
    cmdLine.add("--yoffset");
    cmdLine.add(String.valueOf(initialOffsetY));

    cmdLine.add("--path");
    cmdLine.add(savePathDir.toString().replace(File.separatorChar, '/'));

    cmdLine.add("--name");
    cmdLine.add(name);

    cmdLine.add("--acqtype");
    cmdLine.add("accumulate");

    if (enableCeStepping && currentCeFile != null && currentCeFile.exists()) {
      cmdLine.add("--cetable");
      cmdLine.add(currentCeFile.toPath().toString());
    }

    replacePrecursorCsv(precursorList, true, spot, precursorListCounter, savePathDir, exportOnly);

    if (!exportOnly) {
      final ProcessBuilder builder = new ProcessBuilder(cmdLine).inheritIO();
      final Process process;

      try {
        process = builder.start();
        process.waitFor();
      } catch (IOException | InterruptedException e) {
        logger.log(Level.WARNING,
            "Could not acquire spot " + spot + ". Process finished irregularly.", e);
        return false;
      }
    }
    return true;
  }

  public static boolean acquireLaserOffset(final File acqControl, final String spot,
      final List<MaldiTimsPrecursor> precursorList, final int laserOffsetX, final int laserOffsetY,
      int precursorListCounter, final File savePathDir, String name, File currentCeFile,
      boolean enableCeStepping, boolean exportOnly, String geometry) {
    List<String> cmdLine = new ArrayList<>();

    cmdLine.add(acqControl.toString());
    cmdLine.add("--spot");
    cmdLine.add(spot);

    cmdLine.add("--xoffset");
    cmdLine.add(String.valueOf(0));
    cmdLine.add("--yoffset");
    cmdLine.add(String.valueOf(0));

    cmdLine.add("--laseroffsetx");
    cmdLine.add(String.valueOf(laserOffsetX));
    cmdLine.add("--laseroffsety");
    cmdLine.add(String.valueOf(laserOffsetY));

    cmdLine.add("--path");
    cmdLine.add(savePathDir.toString().replace(File.separatorChar, '/'));

    cmdLine.add("--name");
    cmdLine.add(name);

    cmdLine.add("--geometry");
    cmdLine.add(geometry);

    if (enableCeStepping && currentCeFile != null && currentCeFile.exists()) {
      cmdLine.add("--cetable");
      cmdLine.add(currentCeFile.toPath().toString());
    }

    replacePrecursorCsv(precursorList, true, spot, precursorListCounter, savePathDir, exportOnly);

    if (!exportOnly) {
      final ProcessBuilder builder = new ProcessBuilder(cmdLine).inheritIO();
      final Process process;

      try {
        process = builder.start();
        process.waitFor();
      } catch (IOException | InterruptedException e) {
        logger.log(Level.WARNING,
            "Could not acquire spot " + spot + ". Process finished irregularly.", e);
        return false;
      }
    }
    return true;
  }
}
