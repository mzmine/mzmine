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

package io.github.mzmine.modules.io.export_image_csv;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_image_csv.ImageToCsvExportParameters.HandleMissingValues;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Exports imaging data from a {@link io.github.mzmine.datamodel.featuredata.IonTimeSeries<ImagingScan>}
 * to a csv file in a matrix format. Column indices represent the x-axis, row indices represent the
 * y-axis. The value matrix represents the intensity at the given spot.
 *
 * @author https://github.com/SteffenHeu
 */
public class ImageToCsvExportTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(ImageToCsvExportTask.class.getName());

  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final String sep;

  private final File dir;
  private final Collection<ModularFeature> features;
  private final HandleMissingValues handleMissingSpectra;
  private final HandleMissingValues handleMissingSignals;
  private final Boolean normalize;

  private int processed;

  public ImageToCsvExportTask(ParameterSet param, Collection<ModularFeature> features,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.dir = param.getParameter(ImageToCsvExportParameters.dir).getValue();
    this.sep = param.getParameter(ImageToCsvExportParameters.delimiter).getValue().trim();
    normalize = param.getParameter(ImageToCsvExportParameters.normalize).getValue();
    handleMissingSpectra = param.getParameter(ImageToCsvExportParameters.handleMissingSpectra)
        .getValue();
    handleMissingSignals = param.getParameter(ImageToCsvExportParameters.handleMissingSignals)
        .getValue();
    this.features = features;

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  }

  public static String getImageParamString(ImagingRawDataFile file) {
    StringBuilder b = new StringBuilder();
    b.append((int) file.getImagingParam().getLateralWidth());
    b.append("um(");
    b.append(file.getImagingParam().getMaxNumberOfPixelX());
    b.append("px) x ");
    b.append((int) file.getImagingParam().getLateralHeight());
    b.append("um(");
    b.append(file.getImagingParam().getMaxNumberOfPixelY());
    b.append("px)");
    return b.toString();
  }

  @Override
  public String getTaskDescription() {
    return "Exporting image of feature " + processed + "/" + features.size();
  }

  @Override
  public double getFinishedPercentage() {
    return processed / (double) features.size();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.fine(() -> "Determining maximum export dimensions...");
    final List<ImagingRawDataFile> distinctFiles = features.stream()
        .map(ModularFeature::getRawDataFile).distinct()
        .filter(file -> file instanceof ImagingRawDataFile).map(file -> (ImagingRawDataFile) file)
        .toList();

    // export all features for a specific raw file in the maximum dimension of this file
    for (ImagingRawDataFile raw : distinctFiles) {
      final int absMaxX = raw.getImagingParam().getMaxNumberOfPixelX() + 1;
      final int absMaxY = raw.getImagingParam().getMaxNumberOfPixelY() + 1;

      // invert x and y so we can easily loop while saving
      final double[][] dataMatrix = new double[absMaxY][absMaxX];

      for (ModularFeature f : features) {
        if (isCanceled()) {
          return;
        }

        // only features for this raw data file to have the same dimensions per file
        if (!raw.equals(f.getRawDataFile())) {
          continue;
        }

        // track missing data points with Double.NaN value
        ArrayUtils.fill2D(dataMatrix, Double.NaN);

        final IonTimeSeries<? extends Scan> featureData = f.getFeatureData();
        final IonTimeSeries<? extends ImagingScan> data;
        try {
          if (normalize) {
            final ImageNormalization imageNormalization = MZmineCore.getConfiguration()
                .getImageNormalization();
            data = imageNormalization.normalize((IonTimeSeries<ImagingScan>) featureData,
                (List<ImagingScan>) f.getFeatureList().getSeletedScans(f.getRawDataFile()), null);
          } else {
            data = (IonTimeSeries<? extends ImagingScan>) featureData;
          }
        } catch (ClassCastException e) {
          logger.info(
              "Cannot cast feature data to IonTimeSeries<? extends ImagingScan> for feature "
                  + FeatureUtils.featureToString(f));
          continue;
        }

        double minimumIntensity = Double.MAX_VALUE;
        // insert all values
        for (int i = 0; i < data.getNumberOfValues(); i++) {
          ImagingScan scan = data.getSpectrum(i);
          double intensity = data.getIntensity(i);
          // x and y inverted!
          int y = scan.getCoordinates().getY();
          int x = scan.getCoordinates().getX();
          dataMatrix[y][x] = intensity;

          if (minimumIntensity > intensity) {
            minimumIntensity = intensity;
          }
        }
        if (Double.compare(minimumIntensity, Double.MAX_VALUE) == 0) {
          minimumIntensity = 0;
        }
        // insert missing values for existing spectra
        handleMissingSignals(raw, dataMatrix, minimumIntensity);

        // handle missing spectra: Double.NaN, 0, or minimum intensity
        handleMissingSpectra(dataMatrix, minimumIntensity);

        if (!writeImageToCsv(dataMatrix, f, dir)) {
          return;
        }
        processed++;
      }
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Handle missing values (Double.NaN) for existing spectra.
   *
   * @param raw              the data file
   * @param dataMatrix       [y][x] data matrix. Missing values marked by Double.NaN
   * @param minimumIntensity the minimum signal intensity that is used if handleMissingSignals is
   *                         {@link HandleMissingValues#REPLACE_BY_LOWEST_VALUE}
   */
  private void handleMissingSignals(ImagingRawDataFile raw, double[][] dataMatrix,
      double minimumIntensity) {
    if (handleMissingSignals.equals(HandleMissingValues.LEAVE_EMPTY)) {
      return;
    }
    for (Scan scan : raw.getScans()) {
      if (scan instanceof ImagingScan imagingScan) {
        int y = imagingScan.getCoordinates().getY();
        int x = imagingScan.getCoordinates().getX();
        if (Double.isNaN(dataMatrix[y][x])) {
          dataMatrix[y][x] = replaceMissingValue(handleMissingSignals, minimumIntensity);
        }
      }
    }
  }

  /**
   * Handle missing spectra (Double.NaN) due to irregular imaging shapes or filtering.
   *
   * @param dataMatrix       [y][x] data matrix. Missing values marked by Double.NaN
   * @param minimumIntensity the minimum signal intensity that is used if handleMissingSignals is
   *                         {@link HandleMissingValues#REPLACE_BY_LOWEST_VALUE}
   */
  private void handleMissingSpectra(double[][] dataMatrix, double minimumIntensity) {
    if (handleMissingSpectra.equals(HandleMissingValues.LEAVE_EMPTY)) {
      return;
    }
    for (int y = 0; y < dataMatrix.length; y++) {
      for (int x = 0; x < dataMatrix[y].length; x++) {
        if (Double.isNaN(dataMatrix[y][x])) {
          dataMatrix[y][x] = replaceMissingValue(handleMissingSpectra, minimumIntensity);
        }
      }
    }
  }

  private double replaceMissingValue(HandleMissingValues handle, double minimumIntensity) {
    return switch (handle) {
      case LEAVE_EMPTY -> Double.NaN;
      case REPLACE_BY_ZERO -> 0d;
      case REPLACE_BY_LOWEST_VALUE -> minimumIntensity;
    };
  }

  private boolean writeImageToCsv(double[][] dataMatrix, ModularFeature f, File directory) {
    // Cleanup from illegal filename characters
    final String cleanFlName = f.getFeatureList().getName().replaceAll("[^a-zA-Z0-9.-]", "_");
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory.");
    }
    final File flDir = new File(directory.getAbsolutePath() + File.separator + cleanFlName);
    try {
      Files.createDirectories(flDir.toPath());
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage("Could not create directories for " + flDir.getAbsolutePath());
      return false;
    }

    String newFilename = f.getRawDataFile().getName() + "_mz-" + mzFormat.format(f.getMZ());
    if (f.getMobility() != null && Float.compare(f.getMobility(), 0f) != 0) {
      newFilename += "_mobility-" + mobilityFormat.format(f.getMobility());
    }
    newFilename += "_" + getImageParamString((ImagingRawDataFile) f.getRawDataFile());
    newFilename = newFilename.replace(".", "i");
    newFilename += ".csv";

    final File file = FileAndPathUtil.getUniqueFilename(flDir, newFilename);

    try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {

      for (int y = 0; y < dataMatrix.length; y++) {
        StringBuilder b = new StringBuilder();
        for (int x = 0; x < dataMatrix[y].length; x++) {
          double value = dataMatrix[y][x];
          if (!Double.isNaN(value)) {
            b.append(value);
          }
          if (x < dataMatrix[y].length - 1) {
            b.append(sep);
          }
        }
        writer.append(b.toString());
        if (y < dataMatrix.length - 1) {
          writer.newLine();
        }
      }

    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + file + " for writing.");
      return false;
    }
    return true;
  }
}
