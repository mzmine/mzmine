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

package io.github.mzmine.modules.visualization.scan_histogram;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.CorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.scan_histogram.chart.MzDeltaCorrelationHistogramTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.TxtWriter;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class CorrelatedFeaturesMzHistogramTask extends AbstractTask {

  private final ModularFeatureList flist;
  private final Double minScore;
  private final Boolean limitToDoubleMz;
  private final Boolean useRtRange;
  private final Range<Double> rtRange;
  private final Range<Double> mzRange;
  private final File outputFile;
  private final File outputFileNeutralMasses;
  private final Boolean saveToFile;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private MzDeltaCorrelationHistogramTab tab;
  private final ParameterSet parameters;

  public CorrelatedFeaturesMzHistogramTask(ModularFeatureList flist, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.flist = flist;
    this.parameters = parameters;
    minScore = parameters.getParameter(CorrelatedFeaturesMzHistogramParameters.minCorr).getValue();
    limitToDoubleMz = parameters.getParameter(
        CorrelatedFeaturesMzHistogramParameters.limitToDoubleMz).getValue();
    useRtRange = parameters.getParameter(CorrelatedFeaturesMzHistogramParameters.rtRange)
        .getValue();
    rtRange = parameters.getParameter(CorrelatedFeaturesMzHistogramParameters.rtRange)
        .getEmbeddedParameter().getValue();
    mzRange = parameters.getParameter(CorrelatedFeaturesMzHistogramParameters.mzRange).getValue();

    saveToFile = parameters.getParameter(CorrelatedFeaturesMzHistogramParameters.saveToFile)
        .getValue();
    outputFile =
        saveToFile ? parameters.getParameter(CorrelatedFeaturesMzHistogramParameters.saveToFile)
            .getEmbeddedParameter().getValue() : null;

    outputFileNeutralMasses =
        outputFile != null ? FileAndPathUtil.getRealFilePathWithSuffix(outputFile, "_neutral",
            "csv") : null;
  }

  @Override
  public String getTaskDescription() {
    return "Creating m/z delta histogram of correlated feature in " + flist.getName();
  }

  @Override
  public double getFinishedPercentage() {
    if (tab == null) {
      return 0;
    }
    int totalScans = tab.getTotalScans();
    if (totalScans == 0) {
      return 0;
    } else {
      return (double) tab.getProcessedScans() / totalScans;
    }
  }

  public ModularFeatureList getFeatureList() {
    return flist;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    StringBuilder csvOutput = new StringBuilder();
    StringBuilder csvOutputNeutralMass = new StringBuilder();

    DoubleArrayList deltaMZList = new DoubleArrayList();
    DoubleArrayList deltaMZToNeutralMassList = new DoubleArrayList();

    int counter = 0;
    for (RowsRelationship r2r : flist.getMs1CorrelationMap().values()) {
      if (r2r instanceof R2RFullCorrelationData corr) {
        FeatureListRow a = corr.getRowA();
        FeatureListRow b = corr.getRowB();
        if (corr.getScore() >= minScore && checkRT(a, b) && checkMz(a, b) && limitToDoubleMz(corr,
            a, b)) {
          // get m/z difference for each file that was correlated
          for (var entry : corr.getCorrFeatureShape().entrySet()) {
            RawDataFile raw = entry.getKey();
            CorrelationData corrData = entry.getValue();
            if (corrData.getPearsonR() >= minScore) {
              Double mza = a.getFeature(raw).getMZ();
              Double mzb = b.getFeature(raw).getMZ();
              double mzDelta = Math.abs(mza - mzb);

              deltaMZList.add(mzDelta);

              if (saveToFile) {
                appendRow(csvOutput, a, b, raw, corrData.getPearsonR());
              }

              IonIdentity ionA = a.getBestIonIdentity();
              IonIdentity ionB = b.getBestIonIdentity();
              // delta to neutral mass
              if (ionA != null && ionB != null) {
//                if (ionA.getNetID() != ionB.getNetID()) {
                double neutralMassDeltaA = mzb - ionA.getNetwork().getNeutralMass();
                deltaMZToNeutralMassList.add(neutralMassDeltaA);
                double neutralMassDeltaB = mza - ionB.getNetwork().getNeutralMass();
                deltaMZToNeutralMassList.add(neutralMassDeltaB);

                appendRow(csvOutputNeutralMass, a, b, raw, corrData.getPearsonR());
                appendRow(csvOutputNeutralMass, b, a, raw, corrData.getPearsonR());
//                }
              } else if (ionA != null) {
                double neutralMassDeltaA = mzb - ionA.getNetwork().getNeutralMass();
                deltaMZToNeutralMassList.add(neutralMassDeltaA);
                appendRow(csvOutputNeutralMass, a, b, raw, corrData.getPearsonR());
              } else if (ionB != null) {
                double neutralMassDeltaB = mza - ionB.getNetwork().getNeutralMass();
                deltaMZToNeutralMassList.add(neutralMassDeltaB);
                appendRow(csvOutputNeutralMass, b, a, raw, corrData.getPearsonR());
              }

              counter++;
            }
          }
        }
      }
    }

    logger.info("Total of " + counter + " correlated features");

    if (saveToFile) {
      logger.info("Writing correlation results to file " + outputFile.getAbsolutePath());
      String content = "";
      if (!outputFile.exists()) {
        content += Arrays.stream(Header.values()).map(Object::toString)
            .collect(Collectors.joining(","));
        content += "\n";
      }
      content += csvOutput.toString();

      TxtWriter.write(content, outputFile, "csv", true);

      content = "";
      if (!outputFileNeutralMasses.exists()) {
        content += Arrays.stream(Header.values()).map(Object::toString)
            .collect(Collectors.joining(","));
        content += "\n";
      }
      content += csvOutputNeutralMass.toString();
      TxtWriter.write(content, outputFileNeutralMasses, "csv", true);
    }

    // create histogram dialog
    Platform.runLater(() -> {
      tab = new MzDeltaCorrelationHistogramTab(flist, deltaMZList, deltaMZToNeutralMassList,
          "m/z delta correlation histogram", "delta m/z", parameters);
      MZmineCore.getDesktop().addTab(tab);
    });

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished mz delta histogram of correlated features in " + flist.getName());
  }

  /**
   * Appends a line of correlation information to a csv
   *
   * @param csvOutput   will be appended by the row and a new line
   * @param a           correlated feature
   * @param b           correlated features
   * @param raw         the raw data file of the correlated feature
   * @param pearsonCorr the Pearson correlation
   */
  private void appendRow(StringBuilder csvOutput, FeatureListRow a, FeatureListRow b,
      RawDataFile raw, double pearsonCorr) {
    Feature fa = a.getFeature(raw);
    Feature fb = b.getFeature(raw);

    Double mza = fa.getMZ();
    Double mzb = fb.getMZ();
    double mzDelta = Math.abs(mza - mzb);

    IonIdentity ionA = a.getBestIonIdentity();
    IonIdentity ionB = b.getBestIonIdentity();

    Double neutralMassA = ionA != null ? ionA.getNetwork().getNeutralMass() : null;
    Double neutralMassB = ionB != null ? ionB.getNetwork().getNeutralMass() : null;

    String row = Arrays.stream(Header.values()).map(h -> String.valueOf(switch (h) {
      case id1 -> a.getID();
      case id2 -> b.getID();
      case rt1 -> fa.getRT().toString();
      case rt2 -> fb.getRT().toString();
      case mz1 -> mza.toString();
      case mz2 -> mzb.toString();
      case delta_mz -> String.valueOf(mzDelta);
      case Pearson_r -> String.valueOf(pearsonCorr);
      case ion_1 -> ionA != null ? ionA.toString() : "";
      case ion_2 -> ionB != null ? ionB.toString() : "";
      case neutralmass_1 -> neutralMassA != null ? neutralMassA : "";
      case neutralmass_2 -> neutralMassB != null ? neutralMassB : "";
      case delta_neutralmass1_mz2 -> neutralMassA != null ? mzb - neutralMassA : "";
    })).collect(Collectors.joining(","));

    csvOutput.append(row);
    csvOutput.append("\n");
  }

  private boolean limitToDoubleMz(RowsRelationship corr, FeatureListRow a, FeatureListRow b) {
    if (!limitToDoubleMz) {
      return true;
    } else {
      double mzDelta = corr.getAbsMzDelta();
      return mzDelta < a.getAverageMZ() && mzDelta < b.getAverageMZ();
    }
  }

  private boolean checkMz(FeatureListRow a, FeatureListRow b) {
    return mzRange.contains(a.getAverageMZ()) && mzRange.contains(b.getAverageMZ());
  }

  private boolean checkRT(FeatureListRow a, FeatureListRow b) {
    return !useRtRange || (a.getAverageRT() != null && rtRange.contains(
        a.getAverageRT().doubleValue()) && b.getAverageRT() != null && rtRange.contains(
        b.getAverageRT().doubleValue()));
  }

  enum Header {
    id1, id2, rt1, rt2, mz1, mz2, delta_mz, Pearson_r, ion_1, ion_2, neutralmass_1, neutralmass_2, delta_neutralmass1_mz2
  }

}
