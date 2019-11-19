/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.io.adap.mgfexport;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.adap.mgfexport.AdapMgfExportParameters.MzMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * Export of a feature cluster (ADAP) to mgf. Used in GC-GNPS
 * 
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class AdapMgfExportTask extends AbstractTask {
  private final String newLine = System.lineSeparator();
  //
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat mzNominalForm = new DecimalFormat("0");
  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  // seconds
  private NumberFormat rtsForm = new DecimalFormat("0.###");

  private final PeakList[] peakLists;
  private final File fileName;
  private final String plNamePattern = "{}";
  private final boolean fractionalMZ;
  private final String roundMode;
  private MzMode representativeMZ;
  private final int totalRows;
  private int finishedRows = 0;


  public AdapMgfExportTask(ParameterSet parameters) {
    this(parameters, parameters.getParameter(AdapMgfExportParameters.PEAK_LISTS).getValue()
        .getMatchingPeakLists());
  }

  public AdapMgfExportTask(ParameterSet parameters, PeakList[] peakLists) {
    this.peakLists = peakLists;
    totalRows = (int) Stream.of(peakLists).map(PeakList::getRows).count();

    this.fileName = parameters.getParameter(AdapMgfExportParameters.FILENAME).getValue();

    this.fractionalMZ = parameters.getParameter(AdapMgfExportParameters.FRACTIONAL_MZ).getValue();

    this.roundMode = parameters.getParameter(AdapMgfExportParameters.ROUND_MODE).getValue();
    this.representativeMZ =
        parameters.getParameter(AdapMgfExportParameters.REPRESENTATIVE_MZ).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows != 0 ? finishedRows / totalRows : 0;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(peakLists) + " to MGF file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Process feature lists
    for (PeakList peakList : peakLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Open file
      FileWriter writer;
      try {
        writer = new FileWriter(curFile);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        return;
      }

      try {
        exportPeakList(peakList, writer);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error while writing into file " + curFile + ": " + e.getMessage());
        return;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Close file
      try {
        writer.close();
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not close file " + curFile);
        return;
      }

      // If feature list substitution pattern wasn't found,
      // treat one feature list only
      if (!substitute)
        break;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private void exportPeakList(PeakList peakList, FileWriter writer) throws IOException {
    for (PeakListRow row : peakList.getRows()) {
      IsotopePattern ip = row.getBestIsotopePattern();
      if (ip == null)
        continue;

      exportRow(writer, row, ip);

      finishedRows++;
    }
  }

  private void exportRow(FileWriter writer, PeakListRow row, IsotopePattern ip) throws IOException {
    // data points of this cluster
    DataPoint[] dataPoints = ip.getDataPoints();
    if (!fractionalMZ)
      dataPoints = integerDataPoints(dataPoints, roundMode);
    // get m/z and rt
    double mz = getRepresentativeMZ(row, dataPoints);
    String retTimeInSeconds = rtsForm.format(row.getAverageRT() * 60);
    // write
    writer.write("BEGIN IONS" + newLine);
    writer.write("FEATURE_ID=" + row.getID() + newLine);
    writer.write("PEPMASS=" + formatMZ(mz) + newLine);
    writer.write("RTINSECONDS=" + retTimeInSeconds + newLine);
    writer.write("SCANS=" + row.getID() + newLine);

    // needs to be MSLEVEL=2 for GC-GNPS (even for GC-EI-MS data)
    writer.write("MSLEVEL=2" + newLine);
    writer.write("CHARGE=1+" + newLine);

    for (DataPoint point : dataPoints) {
      String line = formatMZ(point.getMZ()) + " " + intensityForm.format(point.getIntensity());
      writer.write(line + newLine);
    }

    writer.write("END IONS" + newLine);
    writer.write(newLine);
  }

  /**
   * Format as nominal or fractional
   * 
   * @param mz
   * @return
   */
  private String formatMZ(double mz) {
    return fractionalMZ ? mzForm.format(mz) : mzNominalForm.format(mz);
  }

  private double getRepresentativeMZ(PeakListRow row, DataPoint[] data) {
    final double mz;
    switch (representativeMZ) {
      case AS_IN_FEATURE_TABLE:
        mz = row.getAverageMZ();
        break;
      case HIGHEST_MZ:
        mz = Stream.of(data).max((a, b) -> Double.compare(a.getMZ(), b.getMZ()))
            .map(DataPoint::getMZ).orElse(row.getAverageMZ());
        break;
      case MAX_INTENSITY:
        mz = Stream.of(data).max((a, b) -> Double.compare(a.getIntensity(), b.getIntensity()))
            .map(DataPoint::getMZ).orElse(row.getAverageMZ());
        break;
      default:
        mz = row.getAverageMZ();
        break;
    }

    return mz;
  }

  /**
   * Round to nominal masses and select intensity
   * 
   * @param dataPoints
   * @param mode
   * @return
   */
  private DataPoint[] integerDataPoints(final DataPoint[] dataPoints, final String mode) {
    int size = dataPoints.length;

    Map<Double, Double> integerDataPoints = new HashMap<>();

    for (int i = 0; i < size; ++i) {
      double mz = Math.round(dataPoints[i].getMZ());
      double intensity = dataPoints[i].getIntensity();
      Double prevIntensity = integerDataPoints.get(mz);
      if (prevIntensity == null)
        prevIntensity = 0.0;

      switch (mode) {
        case AdapMgfExportParameters.ROUND_MODE_SUM:
          integerDataPoints.put(mz, prevIntensity + intensity);
          break;

        case AdapMgfExportParameters.ROUND_MODE_MAX:
          integerDataPoints.put(mz, Math.max(prevIntensity, intensity));
          break;
      }
    }

    DataPoint[] result = new DataPoint[integerDataPoints.size()];
    int count = 0;
    for (Entry<Double, Double> e : integerDataPoints.entrySet())
      result[count++] = new SimpleDataPoint(e.getKey(), e.getValue());

    return result;
  }
}
