/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.timstofmaldiacq;

import static io.github.mzmine.util.StringUtils.inQuotes;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.modules.io.import_rawdata_bruker_tdf.datamodel.sql.MaldiSpotInfo;
import io.github.mzmine.modules.tools.timstofmaldiacq.imaging.ImagingSpot;
import io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection.MaldiTimsPrecursor;
import io.github.mzmine.util.RangeUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimsTOFAcquisitionUtils {

  private static final Logger logger = Logger.getLogger(TimsTOFAcquisitionUtils.class.getName());

  /**
   * Adjusts the mobility range to a valid range inside the boundaries. The range may be shorter
   * than requested by the minMobilityWidth if it would lie outside the bounds of mobilityBounds.
   *
   * @return A valid mobility range.
   */
  public static Range<Float> adjustMobilityRange(Float mobility, Range<Float> initial,
      Double minMobilityWidth, Double maxMobilityWidth, Range<Float> mobilityBounds) {

    final Float initialLength = RangeUtils.rangeLength(initial);

    if (initialLength <= maxMobilityWidth && initialLength >= minMobilityWidth) {
      return initial;
    } else if (initialLength < minMobilityWidth) {
      final float toBeLowerBound = (float) (mobility - minMobilityWidth / 2);
      final float toBeUpperBound = (float) (mobility + minMobilityWidth / 2);
      return Range.closed(Math.max(toBeLowerBound, mobilityBounds.lowerEndpoint()),
          Math.min(toBeUpperBound, mobilityBounds.upperEndpoint()));
    } else if (initialLength > maxMobilityWidth) {
      final float toBeLowerBound = (float) (mobility - maxMobilityWidth / 2);
      final float toBeUpperBound = (float) (mobility + maxMobilityWidth / 2);
      return Range.closed(Math.max(toBeLowerBound, mobilityBounds.lowerEndpoint()),
          Math.min(toBeUpperBound, mobilityBounds.upperEndpoint()));
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
//      w.write("1");  // activate CE settings from the MALDI MS/MS tab
//      w.newLine();

      // make sure the precursors are sorted
      precursorList.sort(Comparator.comparingDouble(p -> p.mobility().lowerEndpoint()));

      for (final MaldiTimsPrecursor precursor : precursorList) {
        w.write(
            String.format("%.4f,%.3f,%.3f", precursor.mz(), precursor.mobility().lowerEndpoint(),
                precursor.mobility().upperEndpoint()));
        w.newLine();
      }

      w.flush();
      w.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return csv;
  }

  /**
   * @param commandFile          path to command file
   * @param spot                 spot name
   * @param precursorList        list of precursors
   * @param xOffset              offset for the stage, multiplied by the spotIncrement
   * @param yOffset              offset for the stage
   * @param laserOffsetX         offset for the laser
   * @param laserOffsetY         offset for the laser
   * @param precursorListCounter
   * @param savePathDir          path to the parent folder for acquired data
   * @param name                 name of the measurement
   * @param currentCeFile        optional path to the selected ce file
   * @param enableCeStepping     optional
   * @param isolationWidth       optional isolation width, overrides the ce table, if there is one.
   * @throws IOException
   */
  public static void appendToCommandFile(@NotNull File commandFile, final String spot,
      final List<MaldiTimsPrecursor> precursorList, final Integer xOffset, final Integer yOffset,
      final Integer laserOffsetX, final Integer laserOffsetY, int precursorListCounter,
      final File savePathDir, String name, File currentCeFile, boolean enableCeStepping,
      final Double isolationWidth) throws IOException {

    var precursorCsv = createPrecursorCsv(precursorList, spot, precursorListCounter, savePathDir);
    if (precursorCsv == null) {
      throw new RuntimeException("Cannot create precursor list.");
    }

    final List<String> cmdLine = createArgumentList(spot, xOffset, yOffset, savePathDir, name,
        currentCeFile, enableCeStepping, laserOffsetX, laserOffsetY, precursorCsv, isolationWidth);

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

    List<String> cmdLine = List.of(acqControl.getAbsolutePath(), "--commandfile",
        inQuotes(commandFile.getAbsolutePath()));

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
  private static List<String> createArgumentList(String spot, Integer xOffset, Integer yOffset,
      File savePathDir, String name, File currentCeFile, boolean enableCeStepping,
      Integer laserOffsetX, Integer laserOffsetY, File precursorList, Double isolationWidth) {

    final List<String> cmdLine = new ArrayList<>();

    cmdLine.add("--spot");
    cmdLine.add(spot);

    cmdLine.add("--xoffset");
    cmdLine.add(String.valueOf(xOffset != null ? xOffset : 0));
    cmdLine.add("--yoffset");
    cmdLine.add(String.valueOf(yOffset != null ? yOffset : 0));

    cmdLine.add("--path");
    cmdLine.add(inQuotes(savePathDir.toString().replace(File.separatorChar, '/')));

    cmdLine.add("--name");
    cmdLine.add(name);

    cmdLine.add("--acqtype");
    cmdLine.add("single");

    if (laserOffsetX != null) {
      cmdLine.add("--laseroffsetx");
      cmdLine.add(String.valueOf(laserOffsetX));
    }
    if (laserOffsetY != null) {
      cmdLine.add("--laseroffsety");
      cmdLine.add(String.valueOf(laserOffsetY));
    }

    if (enableCeStepping && currentCeFile != null && currentCeFile.exists()) {
      cmdLine.add("--cetable");
      cmdLine.add(inQuotes(currentCeFile.toPath().toString().replace(File.separatorChar, '/')));
    }

    cmdLine.add("--precursorlist");
    cmdLine.add(inQuotes(precursorList.getAbsolutePath().replace(File.separatorChar, '/')));

    if (isolationWidth != null) {
      cmdLine.add("--isolationwidth");
      cmdLine.add(String.format("%.1f", isolationWidth));
    }

    return cmdLine;
  }

  public static int[] getOffsetsForIncrementCounter(int spotIncrement, int maxXIncrement,
      int xOffset, int yOffset) {
    final int finalOffsetX = spotIncrement % maxXIncrement * xOffset;
    final int finalOffsetY = (int) ((Math.floor(spotIncrement / (double) maxXIncrement)) * yOffset);
    return new int[]{finalOffsetX, finalOffsetY};
  }

  /**
   * @return True if the spots have the given distance or less.
   */
  public static boolean areSpotsWithinDistance(MaldiSpotInfo spot1, MaldiSpotInfo spot2,
      double distance) {
    return distance > getDistanceForSpots(spot1, spot2);
  }

  /**
   * @return True, if any of the spots are less than the distance away from the maldiSpotInfo.
   */
  public static boolean areSpotsWithinDistance(MaldiSpotInfo maldiSpotInfo, List<ImagingSpot> spots,
      double distance) {
    for (ImagingSpot spot : spots) {
      if (areSpotsWithinDistance(spot.spotInfo(), maldiSpotInfo, distance)) {
//        logger.finest("distance too low");
        return true;
      }
    }
    return false;
  }

  public static List<ImagingSpot> getSpotsWithinDistance(double distance, List<ImagingSpot> spots,
      MaldiSpotInfo maldiSpotInfo) {
    List<ImagingSpot> spotsWithinDistance = new ArrayList<>();
    for (ImagingSpot spot : spots) {
      if (areSpotsWithinDistance(spot.spotInfo(), maldiSpotInfo, distance)) {
        spotsWithinDistance.add(spot);
      }
    }

    return spotsWithinDistance;
  }

  public static List<Double> getSpotCollisionEnergies(List<ImagingSpot> spots) {
    return spots.stream().map(ImagingSpot::getCollisionEnergy).distinct().toList();
  }

  /**
   * @param minDistance       The minimum distance for two spots of the same collision energy to be
   *                          apart form another.
   * @param precursor         The current precursor, used to track the number of MS/MS per CE. Not
   *                          incremented.
   * @param spots             The spots currently selected for the precursor for MS/MS events.
   * @param spot              The spot in question.
   * @param collisionEnergies The collision energies to chose from.
   * @param numMsMs           The maximum number of MS/MS events per collision energy for this
   *                          precursor.
   * @return The collision energy with the lowest number of MS/MS events for the given precursor
   * that meets the distance criteria or null if the criteria are not met.
   */
  @Nullable
  public static Double getBestCollisionEnergyForSpot(double minDistance,
      MaldiTimsPrecursor precursor, List<ImagingSpot> spots, MaldiSpotInfo spot,
      Collection<Double> collisionEnergies, int numMsMs) {
    final List<ImagingSpot> spotsWithinDistance = TimsTOFAcquisitionUtils.getSpotsWithinDistance(
        minDistance, spots, spot);

    if (spotsWithinDistance.isEmpty()) { // we can use any energy, let's use the one with lowest MS/MS
      precursor.getCollisionEnergyWithFewestSpots();
    }

    final List<Double> spotCEs = TimsTOFAcquisitionUtils.getSpotCollisionEnergies(
        spotsWithinDistance);

    if (spotCEs.size() == collisionEnergies.size()) {
      return null; // no CE left for this spot
    }

    // get the CE with the lowest MsMs numbers and not in range
    final Optional<Entry<Double, Integer>> first = precursor.collisionEnergies().entrySet().stream()
        .sorted(Comparator.comparingInt(Entry::getValue))
        .filter(e -> e.getValue() < numMsMs && !spotCEs.contains(e.getKey())).findFirst();
    return first.map(Entry::getKey).orElse(null);
  }

  /**
   * @param minDistance       The minimum distance for two spots of the same collision energy to be
   *                          apart form another.
   * @param precursor         The current precursor, used to track the number of MS/MS per CE. Not
   *                          incremented.
   * @param spots             The spots currently selected for the precursor for MS/MS events.
   * @param spot              The spot in question.
   * @param collisionEnergies The collision energies to chose from.
   * @param numMsMs           The maximum number of MS/MS events per collision energy for this
   *                          precursor.
   * @return A list of collision energies that can be used for this spot. Empty list if no energies
   * meet the criteria.
   */
  @NotNull
  public static List<Double> getPossibleCollisionEnergiesForSpot(double minDistance,
      MaldiTimsPrecursor precursor, List<ImagingSpot> spots, MaldiSpotInfo spot,
      Collection<Double> collisionEnergies, int numMsMs) {
    final List<ImagingSpot> spotsWithinDistance = TimsTOFAcquisitionUtils.getSpotsWithinDistance(
        minDistance, spots, spot);

    if (spotsWithinDistance.isEmpty()) { // we can use any energy, let's use the one with lowest MS/MS
      return precursor.collisionEnergies().entrySet().stream()
          .filter(e -> e.getValue() < numMsMs && collisionEnergies.contains(e.getKey()))
          .sorted(Comparator.comparingInt(Entry::getValue)).map(Entry::getKey).toList();
    }

    final List<Double> spotCEs = TimsTOFAcquisitionUtils.getSpotCollisionEnergies(
        spotsWithinDistance);

    if (spotCEs.size() == collisionEnergies.size()) {
      return List.of(); // no CE left for this spot
    }

    // get the CE with the lowest MsMs numbers and not in range
    return precursor.collisionEnergies().entrySet().stream()
        .sorted(Comparator.comparingInt(Entry::getValue))
        .filter(e -> e.getValue() < numMsMs && !spotCEs.contains(e.getKey())).map(Entry::getKey)
        .toList();
  }

  public static double getDistanceForSpots(MaldiSpotInfo spot1, MaldiSpotInfo spot2) {
    var dx = spot2.xIndexPos() - spot1.xIndexPos();
    var dy = spot2.yIndexPos() - spot1.yIndexPos();

    return Math.sqrt(dx * dx + dy * dy);
  }

  public static double getRecommendedOneOverK0DistanceForSwitchTime(@NotNull final Frame frame) {
    final double switchTimeMs = 1.65;
    return getOneOverK0DistanceForSwitchTime(frame, switchTimeMs);
  }

  public static double getOneOverK0DistanceForSwitchTime(@NotNull final Frame frame,
      final double switchTimeMs) {
    final double oneOverK0Range = RangeUtils.rangeLength(frame.getMobilityRange());
    final double rampTimeMilliSeconds =
        frame.getNumberOfMobilityScans() * 0.1; // one scan per 100 µs
    final double oneOverK0perMs = oneOverK0Range / rampTimeMilliSeconds;
    return oneOverK0perMs * switchTimeMs;
  }
}
