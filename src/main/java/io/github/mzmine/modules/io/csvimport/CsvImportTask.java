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
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.Feature;
import io.github.mzmine.datamodel.data.FeatureList;
import io.github.mzmine.datamodel.data.ModularFeature;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.FileReader;

public class CsvImportTask extends AbstractTask {

  private final MZmineProject project;
  private RawDataFile rawDataFile;
  private final File fileName;
  private double percent = 0.0;

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
      FeatureList newFeatureList = new ModularFeatureList( fileName.getName(), rawDataFile);
      String[] dataLine;
      int counter = 0;
      while((dataLine = csvReader.readNext()) != null){
        if(isCanceled()){
          return;
        }
        if(counter++ == 0){
          continue;
        }
        double feature_mz = 0.0, mzMin = 0.0, mzMax = 0.0;
        float feature_rt = 0f, intensity = 0f, rtMin = 0f, rtMax = 0f, feature_height = 0f, abundance = 0f;
        Range<Float> finalRTRange;
        Range<Double> finalMZRange;
        Range<Float> finalIntensityRange;

        ModularFeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) newFeatureList, counter-1);
        for(int j=0 ; j<dataLine.length ; j++){
          switch(j){
            case 1:
              feature_mz = Double.parseDouble(dataLine[j]);
              break;
            case 2:
              mzMin = Double.parseDouble(dataLine[j]);
              break;
            case 3:
              mzMax = Double.parseDouble(dataLine[j]);
              break;
            case 4:
              //Retention times are taken in minutes
              feature_rt = (float) (Double.parseDouble(dataLine[j])/60.0);
              break;
            case 5:
              rtMin = (float) (Double.parseDouble(dataLine[j])/60.0);
              break;
            case 6:
              rtMax = (float) (Double.parseDouble(dataLine[j])/60.0);
              break;
            case 9:
              intensity = (float) Double.parseDouble(dataLine[j]);
              feature_height = (float) Double.parseDouble(dataLine[j]);
              break;
          }
        }
        finalMZRange = Range.closed(mzMin, mzMax);
        finalRTRange = Range.closed(rtMin, rtMax);
        finalIntensityRange = Range.singleton(intensity);
        int[] scanNumbers = {};
        DataPoint[] finalDataPoint = new DataPoint[1];
        finalDataPoint[0] = new SimpleDataPoint(feature_mz, feature_height);
        FeatureStatus status = FeatureStatus.UNKNOWN; // abundance unknown
        int representativeScan = 0;
        for(int s_no : rawDataFile.getScanNumbers()){
          if(rawDataFile.getScan(s_no).getRetentionTime() == feature_rt){
            representativeScan = s_no;
            for(DataPoint dp : rawDataFile.getScan(s_no).getDataPoints()){
              if(dp.getMZ() == feature_mz){
                finalDataPoint[0] = dp;
                break;
              }
            }
          }
        }

        int fragmentScan = -1;
        int[] allFragmentScans = new int[]{0};

        Feature feature = new ModularFeature(rawDataFile, feature_mz, feature_rt, feature_height, abundance,
            scanNumbers, finalDataPoint, status, representativeScan, fragmentScan, allFragmentScans,
            finalRTRange, finalMZRange, finalIntensityRange);
        newRow.addFeature(rawDataFile,feature);
        newFeatureList.addRow(newRow);
      }

    if(isCanceled())
      return;

    project.addFeatureList(newFeatureList);
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
