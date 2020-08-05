/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.csvimport;

import com.google.common.collect.Range;
import com.opencsv.CSVReader;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeature;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListRow;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.FileReader;

public class CsvImportTask extends AbstractTask {

  private final MZmineProject project;
  private RawDataFile rawDataFile;
  private final File fileName;
  private double percent=0.0;

  CsvImportTask(MZmineProject project, ParameterSet parameters){
    this.project = project;
    //first file only
    this.rawDataFile = parameters.getParameter(CsvImportParameters.dataFiles).getValue().getMatchingRawDataFiles()[0];
    this.fileName = parameters.getParameter(CsvImportParameters.filename).getValue()[0];
  }


  @Override
  public String getTaskDescription() {
    return "Import csv file";
  }

  @Override
  public double getFinishedPercentage() {
    return percent;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try{
      FileReader fileReader = new FileReader(fileName);
      CSVReader csvReader = new CSVReader(fileReader);
      PeakList newPeakList = new SimplePeakList( fileName.getName(), rawDataFile);
      String[] dataLine;
      int counter = 0;
      while((dataLine = csvReader.readNext()) != null){
        if(isCanceled()){
          return;
        }
        if(counter++ == 0){
          continue;
        }
        double peak_mz = 0.0, peak_rt = 0.0, peak_height = 0.0, abundance = 0.0,
            rtMin = 0.0, rtMax = 0.0, mzMin = 0.0, mzMax = 0.0, intensity = 0.0;
        Range<Double> finalRTRange;
        Range<Double> finalMZRange;
        Range<Double> finalIntensityRange;

        SimplePeakListRow newRow = new SimplePeakListRow(counter-1);
        for(int j=0 ; j<dataLine.length ; j++){
          switch(j){
            case 1:
              peak_mz = Double.parseDouble(dataLine[j]);
              break;
            case 2:
              mzMin = Double.parseDouble(dataLine[j]);
              break;
            case 3:
              mzMax = Double.parseDouble(dataLine[j]);
              break;
            case 4:
              //Retention times are taken in minutes
              peak_rt = Double.parseDouble(dataLine[j])/60.0;
              break;
            case 5:
              rtMin = Double.parseDouble(dataLine[j])/60.0;
              break;
            case 6:
              rtMax = Double.parseDouble(dataLine[j])/60.0;
              break;
            case 9:
              intensity = Double.parseDouble(dataLine[j]);
              peak_height = Double.parseDouble(dataLine[j]);
              break;
          }
        }
        finalMZRange = Range.closed(mzMin, mzMax);
        finalRTRange = Range.closed(rtMin, rtMax);
        finalIntensityRange = Range.singleton(intensity);
        int[] scanNumbers = {};
        DataPoint[] finalDataPoint = new DataPoint[1];
        finalDataPoint[0] = new SimpleDataPoint(peak_mz, peak_height);
        FeatureStatus status = FeatureStatus.UNKNOWN; // abundance unknown
        int representativeScan = 0;
        for(int s_no : rawDataFile.getScanNumbers()){
          if(rawDataFile.getScan(s_no).getRetentionTime() == peak_rt){
            representativeScan = s_no;
            for(DataPoint dp : rawDataFile.getScan(s_no).getDataPoints()){
              if(dp.getMZ() == peak_mz){
                finalDataPoint[0] = dp;
                break;
              }
            }
          }
        }

        int fragmentScan = -1;
        int[] allFragmentScans = new int[]{0};

        Feature peak = new SimpleFeature(rawDataFile, peak_mz, peak_rt, peak_height, abundance,
            scanNumbers, finalDataPoint, status, representativeScan, fragmentScan, allFragmentScans,
            finalRTRange, finalMZRange, finalIntensityRange);
        newRow.addPeak(rawDataFile,peak);
        newPeakList.addRow(newRow);
      }

    if(isCanceled())
      return;

    project.addPeakList(newPeakList);
    }
    catch (Exception e){
      e.printStackTrace();
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not import feature list from file " + fileName.getName() + ": " + e.getMessage());
      return;
    }
    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }
}
