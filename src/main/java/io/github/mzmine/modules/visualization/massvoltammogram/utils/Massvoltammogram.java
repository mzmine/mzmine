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

package io.github.mzmine.modules.visualization.massvoltammogram.utils;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_ecmscalcpotential.EcmsUtils;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramPlotPanel;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.collections.BinarySearch;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class Massvoltammogram {

  //Plot parameter.
  private final MassvoltammogramPlotPanel plot = new MassvoltammogramPlotPanel(this);
  //Raw Data.
  private final RawDataFile file;
  private final ModularFeatureList featureList;
  //Parameter.
  private final ScanSelection scanSelection;
  private final ReactionMode reactionMode;
  private final double delayTime; //In s.
  private final Range<Double> potentialRange;
  private double potentialRampSpeed; //In mV/s.
  private double stepSize; //In mV.
  private Range<Double> userInputMzRange;

  //Potentials in mV.
  private double startPotential;
  private double endPotential;

  //MassvoltammogramScans.
  private List<MassvoltammogramScan> rawScans;
  private List<MassvoltammogramScan> rawScansInMzRange;
  private List<MassvoltammogramScan> processedScans;

  //Progress.
  private int numExtractedScans;
  private int numTotalScans;

  private boolean mzRangeIsEmpty;
  private double divisor;

  //Constructor from raw data file.
  public Massvoltammogram(RawDataFile file, ScanSelection scanSelection, ReactionMode reactionMode,
      double delayTime, double potentialRampSpeed, Range<Double> potentialRange, double stepSize,
      Range<Double> mzRange) {

    //Setting variables.
    this.file = file;
    this.scanSelection = scanSelection;
    this.reactionMode = reactionMode;
    this.delayTime = delayTime;
    this.potentialRampSpeed = potentialRampSpeed;
    this.potentialRange = potentialRange;
    this.stepSize = stepSize;
    this.userInputMzRange = mzRange;

    //Setting unused variables to null.
    this.featureList = null;

    //Setting the potentials.
    setPotentials();
  }

  //Constructor from feature list.
  public Massvoltammogram(ModularFeatureList featureList, ReactionMode reactionMode,
      double delayTime, double potentialRampSpeed, Range<Double> potentialRange, double stepSize,
      Range<Double> mzRange) {

    //Setting variables.
    this.featureList = featureList;
    this.reactionMode = reactionMode;
    this.delayTime = delayTime;
    this.potentialRampSpeed = potentialRampSpeed;
    this.potentialRange = potentialRange;
    this.stepSize = stepSize;
    this.userInputMzRange = mzRange;

    //Setting unused variables to null.
    this.file = null;
    this.scanSelection = null;

    //Setting the potentials.
    setPotentials();
  }

  /**
   * Method to create all necessary MassvoltammogramScans as well as the MassvoltammogramPlotPanel.
   */
  public void draw() {

    //Setting the total number of scans needed to be extracted.
    numTotalScans = ((int) Math.abs(endPotential) / (int) stepSize) + 1;

    //Extracting the raw scans
    if (file != null) {
      extractScansFromRawDataFile();
    } else if (featureList != null) {
      extractScansFromFeatureList();
    }

    //Creating the massvoltammogram from the raw scans.
    cropRawScansToMzRange();

    if (!mzRangeIsEmpty) {

      //Processing the data.
      processRawScans();

      //Plotting the processed data.
      divisor = MassvoltammogramUtils.getDivisor(
          MassvoltammogramUtils.getMaxIntensity(processedScans));
      plotMassvoltammogram();

      //Plotting an empty massvoltammogram if there is no data in the mz-range.
    } else {
      plotEmptyMassvoltammogram();
    }

    //Resetting the number of processed scans, to show the progress correctly if the draw method gets called again.
    numExtractedScans = 0;
  }

  /**
   * @return Returns the progress for the extraction of the raw scans.
   */
  public double getProgress() {
    if (numTotalScans > 0) {
      return (double) numExtractedScans / (double) numTotalScans;
    } else {
      return 0;
    }
  }

  /**
   * Sets the start and end potential as well as the potential step size and potential ramp speed
   * according to the values entered by the user.
   */
  private void setPotentials() {

    if (reactionMode.equals(ReactionMode.OXIDATIVE)) {

      startPotential = potentialRange.lowerEndpoint();
      endPotential = potentialRange.upperEndpoint();
      potentialRampSpeed = Math.abs(potentialRampSpeed);
      stepSize = Math.abs(stepSize);

    } else if (reactionMode.equals(ReactionMode.REDUCTIVE)) {

      startPotential = potentialRange.upperEndpoint();
      endPotential = potentialRange.lowerEndpoint();
      potentialRampSpeed = Math.abs(potentialRampSpeed) * -1;
      stepSize = Math.abs(stepSize) * -1;
    }
  }

  /**
   * Extracts all scans needed to draw the massvoltammogram from a raw data file.
   */
  private void extractScansFromRawDataFile() {

    //Initializing a list to add the scans to.
    final List<MassvoltammogramScan> scans = new ArrayList<>();

    //Initializing a number formatter for the mz-values.
    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    //Setting the starting potential
    double potential = startPotential;

    //Adding scans with the given step size until the maximal potential is reached.
    while (Math.abs(potential) <= Math.abs(endPotential)) {

      //Getting the scan for the given potential
      final float rt = EcmsUtils.getRtAtPotential(delayTime, potentialRampSpeed, potential);
      final Scan scan = scanSelection.getScanAtRt(file, rt);

      //Breaking the loop if the calculated rt exceeds the max rt of the data file.
      if (scan == null) {
        break;
      }

      //Writing the scans mz and intensity values to arrays.
      final double[] mzs = new double[scan.getNumberOfDataPoints()];
      final double[] intensities = new double[scan.getNumberOfDataPoints()];

      for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
        mzs[i] = Double.parseDouble(mzFormat.format(scan.getMzValue(i)));
        intensities[i] = scan.getIntensityValue(i);
      }

      //Adding a MassvoltammogramScan to the list.
      scans.add(new MassvoltammogramScan(mzs, intensities, potential, scan.getSpectrumType()));

      //Incrementing the potential.
      potential = potential + stepSize;

      //Updating the number of extracted scans for the progress.
      numExtractedScans++;
    }

    this.rawScans = scans;
  }

  /**
   * Extracts all scans needed to draw the massvoltammogram from a feature list.
   */
  private void extractScansFromFeatureList() {

    //Initializing a list to add the scans to.
    final List<MassvoltammogramScan> scans = new ArrayList<>();

    //Setting the number format for the mz-values.
    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    //Setting the potential to the start potential
    double potential = startPotential;

    //Getting the rows from the feature list and sorting them to ascending mz-values.
    final List<FeatureListRow> rows = featureList.getRows();
    rows.sort(FeatureListRowSorter.MZ_ASCENDING);

    //Calculating the rt tolerance for a feature around the potential.
    final float timeBetweenScansInS = (float) (stepSize / potentialRampSpeed);
    final double rtToleranceInMin = (timeBetweenScansInS / 2) / 60;

    //Creating a scan for every potential value until the end potential is reached.
    while (Math.abs(potential) <= Math.abs(endPotential)) {

      //Calculating the rt fot the given applied potential.
      final float rt = (EcmsUtils.getRtAtPotential(delayTime, potentialRampSpeed, potential));

      //Initializing lists to add the mz-values and the intensity-values to.
      final List<Double> mzs = new ArrayList<>();
      final List<Double> intensities = new ArrayList<>();

      //Going over all the rows in the feature list and adding the mz and intensity values if the rt matches.
      for (FeatureListRow row : rows) {

        //Getting the feature data from the row.
        final Feature feature = row.getBestFeature();
        final IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();

        //Searching for the index of the features closest rt to the potential rt.
        final int index = BinarySearch.binarySearch(rt, true, featureData.getNumberOfValues(),
            featureData::getRetentionTime);

        //Calculating the rt value for the found index.
        final float foundRt = featureData.getRetentionTime(index);

        //Adding the features mz and intensity if the difference between features rt and potential rt is within the tolerance
        //and the intensity is not 0.
        if (Math.abs(rt - foundRt) < rtToleranceInMin && featureData.getIntensity(index) != 0) {

          mzs.add(Double.parseDouble(mzFormat.format(feature.getMZ())));
          intensities.add(featureData.getIntensity(index));
        }
      }

      //Adding the scan to the list of scans if features were found for the given potential.
      if (!mzs.isEmpty() && !intensities.isEmpty()) {
        scans.add(
            new MassvoltammogramScan(mzs, intensities, potential, MassSpectrumType.CENTROIDED));
      }
      //Increasing the potential by the potential step size.
      potential = potential + stepSize;

      //Updating the number of extracted scans for the progress.
      numExtractedScans++;
    }

    this.rawScans = scans;
  }

  /**
   * Extracts all values of the raw scans within the mz-range.
   */
  private void cropRawScansToMzRange() {

    //Extracting the mz-range from the scans.
    rawScansInMzRange = MassvoltammogramUtils.extractMZRangeFromScan(rawScans, userInputMzRange);

    //Checking if there is no raw data in the mz-range before aligning the scans.
    checkIfMzRangeIsEmpty();

    //Adding datapoints with an intensity of zero around centroid datapoints to visualize them correctly.
    MassvoltammogramUtils.addZerosToCentroidData(rawScansInMzRange);

    //Aligning the scans to all start and end at the same mz-value.
    MassvoltammogramUtils.alignScans(rawScansInMzRange, userInputMzRange, getRawDataMzRange());
  }

  /**
   * Removes excess datapoints for the processed scans.
   */
  private void processRawScans() {

    //Removing low intensity signals as well as neighbouring signals with intensity values of 0.
    processedScans = MassvoltammogramUtils.removeNoise(rawScansInMzRange,
        MassvoltammogramUtils.getMaxIntensity(rawScansInMzRange));
  }

  /**
   * Plots the massvoltammograms processed scans.
   */
  private void plotMassvoltammogram() {

    for (MassvoltammogramScan scan : processedScans) {

      //Initializing a double array for the divided intensities as well as the potentials.
      final double[] intensities = new double[scan.getNumberOfDatapoints()];
      final double[] potential = new double[scan.getNumberOfDatapoints()];

      //writing the divided intensities and the potential to arrays.
      for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {
        intensities[i] = scan.getIntensity(i) / divisor;
        potential[i] = scan.getPotential();
      }

      //Adding the arrays to the plot.
      plot.addLinePlot("Spectrum at " + scan.getPotential() + " mV.", Color.black, scan.getMzs(),
          potential, intensities);
    }

    //Setting the plots axis and labels.
    formatPlot();
  }

  /**
   * Creates an empty massvoltammogram plot in the given mz- and potential-range.
   */
  private void plotEmptyMassvoltammogram() {

    //Adding the empty line plots to the plot panel only if the mz-range is contained by the raw data.
    //Otherwise, showing an empty plot.
    if (rawDataOverlapsWithMzRange(userInputMzRange)) {
      for (MassvoltammogramScan scan : rawScansInMzRange) {
        plot.addLinePlot("Spectrum at " + scan.getPotential() + " mV.", Color.black, scan.getMzs(),
            scan.getPotentialAsArray(), scan.getIntensities());
      }
    }

    //Setting up the plot's axis.
    plot.setAxisLabels("m/z", "Potential / mV", "Intensity / a.u.");
    plot.setFixedBounds(0, userInputMzRange.lowerEndpoint(), userInputMzRange.upperEndpoint());
    plot.setFixedBounds(1, potentialRange.lowerEndpoint(), potentialRange.upperEndpoint());
    plot.setFixedBounds(2, 0d, 10d);
  }

  /**
   * Sets the plots axis and labels.
   */
  private void formatPlot() {

    //Setting up the plot's axis.
    plot.setAxisLabels("m/z", "Potential / mV",
        "Intensity / 10" + MassvoltammogramUtils.toSuperscript((int) Math.log10(divisor))
            + " a.u.");
    plot.setFixedBounds(0, userInputMzRange.lowerEndpoint(), userInputMzRange.upperEndpoint());
    plot.setFixedBounds(1, potentialRange.lowerEndpoint(), potentialRange.upperEndpoint());
  }

  /**
   * Checks if all the MassvoltammogramScans in the extracted mz-range are empty.
   */
  private void checkIfMzRangeIsEmpty() {

    int numScans = rawScansInMzRange.size();
    int numEmptyScans = 0;

    for (MassvoltammogramScan scan : rawScansInMzRange) {
      if (scan.isEmpty()) {
        numEmptyScans++;
      }
    }
    mzRangeIsEmpty = numScans == numEmptyScans;
  }

  /**
   * Checks weather a given mz-range is overlaps with the raw data's mz-range.
   *
   * @param mzRange The mz-range to be checked.
   * @return Returns true if the given mz-range overlaps with the raw data's mz-range.
   */
  private boolean rawDataOverlapsWithMzRange(Range<Double> mzRange) {

    Range<Double> rawDataMzRange = getRawDataMzRange();
    return rawDataMzRange.isConnected(mzRange);
  }

  /**
   * @return Returns the mz-range of the raw data file or feature list used for the
   * massvoltammogram.
   */
  private Range<Double> getRawDataMzRange() {

    //Creating an empty range.
    Range<Double> rawDataMzRange = null;

    //Setting the mz-range to the raw data files mz-range if a raw data file is used.
    if (file != null) {
      rawDataMzRange = file.getDataMZRange(1);

      //Setting the mz-range to the feature lists mz-range if a feature list is used instead.
    } else if (featureList != null) {

      //Setting the range to the first feature lists mz-range.
      List<RawDataFile> files = featureList.getRawDataFiles();
      rawDataMzRange = files.get(0).getDataMZRange(1);

      //Spanning the mz-range around all feature lists if multiple feature lists are used.
      if (files.size() > 1) {

        for (int i = 1; i < files.size(); i++) {
          rawDataMzRange = rawDataMzRange.span(files.get(i).getDataMZRange(1));
        }
      }
    }
    return rawDataMzRange;
  }

  /**
   * @return Returns the massvoltammograms raw scans in m/z-range.
   */
  public List<MassvoltammogramScan> getRawScansInMzRange() {
    return rawScansInMzRange;
  }

  /**
   * @return Returns the processed MassvoltammogramScans as a plot.
   */
  public MassvoltammogramPlotPanel getPlot() {
    return plot;
  }

  /**
   * @return Returns the filename of the raw data file / feature list. Returns an empty string, if
   * the raw data file and the feature list both are null.
   */
  public String getFileName() {

    if (file != null) {
      return file.getName();

    } else if (featureList != null) {
      return featureList.getName();
    }

    return "";
  }


  /**
   * Asks the user for a new m/z-range, extracts this new m/z- range from the raw scans and plots
   * the scans in the new m/z-range.
   */
  public void editMzRange(Range<Double> mzRange) {

    this.userInputMzRange = mzRange;
    cropRawScansToMzRange();

    //Removing the old line plots to draw new ones.
    plot.removeAllPlots();

    if (!mzRangeIsEmpty) {

      //Processing the data.
      processRawScans();

      //Plotting the processed data.
      divisor = MassvoltammogramUtils.getDivisor(
          MassvoltammogramUtils.getMaxIntensity(processedScans));
      plotMassvoltammogram();

      //Plotting an empty massvoltammogram if there is no data in the mz-range.
    } else {
      plotEmptyMassvoltammogram();
    }
  }

  /**
   * Scales the massvoltammogram-plots intensity axis to a set max value.
   *
   * @param maxValue The max value the intensity axis will be scaled to.
   */
  public void scalePlotsIntensityAxis(double maxValue) {

    //Calculating the new divisor to resize the shown data.
    this.divisor = MassvoltammogramUtils.getDivisor(maxValue);

    //Removing the old line plots and plotting the rescaled data.
    plot.removeAllPlots();
    if (!mzRangeIsEmpty) {
      plotMassvoltammogram();
    } else {
      plotEmptyMassvoltammogram();
      plot.setAxisLabel(2,
          "Intensity / 10" + MassvoltammogramUtils.toSuperscript((int) Math.log10(divisor))
              + " a.u.");
    }

    //Scaling the intensity axis to the new max value.
    plot.scaleIntensityAxis(0, maxValue / divisor);
  }
}
