/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package io.github.mzmine.modules.visualization.massvoltammogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTask extends AbstractTask {


  private RawDataFile file;
  private ModularFeatureList featureList;
  private double potentialRampSpeed;
  /**
   * step size between drawn spectra in mV
   */
  private double stepSize;
  /**
   * potential range of mass voltammogram in mV
   */
  private final Range<Double> potentialRange;
  /**
   * m/z range of drawn spectra
   */
  private final Range<Double> mzRange;
  /**
   * Mode of the EC/MS experiment.
   */
  private final ReactionMode reactionMode;
  /**
   * The beginning of the potential ramp.
   */
  private double startPotential;
  /**
   * The end of the potential ramp.
   */
  private double endPotential;
  /**
   * The scan selection filter.
   */
  private final ScanSelection scanSelection;
  /**
   * Delay time between the EC cell and the MS in s.
   */
  private final double delayTime;

  public MassvoltammogramTask(@NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    if (parameters instanceof MassvoltammogramFromFileParameters) {

      file = MassvoltammogramFromFileParameters.files.getValue().getMatchingRawDataFiles()[0];
      featureList = null;

    } else if (parameters instanceof MassvoltammogramFromFeatureListParameters) {

      featureList = MassvoltammogramFromFeatureListParameters.featureList.getValue()
          .getMatchingFeatureLists()[0];
      file = null;
    }

    potentialRampSpeed = parameters.getValue(MassvoltammogramFromFileParameters.potentialRampSpeed);
    stepSize = parameters.getValue(MassvoltammogramFromFileParameters.stepSize);
    potentialRange = parameters.getValue(MassvoltammogramFromFileParameters.potentialRange);
    mzRange = parameters.getValue(MassvoltammogramFromFileParameters.mzRange);
    reactionMode = parameters.getValue(MassvoltammogramFromFileParameters.reactionMode);
    delayTime = parameters.getValue(MassvoltammogramFromFileParameters.delayTime);
    scanSelection = parameters.getValue(MassvoltammogramFromFileParameters.scanSelection);
  }

  @Override
  public String getTaskDescription() {
    return "Creating Massvoltammogram";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    //Setting up the parameters correctly depending on the selected reaction type.
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

    //Creating new 3D Plot.
    final ExtendedPlot3DPanel plot = new ExtendedPlot3DPanel();

    //Initializing a list of double arrays to store the scans data in.
    List<double[][]> scans = null;

    //Extracting the needed scans from teh raw data file or from the feature list.
    if (file != null) {

      scans = MassvoltammogramUtils.getScansFromRawDataFile(file, scanSelection, delayTime,
          startPotential, endPotential, potentialRampSpeed, stepSize);

    } else if (featureList != null) {

      scans = MassvoltammogramUtils.getScansFromFeatureList(featureList, delayTime, startPotential,
          endPotential, potentialRampSpeed, stepSize);

    }

    //Checking weather the scans were extracted correctly.
    if (scans.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(
          "The entered parameters do not match the selected data file!\nThe massvolatammogarm cannot be created.\nCheck the entered parameters.");
      return;
    }

    //Extracting all spectra within the given m/z-range.
    final List<double[][]> spectra = MassvoltammogramUtils.extractMZRangeFromScan(scans, mzRange);

    //Adding zeros around the datapoints if the spectra are centroid, so they will be visualized correctly.
    final List<double[][]> processedSpectra;

    //Adding intensity values of 0 around centroid datapoints, so that the massvoltammogram will be visualized correclty.
    if (file == null || file.getScan(0).getSpectrumType() == MassSpectrumType.CENTROIDED) {

      processedSpectra = MassvoltammogramUtils.addZerosToCentroidData(spectra);
      plot.setMassSpectrumType(MassSpectrumType.CENTROIDED);
    } else {

      processedSpectra = spectra;
    }

    //Getting the maximal intensity from all spectra.
    final double maxIntensity = MassvoltammogramUtils.getMaxIntensity(processedSpectra);

    //Removing all datapoints with low intensity values.
    final List<double[][]> spectraWithoutNoise = MassvoltammogramUtils.removeNoise(processedSpectra,
        maxIntensity);

    //Removing excess zeros from the dataset.
    final List<double[][]> spectraWithoutZeros = MassvoltammogramUtils.removeExcessZeros(
        spectraWithoutNoise);

    //Adding the data to the plot for later export.
    plot.addRawScans(scans);
    plot.addRawScansInMzRange(spectra);

    //Calculating the divisor needed to scale the z-axis.
    final double divisor = MassvoltammogramUtils.getDivisor(maxIntensity);

    //Adding all the spectra to the plot.
    MassvoltammogramUtils.addSpectraToPlot(spectraWithoutZeros, divisor, plot);

    //Setting up the plot correctly.
    plot.setAxisLabels("m/z", "Potential / mV",
        "Intensity / 10" + MassvoltammogramUtils.toSupercript((int) Math.log10(divisor)) + " a.u.");
    plot.setFixedBounds(1, potentialRange.lowerEndpoint(), potentialRange.upperEndpoint());
    plot.setFixedBounds(0, mzRange.lowerEndpoint(), mzRange.upperEndpoint());

    String fileName = null;

    if (file != null) {

      fileName = file.getName();

    } else if (featureList != null) {

      fileName = featureList.getName();
    }

    //Adding the plot to a new MZmineTab.
    final MassvoltammogramTab mvTab = new MassvoltammogramTab("Massvoltammogram", plot, fileName);
    MZmineCore.getDesktop().addTab(mvTab);

    setStatus(TaskStatus.FINISHED);
  }
}
