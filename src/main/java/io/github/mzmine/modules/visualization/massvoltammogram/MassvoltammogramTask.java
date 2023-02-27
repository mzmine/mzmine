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
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramFromFeatureListParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.io.MassvoltammogramFromFileParameters;
import io.github.mzmine.modules.visualization.massvoltammogram.plot.ExtendedPlot3DPanel;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.MassvoltammogramUtils;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.PlotData;
import io.github.mzmine.modules.visualization.massvoltammogram.utils.ReactionMode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class MassvoltammogramTask extends AbstractTask {

  //Raw Data
  private RawDataFile file;
  private ModularFeatureList featureList;

  //Parameter
  private ScanSelection scanSelection;
  private final ReactionMode reactionMode;
  private final double delayTime; //In s.
  private double potentialRampSpeed; //In mV/s.
  private final Range<Double> potentialRange;
  private double stepSize; //In mV.
  private final Range<Double> mzRange;

  //Potentials in mV.
  private double startPotential;
  private double endPotential;

  //Plot Data
  private MassSpectrumType massSpectrumType;

  public MassvoltammogramTask(@NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    //Setting up the parameter that depend on a raw data file or a feature list being chosen.
    if (parameters instanceof MassvoltammogramFromFileParameters) {

      file = MassvoltammogramFromFileParameters.files.getValue().getMatchingRawDataFiles()[0];
      scanSelection = parameters.getValue(MassvoltammogramFromFileParameters.scanSelection);
      featureList = null;

    } else if (parameters instanceof MassvoltammogramFromFeatureListParameters) {

      featureList = MassvoltammogramFromFeatureListParameters.featureList.getValue()
          .getMatchingFeatureLists()[0];
      file = null;
      scanSelection = null;
    }

    //Setting up the remaining parameters.
    potentialRampSpeed = parameters.getValue(MassvoltammogramFromFileParameters.potentialRampSpeed);
    stepSize = parameters.getValue(MassvoltammogramFromFileParameters.stepSize);
    potentialRange = parameters.getValue(MassvoltammogramFromFileParameters.potentialRange);
    mzRange = parameters.getValue(MassvoltammogramFromFileParameters.mzRange);
    reactionMode = parameters.getValue(MassvoltammogramFromFileParameters.reactionMode);
    delayTime = parameters.getValue(MassvoltammogramFromFileParameters.delayTime);

  }

  @Override
  public String getTaskDescription() {
    return "Creating Massvoltammogram";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  /**
   * Todo
   * <p>
   * MassvoltammogramTab todo
   * mz-Bereich aller plots angleichen
   * mzParameter Übernahme korrigieren
   * double centroid Abweichung
   * MassvoltammogramScan Objekt einführen
   * Centroid Datenpunkte mit Intensität 0 nicht hinzufügen
   */

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    //Setting the start and end potential depending on the selected reaction type.
    setPotentials();

    //Setting the mass spectrum type for the used data.
    setMassSpectrumType();

    //Extracting the needed scans from the data source.
    List<double[][]> scans = getScans();

    //Checking weather the scans were extracted correctly.
    if (scans.isEmpty()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("The entered parameters do not match the selected data file!"
          + "\nCheck the entered parameters.");
      return;
    }

    //Extracting the spectra within the given m/z-range.
    final List<double[][]> scansInMzRange = getScansInMzRange(scans);

    //Getting the max intensity of all scans used for processing and formatting the plot.
    final double maxIntensity = MassvoltammogramUtils.getMaxIntensity(scansInMzRange);

    //Processing the scans to remove excess datapoints.
    final List<double[][]> processedScans = processScans(scansInMzRange, maxIntensity);

    //Creating new 3D Plot.
    final ExtendedPlot3DPanel plot = new ExtendedPlot3DPanel();

    //Adding all the spectra to the plot and formatting it.
    MassvoltammogramUtils.addSpectraToPlot(processedScans, maxIntensity, plot);
    formatPlot(plot, maxIntensity);

    //Adding the data to the plot for later export.
    plot.addPlotData(new PlotData(massSpectrumType, scans, scansInMzRange));

    //Adding the plot to a new MZmineTab.
    final MassvoltammogramTab mvTab = new MassvoltammogramTab("Massvoltammogram", plot,
        getFileName());
    MZmineCore.getDesktop().addTab(mvTab);

    setStatus(TaskStatus.FINISHED);
  }


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

  private void setMassSpectrumType() {

    if (file == null || file.getScan(0).getSpectrumType().isCentroided()) {
      massSpectrumType = MassSpectrumType.CENTROIDED;

    } else {
      massSpectrumType = file.getScan(0).getSpectrumType();
    }
  }

  private List<double[][]> getScans() {

    if (file != null) {
      return MassvoltammogramUtils.getScansFromRawDataFile(file, scanSelection, delayTime,
          startPotential, endPotential, potentialRampSpeed, stepSize);

    } else if (featureList != null) {
      return MassvoltammogramUtils.getScansFromFeatureList(featureList, delayTime, startPotential,
          endPotential, potentialRampSpeed, stepSize);
    }

    return new ArrayList<>();
  }

  private List<double[][]> getScansInMzRange(List<double[][]> scans) {

    final List<double[][]> scansInMzRange = MassvoltammogramUtils.extractMZRangeFromScan(scans,
        mzRange);

    if (file == null || massSpectrumType.isCentroided()) {
      return MassvoltammogramUtils.addZerosToCentroidData(scansInMzRange);

    } else {

      return scans;
    }
  }

  private List<double[][]> processScans(List<double[][]> scans, double maxIntensity) {

    //Removing all datapoints with low intensity values.
    final List<double[][]> scansWithoutNoise = MassvoltammogramUtils.removeNoise(scans,
        maxIntensity);

    //Removing excess zeros from the dataset.
    return MassvoltammogramUtils.removeExcessZeros(scansWithoutNoise);
  }

  private void formatPlot(ExtendedPlot3DPanel plot, double maxIntensity) {

    //Calculating the divisor needed to scale the z-axis.
    final double divisor = MassvoltammogramUtils.getDivisor(maxIntensity);

    //Setting up the plots axis.
    plot.setAxisLabels("m/z", "Potential / mV",
        "Intensity / 10" + MassvoltammogramUtils.toSupercript((int) Math.log10(divisor)) + " a.u.");
    plot.setFixedBounds(1, potentialRange.lowerEndpoint(), potentialRange.upperEndpoint());
    plot.setFixedBounds(0, mzRange.lowerEndpoint(), mzRange.upperEndpoint());
  }


  private String getFileName() {

    if (file != null) {
      return file.getName();

    } else if (featureList != null) {
      return featureList.getName();
    }

    return "";
  }

}
