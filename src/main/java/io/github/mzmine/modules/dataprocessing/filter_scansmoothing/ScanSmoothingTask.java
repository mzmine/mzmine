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

package io.github.mzmine.modules.dataprocessing.filter_scansmoothing;

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
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ScanSmoothingTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private List<Scan> scanNumbers;

  // User parameters
  private String suffix;
  private double timeSpan, minimumHeight;
  private int scanSpan;
  private int mzPoints;
  private double mzTol;
  private boolean removeOriginal;
  private ParameterSet parameters;
  private final MemoryMapStorage storage;
  RawDataFile newRDF = null;

  /**
   * @param dataFile
   * @param parameters
   * @param storage
   */
  public ScanSmoothingTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.dataFile = dataFile;

    this.timeSpan = parameters.getParameter(ScanSmoothingParameters.timeSpan).getValue();
    this.scanSpan = parameters.getParameter(ScanSmoothingParameters.scanSpan).getValue();
    this.mzTol = parameters.getParameter(ScanSmoothingParameters.mzTolerance).getValue();
    this.mzPoints = parameters.getParameter(ScanSmoothingParameters.mzPoints).getValue();
    this.minimumHeight = parameters.getParameter(ScanSmoothingParameters.minimumHeight).getValue();
    this.suffix = parameters.getParameter(ScanSmoothingParameters.suffix).getValue();
    this.removeOriginal = parameters.getParameter(ScanSmoothingParameters.removeOld).getValue();

    this.parameters = parameters;
    this.storage = storage;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Smoothing scans in " + dataFile;
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

    logger.info("Started Scan Smoothing on " + dataFile);

    scanNumbers = dataFile.getScanNumbers(1);
    totalScans = scanNumbers.size();

    RawDataFile newRDFW = null;
    int timepassed = 0;
    int mzpassed = 0;
    try {
      newRDFW = MZmineCore.createNewFile(dataFile.getName() + ' ' + suffix, null, storage);

      DataPoint mzValues[][] = null; // [relative scan][j value]
      int i, j, si, sj, ii, k, ssi, ssj;
      for (i = 0; i < totalScans; i++) {

        if (isCanceled())
          return;

        // Smoothing in TIME space
        Scan scan = scanNumbers.get(i);
        if (scan != null) {
          double rt = scan.getRetentionTime();
          DataPoint[] newDP = null;
          sj = si = i;
          ssi = ssj = i;
          if (timeSpan > 0 || scanSpan > 0) {
            double timeMZtol = Math.max(mzTol, 1e-5);
            for (si = i; si > 1; si--) {
              Scan scanS = scanNumbers.get(si - 1);
              if (scanS == null || scanS.getRetentionTime() < rt - timeSpan / 2) {
                break;
              }
            }
            for (sj = i; sj < totalScans - 1; sj++) {
              Scan scanS = scanNumbers.get(sj + 1);
              if (scanS == null || scanS.getRetentionTime() >= rt + timeSpan / 2) {
                break;
              }
            }
            ssi = i - (scanSpan - 1) / 2;
            ssj = i + (scanSpan - 1) / 2;
            if (ssi < 0) {
              ssj += -ssi;
              ssi = 0;
            }
            if (ssj >= totalScans) {
              ssi -= (ssj - totalScans + 1);
              ssj = totalScans - 1;
            }
            if (sj - si + 1 < scanSpan) {
              si = ssi;
              sj = ssj;
              // si = Math.min(si, ssi);
              // sj = Math.max(sj, ssj);
            }
            if (sj > si) {
              timepassed++;
              // Allocate
              if (mzValues == null || mzValues.length < sj - si + 1)
                mzValues = new DataPoint[sj - si + 1][];
              // Load Data Points
              for (j = si; j <= sj; j++) {
                Scan xscan = scanNumbers.get(j);
                mzValues[j - si] = ScanUtils.extractDataPoints(xscan);
              }
              // Estimate Averages
              ii = i - si;
              newDP = new DataPoint[mzValues[ii].length];
              for (k = 0; k < mzValues[ii].length; k++) {
                DataPoint dp = mzValues[ii][k];
                double mz = dp.getMZ();
                double intensidad = 0;
                if (dp.getIntensity() > 0) { // only process
                  // those > 0
                  double a = 0;
                  short c = 0;
                  int f = 0;
                  for (j = 0; j < mzValues.length; j++) {
                    // System.out.println(j);
                    if (mzValues[j].length > k
                        && Math.abs(mzValues[j][k].getMZ() - mz) < timeMZtol) {
                      f = k;
                    } else {
                      f = findFirstMass(mz, mzValues[j]);
                      if (Math.abs(mzValues[j][f].getMZ() - mz) > timeMZtol) {
                        f = -f;
                      }
                    }
                    if (f >= 0 && mzValues[j][f].getIntensity() >= minimumHeight) {
                      a += mzValues[j][f].getIntensity();
                      c++;
                    } else {
                      c = (short) (c + 0);
                    }
                  }
                  intensidad = c > 0 ? a / c : 0;
                }
                newDP[k] = new SimpleDataPoint(mz, intensidad);
              }
            }
          } else if (scan != null) {
            newDP = ScanUtils.extractDataPoints(scan);
          }

          // Smoothing in MZ space

          if ((mzTol > 0 || mzPoints > 0)) {
            mzpassed++;
            DataPoint[] updatedDP = new DataPoint[newDP.length];
            for (k = 0; k < newDP.length; k++) {
              double mz = newDP[k].getMZ();
              double intensidad = 0;
              if (newDP[k].getIntensity() > 0) {
                for (si = k; si > 0
                    && (newDP[si].getMZ() + mzTol >= mz || k - si <= mzPoints); si--);
                for (sj = k; sj < newDP.length - 1
                    && (newDP[sj].getMZ() - mzTol <= mz || sj - k <= mzPoints); sj++);
                double sum = 0;
                for (j = si; j <= sj; j++) {
                  sum += newDP[j].getIntensity();
                }
                intensidad = sum / (sj - si + 1);
              }
              updatedDP[k] = new SimpleDataPoint(mz, intensidad);
            }
            newDP = updatedDP;
          }

          // Register new smoothing data
          if (scan != null && newDP != null) {
            double[][] dp = DataPointUtils.getDataPointsAsDoubleArray(newDP);
            final SimpleScan newScan = new SimpleScan(newRDFW, scan, dp[0], dp[1]);
            newRDFW.addScan(newScan);
          }
        }
        processedScans++;
      }

      if (!isCanceled()) {
        newRDF = newRDFW;
        for (FeatureListAppliedMethod appliedMethod : dataFile.getAppliedMethods()) {
          newRDF.getAppliedMethods().add(appliedMethod);
        }
        newRDF.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
            ScanSmoothingModule.class, parameters, getModuleCallDate()));

        // Add the newly created file to the project
        project.addFile(newRDF);

        // Remove the original data file if requested
        if (removeOriginal) {
          project.removeFile(dataFile);
        }

        setStatus(TaskStatus.FINISHED);

        if (mzpassed + timepassed < totalScans / 2) {
          logger.warning("It seems that parameters were not properly set. Scans processed : time="
              + timepassed + ", mz=" + mzpassed);
        }

        logger.info("Finished Scan Smoothing on " + dataFile);

      }

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  static int findFirstMass(double mass, DataPoint mzValues[]) {
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
