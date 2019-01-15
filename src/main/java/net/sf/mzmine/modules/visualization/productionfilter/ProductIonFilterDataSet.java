/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.productionfilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.XYDataset;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;



class ProductIonFilterDataSet extends AbstractXYDataset implements Task, XYToolTipGenerator {

  private static final long serialVersionUID = 1L;

  private RawDataFile rawDataFile;

  private Range<Double> totalMZRange;

  private Object xAxisType;
  private int scanNumbers[], totalScans, processedScans;

  private MZTolerance mzDifference;
  private List<Double> targetedMZ_List;
  private List<Double> targetedNF_List;
  private File fileName;
  private Double basePeakPercent;

  private TaskStatus status = TaskStatus.WAITING;

  private HashMap<Integer, Vector<ProductIonFilterDataPoint>> dataSeries;

  private ProductIonFilterVisualizerWindow visualizer;

  private static int RAW_LEVEL = 0;
  private static int PRECURSOR_LEVEL = 1;
  private static int NEUTRALLOSS_LEVEL = 2;


  ProductIonFilterDataSet(RawDataFile rawDataFile, Object xAxisType, Range<Double> rtRange,
      Range<Double> mzRange, ProductIonFilterVisualizerWindow visualizer, MZTolerance mzDifference,
      List<Double> targetedMZ_List, List<Double> targetedNF_List, Double basePeakPercent,
      File fileName) {

    this.rawDataFile = rawDataFile;

    totalMZRange = mzRange;

    this.xAxisType = xAxisType;
    this.visualizer = visualizer;

    // mzDifference is maximum difference allowed between selected product m/z values and scan m/z
    // value
    this.mzDifference = mzDifference;
    this.targetedMZ_List = targetedMZ_List;
    this.targetedNF_List = targetedNF_List;

    // output filename
    this.fileName = fileName;

    // Percent of base peak of which product ions must be above in order to include in analysis
    this.basePeakPercent = basePeakPercent / 100;

    // get MS/MS scans
    scanNumbers = rawDataFile.getScanNumbers(2, rtRange);

    totalScans = scanNumbers.length;

    dataSeries = new HashMap<Integer, Vector<ProductIonFilterDataPoint>>();

    dataSeries.put(RAW_LEVEL, new Vector<ProductIonFilterDataPoint>(totalScans));
    dataSeries.put(PRECURSOR_LEVEL, new Vector<ProductIonFilterDataPoint>(totalScans));
    dataSeries.put(NEUTRALLOSS_LEVEL, new Vector<ProductIonFilterDataPoint>(totalScans));

  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);
    processedScans = 0;

    // dataList that will contain output m/z values, RT, and scan number for ID, ##for use in
    // targeted peak detection
    List<String> dataList = new ArrayList<String>();

    // in house generated list, used to output each precursor/product ion m/z for plotting in R
    List<String> dataListVisual = new ArrayList<String>();

    for (int scanNumber : scanNumbers) {

      // Cancel?
      if (status == TaskStatus.CANCELED)
        return;

      Scan scan = rawDataFile.getScan(scanNumber);

      // check parent m/z
      if (!totalMZRange.contains(scan.getPrecursorMZ())) {
        continue;
      }


      // get m/z and intensity values
      DataPoint scanDataPoints[] = scan.getDataPoints();

      // skip empty scans
      if (scan.getHighestDataPoint() == null) {
        processedScans++;
        continue;
      }

      // topPeaks will contain indexes to mzValues in scan above a threshold defined as : 'scan
      // basePeak Intensity' * percent of base Peak to include
      List<Integer> topPeaksList = new ArrayList<Integer>();
      double highestIntensity = scan.getHighestDataPoint().getIntensity() * basePeakPercent;

      for (int i = 0; i < scanDataPoints.length; i++) {
        // Cancel?
        if (status == TaskStatus.CANCELED)
          return;

        if ((scanDataPoints[i].getIntensity()) > highestIntensity) {
          // add the peaks
          topPeaksList.add(i);
        }
      }

      // Transfer topPeakList over to array
      Integer[] topPeaks = topPeaksList.toArray(new Integer[topPeaksList.size()]);

      // Default set to pass scan and not add to list
      boolean pass = false;

      /**
       * Depending on filter conditions these if statements will filter based off of product m/z or
       * neutral loss or both within a scan. Pass becomes set to true if filter conditions are met
       * and scan is added to output file and visual plot
       */

      // Filter based off both m/z and neutral loss if both are not equal to 0
      if (targetedMZ_List.get(0) != 0 && targetedNF_List.get(0) != 0) {
        boolean passA = false;
        boolean passB = false;
        boolean[] booleanValuesA = new boolean[targetedMZ_List.size()];
        boolean[] booleanValuesB = new boolean[targetedNF_List.size()];

        // scan through each m/z within scan m/z peaks
        for (int h = 0; h < topPeaks.length; h++) {
          // Cancel?
          if (status == TaskStatus.CANCELED)
            return;

          int peakIndex = topPeaks[h];
          if (peakIndex < 0)
            break;
          double neutralLoss = scan.getPrecursorMZ() - scanDataPoints[peakIndex].getMZ();

          // scan for all m/z values if more than one, set pass to true if all m/z values are found
          for (int j = 0; j < targetedMZ_List.size(); j++) {
            // Cancel?
            if (status == TaskStatus.CANCELED)
              return;

            if (mzDifference.getToleranceRange(targetedMZ_List.get(j))
                .contains(scanDataPoints[peakIndex].getMZ()) == true) {
              booleanValuesA[j] = true;
            }
          }

          if (isAllTrue(booleanValuesA)) {
            passA = true;
          }

          // scan for all neutral loss values if more than one, set pass to true if all netural loss
          // values are found
          for (int j = 0; j < targetedNF_List.size(); j++) {
            // Cancel?
            if (status == TaskStatus.CANCELED)
              return;

            if (mzDifference.getToleranceRange(targetedNF_List.get(j))
                .contains(neutralLoss) == true) {
              booleanValuesB[j] = true;
            }
          }
          if (isAllTrue(booleanValuesB)) {
            passB = true;
          }

        }
        // if both m/z and neutral loss pass, then total pass becomes set to true, and scan is added
        if (passA && passB) {
          pass = true;
        }

        // if only m/z requirements set, search for m/z and set to pass if found in scan
      } else if (targetedMZ_List.get(0) != 0) {
        boolean[] booleanValues = new boolean[targetedMZ_List.size()];
        for (int h = 0; h < topPeaks.length; h++) {
          int peakIndex = topPeaks[h];
          if (peakIndex < 0)
            break;
          for (int j = 0; j < targetedMZ_List.size(); j++) {
            // Cancel?
            if (status == TaskStatus.CANCELED)
              return;

            if (mzDifference.getToleranceRange(targetedMZ_List.get(j))
                .contains(scanDataPoints[peakIndex].getMZ()) == true) {
              booleanValues[j] = true;
            }
          }
          if (isAllTrue(booleanValues)) {
            pass = true;
          }
        }

        // scan for n/f if both are not searched for and m/z is not searched for
      } else if (targetedNF_List.get(0) != 0) {
        boolean[] booleanValues = new boolean[targetedMZ_List.size()];
        for (int h = 0; h < topPeaks.length; h++) {
          // Cancel?
          if (status == TaskStatus.CANCELED)
            return;

          int peakIndex = topPeaks[h];
          if (peakIndex < 0)
            break;
          double neutralLoss = scan.getPrecursorMZ() - scanDataPoints[peakIndex].getMZ();
          for (int j = 0; j < targetedNF_List.size(); j++) {
            // Cancel?
            if (status == TaskStatus.CANCELED)
              return;

            if (mzDifference.getToleranceRange(targetedNF_List.get(j))
                .contains(neutralLoss) == true) {
              booleanValues[j] = true;
            }
          }
          if (isAllTrue(booleanValues)) {
            pass = true;
          }

        }

        // If no requirements set, simply ouptut all scans
      } else {
        pass = true;
      }
      // If pass is set to true, include scan in output file and visual plot
      if (pass == true) {

        // Add all data points to visual plot and output file from scan
        for (int i = 0; i < topPeaks.length; i++) {
          // Cancel?
          if (status == TaskStatus.CANCELED)
            return;

          int peakIndex = topPeaks[i];

          // if we have a very few peaks, the array may not be full
          if (peakIndex < 0)
            break;

          ProductIonFilterDataPoint newPoint =
              new ProductIonFilterDataPoint(scanDataPoints[peakIndex].getMZ(), scan.getScanNumber(),
                  scan.getPrecursorMZ(), scan.getPrecursorCharge(), scan.getRetentionTime());

          dataSeries.get(0).add(newPoint);

          // Grab product ion, precursor ion, and retention time for sending to output file
          String temp = Double.toString(scan.getPrecursorMZ()) + ","
              + Double.toString(scanDataPoints[peakIndex].getMZ()) + ","
              + Double.toString(scan.getRetentionTime());
          // add to output file
          dataListVisual.add(temp);
        }

        // add precursor m/z, retention time, and scan number to output .csv file
        String dataMZ = Double.toString(scan.getPrecursorMZ());
        String dataRT = Double.toString(scan.getRetentionTime());
        String dataNM = Double.toString(scan.getScanNumber());
        String temp = dataMZ + "," + dataRT + "," + dataNM;

        dataList.add(temp);
      }

      processedScans++;
    }

    // Write output to csv file - for targeted peak detection module.
    try {
      // Cancel?
      if (status == TaskStatus.CANCELED)
        return;

      String namePattern = "{}";
      File curFile = fileName;

      if (fileName.getPath().contains(namePattern)) {

        String cleanPlName = rawDataFile.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath().replaceAll(Pattern.quote(namePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      FileWriter writer = new FileWriter(curFile);

      String collect = dataList.stream().collect(Collectors.joining("\n"));

      writer.write(collect);
      writer.close();

    } catch (IOException e) {
      System.out.print("Could not output to file");
      System.out.print(e.getStackTrace());

      fireDatasetChanged();
      setStatus(TaskStatus.FINISHED);
    }

    // write output to csv file - for visual plotting in R. has product ion, precursor ion m/z, and
    // retention times.
    try {
      // Cancel?
      if (status == TaskStatus.CANCELED)
        return;

      String namePattern = "{}";
      File curFile = fileName;

      String newFilenameTemp = fileName.getPath().replaceAll(".csv", "_data.csv");
      curFile = new File(newFilenameTemp);

      if (fileName.getPath().contains(namePattern)) {

        String cleanPlName = rawDataFile.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename = fileName.getPath().replaceAll(Pattern.quote(namePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      FileWriter writer = new FileWriter(curFile);

      String collect = dataListVisual.stream().collect(Collectors.joining("\n"));

      writer.write(collect);
      writer.close();

    } catch (IOException e) {
      System.out.print("Could not output to file");
      System.out.print(e.getStackTrace());

      fireDatasetChanged();
      setStatus(TaskStatus.FINISHED);
    }



    fireDatasetChanged();
    setStatus(TaskStatus.FINISHED);

  }

  public void updateOnRangeDataPoints(String rangeType) {

    ProductIonFilterPlot plot = visualizer.getPlot();
    Range<Double> prRange = plot.getHighlightedPrecursorRange();
    Range<Double> nlRange = plot.getHighlightedNeutralLossRange();

    // Set type of search
    int level = NEUTRALLOSS_LEVEL;
    if (rangeType.equals("HIGHLIGHT_PRECURSOR"))
      level = PRECURSOR_LEVEL;

    // Clean previous selection
    dataSeries.get(level).clear();

    ProductIonFilterDataPoint point;
    boolean b = false;
    for (int i = 0; i < dataSeries.get(RAW_LEVEL).size(); i++) {
      point = dataSeries.get(RAW_LEVEL).get(i);
      // Verify if the point is on range
      if (level == PRECURSOR_LEVEL)
        b = prRange.contains(point.getPrecursorMass());
      else
        b = nlRange.contains(point.getProductMZ());
      if (b)
        dataSeries.get(level).add(point);
    }

    fireDatasetChanged();
  }

  /**
   * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesCount()
   */
  public int getSeriesCount() {
    return dataSeries.size();
  }

  /**
   * @see org.jfree.data.general.AbstractSeriesDataset#getSeriesKey(int)
   */
  public Comparable<Integer> getSeriesKey(int series) {
    return series;
  }

  /**
   * @see org.jfree.data.xy.XYDataset#getItemCount(int)
   */
  public int getItemCount(int series) {
    return dataSeries.get(series).size();
  }

  /**
   * @see org.jfree.data.xy.XYDataset#getX(int, int)
   */
  public Number getX(int series, int item) {
    ProductIonFilterDataPoint point = dataSeries.get(series).get(item);
    if (xAxisType.equals(ProductIonFilterParameters.xAxisPrecursor)) {
      // double mz = point.getPrecursorMass();
      double mz = point.getPrecursorMZ();
      return mz;
    } else
      return point.getRetentionTime();

  }

  /**
   * @see org.jfree.data.xy.XYDataset#getY(int, int)
   */
  public Number getY(int series, int item) {
    ProductIonFilterDataPoint point = dataSeries.get(series).get(item);
    return point.getProductMZ();
  }

  public ProductIonFilterDataPoint getDataPoint(int item) {
    return dataSeries.get(RAW_LEVEL).get(item);
  }

  public ProductIonFilterDataPoint getDataPoint(double xValue, double yValue) {
    Vector<ProductIonFilterDataPoint> dataCopy =
        new Vector<ProductIonFilterDataPoint>(dataSeries.get(RAW_LEVEL));
    Iterator<ProductIonFilterDataPoint> it = dataCopy.iterator();
    double currentX, currentY;
    while (it.hasNext()) {
      ProductIonFilterDataPoint point = it.next();
      if (xAxisType == ProductIonFilterParameters.xAxisPrecursor)
        currentX = point.getPrecursorMass();
      else
        currentX = point.getRetentionTime();
      currentY = point.getProductMZ();
      // check for equality
      if ((Math.abs(currentX - xValue) < 0.00000001) && (Math.abs(currentY - yValue) < 0.00000001))
        return point;
    }
    return null;
  }

  /**
   * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  public String generateToolTip(XYDataset dataset, int series, int item) {
    return dataSeries.get(series).get(item).getName();
  }

  public void cancel() {
    setStatus(TaskStatus.CANCELED);
  }

  public String getErrorMessage() {
    return null;
  }

  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    else
      return ((double) processedScans / totalScans);
  }

  public TaskStatus getStatus() {
    return status;
  }

  public String getTaskDescription() {
    return "Updating fragment filter visualizer of " + rawDataFile;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#setStatus()
   */
  public void setStatus(TaskStatus newStatus) {
    this.status = newStatus;
  }

  public boolean isCanceled() {
    return status == TaskStatus.CANCELED;
  }

  public static boolean isAllTrue(boolean[] array) {
    for (boolean b : array)
      if (!b)
        return false;
    return true;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.NORMAL;
  }

}
