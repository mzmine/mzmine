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
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramMzRangeParameter;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.MassvoltammogramPlotPanel;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.collections.BinarySearch;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class Massvoltammogram {

  //The plot.
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
    processRawScans();
    plotMassvoltammogram();

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
    List<MassvoltammogramScan> scansInMzRange = MassvoltammogramUtils.extractMZRangeFromScan(
        rawScans, userInputMzRange);

    //Adding datapoints with an intensity of zero around centroid datapoints to visualize them correctly.
    MassvoltammogramUtils.addZerosToCentroidData(scansInMzRange);

    //Aligning the scans to all start and end at the same mz-value.
    MassvoltammogramUtils.alignScans(scansInMzRange, userInputMzRange);

    this.rawScansInMzRange = scansInMzRange;
  }

  /**
   * Removes excess datapoints for the processed scans.
   */
  private void processRawScans() {

    //Removing low intensity signals as well as neighbouring signals with intensity values of 0.
    processedScans = MassvoltammogramUtils.removeNoise(rawScansInMzRange,
        getMaxIntensity(rawScansInMzRange));
  }

  /**
   * Plots the massvoltammograms processed scans.
   */
  private void plotMassvoltammogram() {

    //Calculating the divisor needed to scale the z-axis.
    final double divisor = MassvoltammogramUtils.getDivisor(getMaxIntensity(processedScans));

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
   * Sets the plots axis and labels.
   */
  private void formatPlot() {

    //Calculating the divisor needed to scale the z-axis.
    final double divisor = MassvoltammogramUtils.getDivisor(getMaxIntensity(processedScans));

    //Setting up the plot's axis.
    plot.setAxisLabels("m/z", "Potential / mV",
        "Intensity / 10" + MassvoltammogramUtils.toSuperscript((int) Math.log10(divisor))
            + " a.u.");
    plot.setFixedBounds(1, potentialRange.lowerEndpoint(), potentialRange.upperEndpoint());
    plot.setFixedBounds(0, userInputMzRange.lowerEndpoint(), userInputMzRange.upperEndpoint());
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
   * Finds the maximal intensity in a set of MassvoltammogramScans.
   *
   * @param scans The list of MassvoltammogramScans the maximal intensity will be extracted from.
   * @return Returns the maximal intensity over all MassvoltammogramScans.
   */
  public double getMaxIntensity(List<MassvoltammogramScan> scans) {

    //Setting the initial max intensity to 0.
    double maxIntensity = 0;

    //Going over every datapoint in all the scans and comparing the intensity to the current max intensity.
    for (MassvoltammogramScan scan : scans) {
      for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {
        if (scan.getIntensity(i) > maxIntensity) {
          maxIntensity = scan.getIntensity(i);
        }
      }
    }
    return maxIntensity;
  }


  public List<MassvoltammogramScan> getProcessedScans() {
    return processedScans;
  }

  /**
   * Asks the user for a new m/z-range, extracts this new m/z- range from the raw scans and plots
   * the scans in the new m/z-range.
   */
  public void editMzRange() {

    //Getting user input for the new m/z-Range.
    final MassvoltammogramMzRangeParameter mzRangeParameter = new MassvoltammogramMzRangeParameter();
    if (mzRangeParameter.showSetupDialog(true) != ExitCode.OK) {
      return;
    }
    this.userInputMzRange = mzRangeParameter.getValue(MassvoltammogramMzRangeParameter.mzRange);

    cropRawScansToMzRange();
    processRawScans();

    plot.removeAllPlots();
    plotMassvoltammogram();
  }
}
