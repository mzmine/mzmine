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

package io.github.mzmine.modules.dataprocessing.filter_alignscans;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlignScansTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private Scan[] scanNumbers;

  // User parameters
  private String suffix;
  private double minimumHeight;
  private int scanSpan, mzSpan;
  private boolean logScale = false;
  private boolean removeOriginal;
  private ParameterSet parameters;

  /**
   * @param dataFile
   * @param parameters
   * @param storage
   */
  public AlignScansTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    this.scanSpan = parameters.getParameter(AlignScansParameters.scanSpan).getValue();
    this.mzSpan = parameters.getParameter(AlignScansParameters.mzSpan).getValue();
    this.minimumHeight = parameters.getParameter(AlignScansParameters.minimumHeight).getValue();
    this.suffix = parameters.getParameter(AlignScansParameters.suffix).getValue();
    this.removeOriginal = parameters.getParameter(AlignScansParameters.removeOld).getValue();
    this.logScale = parameters.getParameter(AlignScansParameters.logTransform).getValue();
    this.parameters = parameters;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Aligning scans in " + dataFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    else
      return (double) processedScans / totalScans;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started Scan Alignment on " + dataFile);

    scanNumbers = dataFile.getScanNumbers(1).toArray(Scan[]::new);
    totalScans = scanNumbers.length;

    RawDataFile newRDFW = null;
    try {
      newRDFW = MZmineCore.createNewFile(dataFile.getName() + ' ' + suffix, null, getMemoryMapStorage());

      DataPoint[][] mzValues = null; // [relative scan][j value]
      int i, j, si, sj, ii, k, shift, ks;
      int shiftedScans[] = new int[mzSpan * 2 + 1];
      for (i = 0; i < totalScans; i++) {

        if (isCanceled())
          return;

        Scan scan = scanNumbers[i];
        si = Math.max(0, i - scanSpan);
        sj = si + 2 * scanSpan;
        if (sj >= totalScans) {
          si = Math.max(0, si - (sj - totalScans + 1));
          sj = si + 2 * scanSpan;
        }
        if (scan != null) {
          // Allocate
          if (mzValues == null || mzValues.length < sj - si + 1)
            mzValues = new DataPoint[sj - si + 1][];
          // Load Data Points
          for (j = si; j <= sj; j++) {
            Scan xscan = scanNumbers[j];
            mzValues[j - si] = ScanUtils.extractDataPoints(xscan);
          }
          // Estimate Correlations
          ii = i - si;
          DataPoint[] newDP = new DataPoint[mzValues[ii].length];
          int maxShift = 0;
          double maxCorrelation = 0;
          int ndp = mzValues[ii].length;
          // System.out.print("Scan="+i);
          for (shift = -mzSpan; shift <= mzSpan; shift++) {
            PearsonCorrelation thisShift = new PearsonCorrelation();
            for (k = 0; k < ndp; k++) {
              ks = k + shift;
              if (ks >= 0 && ks < ndp && mzValues[ii][ks].getIntensity() >= minimumHeight) {
                DataPoint dp = mzValues[ii][k];
                double mz = dp.getMZ();
                int f = 0;
                for (j = 0; j < mzValues.length; j++) {
                  // System.out.println(j);
                  if (j != ii) {
                    if (mzValues[j].length > k && Math.abs(mzValues[j][k].getMZ() - mz) < 1e-10) {
                      f = k;
                    } else {
                      f = findFirstMass(mz, mzValues[j]);
                      if (Math.abs(mzValues[j][f].getMZ() - mz) > 1e-10) {
                        f = -f;
                      }
                    }
                    if (f >= 0) {
                      if (logScale) {
                        thisShift.enter(Math.log(mzValues[j][f].getIntensity()),
                            Math.log(mzValues[ii][ks].getIntensity()));
                      } else {
                        thisShift.enter(mzValues[j][f].getIntensity(),
                            mzValues[ii][ks].getIntensity());
                      }
                    }
                  }
                }
              }
            }
            // System.out.print(", shift="+shift+",
            // correlation="+Math.round(thisShift.correlation()*1000)/1000.0);
            if (thisShift.correlation() > maxCorrelation) {
              maxShift = shift;
              maxCorrelation = thisShift.correlation();
            }
            // newDP[k] = new SimpleDataPoint(mz, c > 0 ? a/c : 0);
          }
          // Copy DataPoints with maxShift as the shift
          shift = maxShift;
          // System.out.println("\nScan="+i+", Shift="+maxShift+",
          // Correlation="+maxCorrelation);
          shiftedScans[maxShift + mzSpan]++;
          for (k = 0; k < ndp; k++) {
            ks = k + shift;
            if (ks >= 0 && ks < ndp) {
              newDP[k] =
                  new SimpleDataPoint(mzValues[ii][k].getMZ(), mzValues[ii][ks].getIntensity());
            } else {
              newDP[k] = new SimpleDataPoint(mzValues[ii][k].getMZ(), 0);
            }
          }
          double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(newDP);
          final SimpleScan newScan = new SimpleScan(newRDFW, scan, dp[0], dp[1]);
          newRDFW.addScan(newScan);
        }

        processedScans++;
      }

      if (!isCanceled()) {

        // Add the newly created file to the project
        for (FeatureListAppliedMethod appliedMethod : dataFile.getAppliedMethods()) {
          newRDFW.getAppliedMethods().add(appliedMethod);
        }

        newRDFW.getAppliedMethods()
            .add(new SimpleFeatureListAppliedMethod(AlignScansModule.class, parameters, getModuleCallDate()));
        project.addFile(newRDFW);

        // Remove the original data file if requested
        if (removeOriginal) {
          project.removeFile(dataFile);
        }

        setStatus(TaskStatus.FINISHED);

        String shifts = "";
        for (i = -mzSpan; i <= mzSpan; i++) {
          shifts = shifts + i + ":" + shiftedScans[i + mzSpan] + " | ";
        }
        logger.info("Finished scan alignment on " + dataFile + ". Scans per shift = " + shifts);

      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  int findFirstMass(double mass, DataPoint mzValues[]) {
    int l = 0;
    int r = mzValues.length - 1;
    int mid = 0;
    while (l < r) {
      mid = (r + l) / 2;
      if (mzValues[mid].getMZ() > mass) {
        r = mid - 1;
      } else if (mzValues[mid].getMZ() < mass) {
        l = mid + 1;
      } else {
        r = mid;
      }
    }
    return l;
  }

}


class PearsonCorrelation {

  private int count; // Number of numbers that have been entered.
  private double sumX = 0;
  private double sumY = 0;
  private double sumXX = 0;
  private double sumYY = 0;
  private double sumXY = 0;

  void enter(double x, double y) {
    count++;
    sumX += x;
    sumY += y;
    sumXX += x * x;
    sumYY += y * y;
    sumXY += x * y;
  }

  int getCount() {
    return count;
  }

  double meanX() {
    return sumX / count;
  }

  double meanY() {
    return sumY / count;
  }

  double correlation() {

    double numerator = count * sumXY - sumX * sumY;
    int n = count; // here always use the same ... (count > 50 ? count - 1 :
    // count);
    double denominator = Math.sqrt(n * sumXX - sumX * sumX) * Math.sqrt(n * sumYY - sumY * sumY);
    double c = (count < 3 ? 0 : numerator / denominator);
    return c;
  }
}
