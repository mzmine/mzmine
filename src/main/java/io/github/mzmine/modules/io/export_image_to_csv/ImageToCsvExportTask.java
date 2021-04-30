/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.io.export_image_to_csv;

import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_imzml.ImagingParameters;
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
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Exports imaging data from a {@link io.github.mzmine.datamodel.featuredata.IonTimeSeries<ImagingScan>}
 * to a csv file in a matrix format. Column indices represent the x-axis, row indices represent the
 * y-axis. The value matrix represents the intensity at the given spot.
 *
 * @author https://github.com/SteffenHeu
 */
public class ImageToCsvExportTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(ImageToCsvExportTask.class.getName());

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;
  private final String sep;

  private final File dir;
  private final Collection<ModularFeature> features;
  private int processed;

  public ImageToCsvExportTask(ParameterSet param,
      Collection<ModularFeature> features) {
    super(null);
    this.dir = param.getParameter(ImageToCsvExportParameters.dir).getValue();
    this.sep = param.getParameter(ImageToCsvExportParameters.delimiter).getValue().trim();
    this.features = features;

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
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
    final List<ImagingRawDataFile> distinctFiles = features
        .stream().map(ModularFeature::getRawDataFile).distinct()
        .filter(file -> file instanceof ImagingRawDataFile).map(file -> (ImagingRawDataFile) file)
        .toList();

    // create a buffer with maximum dimensions
    final int absMaxX = distinctFiles.stream()
        .mapToInt(file -> file.getImagingParam().getMaxNumberOfPixelX()).max().orElse(0) + 1;
    final int absMaxY = distinctFiles.stream()
        .mapToInt(file -> file.getImagingParam().getMaxNumberOfPixelY()).max().orElse(0) + 1;

    // invert x and y so we can easily loop while saving
    final double[][] dataMatrix = new double[absMaxY][absMaxX];

    for (ModularFeature f : features) {
      if (isCanceled()) {
        return;
      }

      ArrayUtils.fill2D(dataMatrix, 0d);

      final IonTimeSeries<? extends Scan> featureData = f.getFeatureData();
      final IonTimeSeries<? extends ImagingScan> data;
      try {
        data = (IonTimeSeries<? extends ImagingScan>) featureData;
      } catch (ClassCastException e) {
        logger.info("Cannot cast feature data to IonTimeSeries<? extends ImagingScan> for feature "
            + FeatureUtils.featureToString(f));
        continue;
      }

      final ImagingRawDataFile rawDataFile = (ImagingRawDataFile) f.getRawDataFile();
      final ImagingParameters imagingParam = rawDataFile.getImagingParam();

      for (int i = 0; i < data.getNumberOfValues(); i++) {
        ImagingScan scan = data.getSpectrum(i);
        // x and y inverted!
        dataMatrix[scan.getCoordinates().getY()][scan.getCoordinates().getX()] = data
            .getIntensity(i);
      }

      if (!writeImageToCsv(dataMatrix, f, dir)) {
        return;
      }
      processed++;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private boolean writeImageToCsv(double[][] dataMatrix, ModularFeature f, File directory) {
    // Cleanup from illegal filename characters
    final String cleanFlName = f.getFeatureList().getName().replaceAll("[^a-zA-Z0-9.-]", "_");
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException(directory.getAbsolutePath() + " is not a directory.");
    }
    final File flDir = new File(directory.getAbsolutePath() + File.separator + cleanFlName);
    flDir.mkdirs();

    String newFilename =
        f.getRawDataFile().getName() + "_mz-" + mzFormat.format(f.getMZ());
    if (f.getMobility() != null && Float.compare(f.getMobility(), 0f) != 0) {
      newFilename += "_mobility-" + mobilityFormat.format(f.getMobility());
    }
    newFilename += "_" + getImageParamString((ImagingRawDataFile) f.getRawDataFile());
    newFilename = newFilename.replace(".", "i");
    newFilename += ".csv";

    final File file = FileAndPathUtil.getUniqueFilename(flDir, newFilename);

    try (BufferedWriter writer = Files
        .newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {

      for (int y = 0; y < dataMatrix.length; y++) {
        StringBuilder b = new StringBuilder();
        for (int x = 0; x < dataMatrix[y].length; x++) {
          if (Double.compare(dataMatrix[y][x], 0d) != 0) {
            b.append(dataMatrix[y][x]);
          }
          if(x < dataMatrix[y].length) {
            b.append(sep);
          }
        }
        writer.append(b.toString());
        writer.newLine();
      }

    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + file + " for writing.");
      return false;
    }
    return true;
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
}
