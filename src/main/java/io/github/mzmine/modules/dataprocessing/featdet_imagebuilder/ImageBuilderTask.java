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
 *
 * Edited and modified by Owen Myers (Oweenm@gmail.com)
 */

package io.github.mzmine.modules.dataprocessing.featdet_imagebuilder;


import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.Coordinates;
import io.github.mzmine.modules.io.rawdataimport.fileformats.imzmlimport.ImagingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.StorableImagingScan;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureConvertors;


public class ImageBuilderTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(ImageBuilderTask.class.getName());

  private RangeSet<Double> rangeSet = TreeRangeSet.create();
  private HashMap<Range<Double>, IImage> rangeToImageMap = new HashMap<>();

  private final MZmineProject project;
  private final ImagingRawDataFile rawDataFile;
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final String massList;
  private final int minTotalSignals;
  private final ScanSelection scanSelection;
  private final ImagingParameters imagingParameters;
  private double progress = 0.0;
  private String taskDescription = "";

  public ImageBuilderTask(MZmineProject project, RawDataFile rawDataFile, ParameterSet parameters) {
    this.project = project;
    this.rawDataFile = (ImagingRawDataFile) rawDataFile;
    this.imagingParameters = ((ImagingRawDataFile) rawDataFile).getImagingParam();
    this.mzTolerance = parameters.getParameter(ImageBuilderParameters.mzTolerance).getValue();
    this.massList = parameters.getParameter(ImageBuilderParameters.massList).getValue();
    this.minTotalSignals =
        parameters.getParameter(ImageBuilderParameters.minTotalSignals).getValue();
    this.scanSelection = parameters.getParameter(ImageBuilderParameters.scanSelection).getValue();
    this.suffix = parameters.getParameter(ImageBuilderParameters.suffix).getValue();
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      return;
    }
    progress = 0.0;
    Set<ImageDataPoint> imageDataPoints = extractAllDataPointsFromScans();
    createImageTargetSet(imageDataPoints);
    SortedSet<IImage> images = finishImages();
    buildModularFeatureList(images);
    progress = 1.0;
    setStatus(TaskStatus.FINISHED);
  }

  // Extract all data point sorted by intensity
  private Set<ImageDataPoint> extractAllDataPointsFromScans() {
    logger.info("Start data point extraction");
    taskDescription = "Get data points from scans";
    int processedScans = 1;
    SortedSet<ImageDataPoint> allDataPoints = new TreeSet<>(new Comparator<ImageDataPoint>() {
      @Override
      public int compare(ImageDataPoint o1, ImageDataPoint o2) {
        if (o1.getIntensity() > o2.getIntensity()) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    Scan[] scans = scanSelection.getMatchingScans(rawDataFile);

    for (int i = 0; i < scans.length; i++) {

      if (!(scans[0] instanceof StorableImagingScan) || !scanSelection.matches(scans[0])) {
        continue;
      }
      System.out.println(((StorableImagingScan) scans[0]).getCoordinates().toString());
      // if (scans[0].getMassList(massList) == null) {
      // setStatus(TaskStatus.ERROR);
      // setErrorMessage(
      // "Scan #" + scans[0].getScanNumber() + " does not have a mass list " + massList);
      // } else {
      // Arrays.stream(scans[0].getMassList(massList).getDataPoints())
      // .forEach(dp -> allDataPoints.add(new ImageDataPoint(dp.getMZ(), dp.getIntensity(),
      // scans[0].getScanNumber(), ((StorableImagingScan) scans[0]).getCoordinates(),
      // imagingParameters.getPixelWidth(), imagingParameters.getPixelShape())));
      // }
      Arrays.stream(scans[0].getDataPoints())
          .forEach(dp -> allDataPoints.add(new ImageDataPoint(dp.getMZ(), dp.getIntensity(),
              scans[0].getScanNumber(), ((StorableImagingScan) scans[0]).getCoordinates(),
              imagingParameters.getPixelWidth(), imagingParameters.getPixelShape())));
      progress = (processedScans / (double) scans.length) / 4;
      processedScans++;
    }
    logger.info("Extracted " + allDataPoints.size() + " ims data points");
    return allDataPoints;
  }

  private void createImageTargetSet(Set<ImageDataPoint> imageDataPoints) {
    logger.info("Start m/z ranges calculation");
    taskDescription = "Calculate m/z ranges";
    int processedDataPoint = 1;
    for (ImageDataPoint imageDataPoint : imageDataPoints) {
      if (isCanceled()) {
        return;
      }
      Range<Double> containsDataPointRange = rangeSet.rangeContaining(imageDataPoint.getMZ());
      Range<Double> toleranceRange = mzTolerance.getToleranceRange(imageDataPoint.getMZ());
      if (containsDataPointRange == null) {
        // look +- mz tolerance to see if ther is a range near by.
        // If there is use the proper boundry of that range for the
        // new range to insure than NON OF THE RANGES OVERLAP.
        Range<Double> plusRange = rangeSet.rangeContaining(toleranceRange.upperEndpoint());
        Range<Double> minusRange = rangeSet.rangeContaining(toleranceRange.lowerEndpoint());
        Double toBeLowerBound;
        Double toBeUpperBound;

        // If both of the above ranges are null then we make the new range spaning the full
        // mz tolerance range.
        // If one or both are not null we need to properly modify the range of the new
        // chromatogram so that none of the points are overlapping.
        if ((plusRange == null) && (minusRange == null)) {
          toBeLowerBound = toleranceRange.lowerEndpoint();
          toBeUpperBound = toleranceRange.upperEndpoint();
        } else if ((plusRange == null) && (minusRange != null)) {
          // the upper end point of the minus range will be the lower
          // range of the new one
          toBeLowerBound = minusRange.upperEndpoint();
          toBeUpperBound = toleranceRange.upperEndpoint();

        } else if ((minusRange == null) && (plusRange != null)) {
          toBeLowerBound = toleranceRange.lowerEndpoint();
          toBeUpperBound = plusRange.lowerEndpoint();
        } else if ((minusRange != null) && (plusRange != null)) {
          toBeLowerBound = minusRange.upperEndpoint();
          toBeUpperBound = plusRange.lowerEndpoint();
        } else {
          toBeLowerBound = 0.0;
          toBeUpperBound = 0.0;
        }

        if (toBeLowerBound < toBeUpperBound) {
          Range<Double> newRange = Range.open(toBeLowerBound, toBeUpperBound);
          IImage newImage = new Image(imageDataPoint.getMZ(), imagingParameters,
              imageDataPoint.getCoordinates(), imageDataPoint.getIntensity(), newRange);
          Set<ImageDataPoint> dataPointsSetForImage = new HashSet<>();
          dataPointsSetForImage.add(imageDataPoint);
          newImage.setDataPoints(dataPointsSetForImage);
          rangeToImageMap.put(newRange, newImage);
          rangeSet.add(newRange);
        } else if (toBeLowerBound.equals(toBeUpperBound) && plusRange != null) {
          IImage currentImage = rangeToImageMap.get(plusRange);
          currentImage.getDataPoints().add(imageDataPoint);
        } else
          throw new IllegalStateException(String.format("Incorrect range [%f, %f] for m/z %f",
              toBeLowerBound, toBeUpperBound, imageDataPoint.getMZ()));

      } else {
        // In this case we do not need to update the rangeSet

        IImage currentImage = rangeToImageMap.get(containsDataPointRange);
        currentImage.getDataPoints().add(imageDataPoint);

        // update the entry in the map
        rangeToImageMap.put(containsDataPointRange, currentImage);
      }
      double progressStep = (processedDataPoint / imageDataPoints.size()) / 4;
      progress += progressStep;
    }
  }

  private SortedSet<IImage> finishImages() {
    Set<Range<Double>> ranges = rangeSet.asRanges();
    Iterator<Range<Double>> rangeIterator = ranges.iterator();
    SortedSet<IImage> images = new TreeSet<>(new Comparator<IImage>() {
      @Override
      public int compare(IImage o1, IImage o2) {
        if (o1.getMz() > o2.getMz()) {
          return 1;
        } else {
          return -1;
        }
      }
    });
    double progressStep = (!ranges.isEmpty()) ? 0.75 / ranges.size() : 0.0;
    while (rangeIterator.hasNext()) {
      if (isCanceled()) {
        break;
      }
      progress += progressStep;
      Range<Double> currentRangeKey = rangeIterator.next();
      IImage image = rangeToImageMap.get(currentRangeKey);
      if (image.getDataPoints().size() >= minTotalSignals) {
        logger.info("Build ion trace for m/z range " + image.getMzRange());
        finishImage(image);
        images.add(image);
      }
    }
    return images;
  }

  private IImage finishImage(IImage image) {
    Range<Double> rawDataPointsIntensityRange = null;
    Range<Double> rawDataPointsMZRange = null;
    Set<Integer> scanNumbers = new HashSet<>();
    SortedSet<ImageDataPoint> sortedRetentionTimeMobilityDataPoints =
        new TreeSet<>(new Comparator<ImageDataPoint>() {
          @Override
          public int compare(ImageDataPoint o1, ImageDataPoint o2) {
            if (o1.getScanNumber() > o2.getScanNumber()) {
              return 1;
            } else {
              return -1;
            }
          }
        });
    sortedRetentionTimeMobilityDataPoints.addAll(image.getDataPoints());
    // Update raw data point ranges, height, and representative scan
    double maximumIntensity = Double.MIN_VALUE;
    Coordinates mostIntenseCoordinate = null;
    for (ImageDataPoint imageDataPoint : sortedRetentionTimeMobilityDataPoints) {
      scanNumbers.add(imageDataPoint.getScanNumber());

      // set ranges
      if (rawDataPointsIntensityRange == null && rawDataPointsMZRange == null) {
        rawDataPointsIntensityRange = Range.singleton(imageDataPoint.getIntensity());
        rawDataPointsMZRange = Range.singleton(imageDataPoint.getMZ());
      } else {
        rawDataPointsIntensityRange =
            rawDataPointsIntensityRange.span(Range.singleton(imageDataPoint.getIntensity()));
        rawDataPointsMZRange = rawDataPointsMZRange.span(Range.singleton(imageDataPoint.getMZ()));
      }

      // set maxima
      if (maximumIntensity < imageDataPoint.getIntensity()) {
        maximumIntensity = imageDataPoint.getIntensity();
        mostIntenseCoordinate = imageDataPoint.getCoordinates();
      }

    }

    // TODO think about representative scan
    image.setScanNumbers(scanNumbers);
    image.setMzRange(rawDataPointsMZRange);
    image.setIntensityRange(rawDataPointsIntensityRange);
    image.setMaximumIntensity(maximumIntensity);
    image.setMostIntensCoordinate(mostIntenseCoordinate);
    // logger.info("Ion Trace results:\n" + "Scan numbers: " + ionTrace.getScanNumbers() + "\n" + //
    // "Mobility range: " + ionTrace.getMobilityRange() + "\n" + //
    // "m/z range: " + ionTrace.getMzRange() + "\n" + //
    // "rt range: " + ionTrace.getRetentionTimeRange() + "\n" + //
    // "intensity range: " + ionTrace.getIntensityRange() + "\n" + //
    // "Max intensity : " + ionTrace.getMaximumIntensity() + "\n" + //
    // "Retention time : " + ionTrace.getRetentionTime() + "\n" + //
    // "Mobility : " + ionTrace.getMobility()//
    // );
    // TODO calc area
    // Update area
    // double area = 0;
    // for (int i = 1; i < allScanNumbers.length; i++) {
    // // For area calculation, we use retention time in seconds
    // double previousRT = dataFile.getScan(allScanNumbers[i - 1]).getRetentionTime() * 60d;
    // double currentRT = dataFile.getScan(allScanNumbers[i]).getRetentionTime() * 60d;
    // double previousHeight = dataPointsMap.get(allScanNumbers[i - 1]).getIntensity();
    // double currentHeight = dataPointsMap.get(allScanNumbers[i]).getIntensity();
    // area += (currentRT - previousRT) * (currentHeight + previousHeight) / 2;
    // }

    // TODO
    // Update fragment scan
    // fragmentScan =
    // ScanUtils.findBestFragmentScan(dataFile, dataFile.getDataRTRange(1), rawDataPointsMZRange);

    // allMS2FragmentScanNumbers = ScanUtils.findAllMS2FragmentScans(dataFile,
    // dataFile.getDataRTRange(1), rawDataPointsMZRange);

    // if (fragmentScan > 0) {
    // Scan fragmentScanObject = dataFile.getScan(fragmentScan);
    // int precursorCharge = fragmentScanObject.getPrecursorCharge();
    // if (precursorCharge > 0)
    // this.charge = precursorCharge;
    // }

    return image;
  }


  private void buildModularFeatureList(SortedSet<IImage> images) {
    taskDescription = "Build feature list";
    ModularFeatureList featureList =
        new ModularFeatureList(rawDataFile + " " + suffix, rawDataFile);
    // featureList.addRowType(new FeatureShapeIonMobilityRetentionTimeType());
    // featureList.addRowType(new FeatureShapeIonMobilityRetentionTimeHeatMapType());
    // featureList.addRowType(new FeatureShapeMobilogramType());
    // featureList.addRowType(new MobilityType());
    featureList.addRowType(new ImageType());
    int featureId = 1;
    for (IImage image : images) {
      System.out.println(image.getDataPoints().size());
      Set<ImageDataPoint> dps = image.getDataPoints();
      for (ImageDataPoint dp : dps) {
        System.out.println(dp.getCoordinates().toString());
      }
      image.setFeatureList(featureList);
      ModularFeature modular = FeatureConvertors.ImageToModularFeature(image, rawDataFile);
      ModularFeatureListRow newRow =
          new ModularFeatureListRow(featureList, featureId, rawDataFile, modular);
      newRow.set(ImageType.class, newRow.getFeaturesProperty());
      // newRow.set(MobilityType.class, image.getMobility());
      // newRow.set(FeatureShapeIonMobilityRetentionTimeType.class, newRow.getFeaturesProperty());
      // newRow.set(FeatureShapeMobilogramType.class, newRow.getFeaturesProperty());
      // newRow.set(FeatureShapeIonMobilityRetentionTimeHeatMapType.class,
      // newRow.getFeaturesProperty());
      featureList.addRow(newRow);
      featureId++;
    }
    project.addFeatureList(featureList);
  }

}

