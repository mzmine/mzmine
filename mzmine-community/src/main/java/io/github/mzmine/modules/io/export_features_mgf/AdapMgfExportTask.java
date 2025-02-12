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

package io.github.mzmine.modules.io.export_features_mgf;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.export_features_mgf.AdapMgfExportParameters.MzMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.ScanUtils.IntegerMode;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

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

  private final FeatureList[] featureLists;
  private final File fileName;
  private final String plNamePattern = "{}";
  private final boolean fractionalMZ;
  private final IntegerMode roundMode;
  private MzMode representativeMZ;
  private final int totalRows;
  private int finishedRows = 0;

  public AdapMgfExportTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    this(parameters, parameters.getParameter(AdapMgfExportParameters.FEATURE_LISTS).getValue()
        .getMatchingFeatureLists(), moduleCallDate);
  }

  public AdapMgfExportTask(ParameterSet parameters, FeatureList[] featureLists,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.featureLists = featureLists;
    totalRows = (int) Stream.of(featureLists).map(FeatureList::getRows).count();

    this.fileName = parameters.getParameter(AdapMgfExportParameters.FILENAME).getValue();

    this.fractionalMZ = parameters.getParameter(AdapMgfExportParameters.FRACTIONAL_MZ).getValue();

    this.roundMode = parameters.getParameter(AdapMgfExportParameters.ROUND_MODE).getValue();
    this.representativeMZ = parameters.getParameter(AdapMgfExportParameters.REPRESENTATIVE_MZ)
        .getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows != 0 ? finishedRows / totalRows : 0;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists) + " to MGF file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Process feature lists
    for (FeatureList featureList : featureLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath()
            .replaceAll(Pattern.quote(plNamePattern), cleanPlName);
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
        exportFeatureList(featureList, writer);
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
      if (!substitute) {
        break;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void exportFeatureList(FeatureList featureList, FileWriter writer) throws IOException {
    for (FeatureListRow row : featureList.getRows()) {
      Scan ip = row.getMostIntenseFragmentScan();
      if (ip == null) {
        continue;
      }

      exportRow(writer, row, ip);

      finishedRows++;
    }
  }

  private void exportRow(FileWriter writer, FeatureListRow row, Scan ip)
      throws IOException {
    // data points of this cluster
    DataPoint dataPoints[] = ScanUtils.extractDataPoints(ip);
    if (!fractionalMZ) {
      dataPoints = ScanUtils.integerDataPoints(dataPoints, roundMode);
    }
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
    writer.write("Num peaks="+ dataPoints.length + newLine);


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

  private double getRepresentativeMZ(FeatureListRow row, DataPoint[] data) {
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
}
