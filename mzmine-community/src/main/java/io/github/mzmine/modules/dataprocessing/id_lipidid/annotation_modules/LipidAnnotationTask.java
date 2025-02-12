/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidIon;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.FragmentScanSelection;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * Task to search and annotate lipids in feature list
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class LipidAnnotationTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedSteps;
  private double totalSteps;
  private final FeatureList featureList;
  private ILipidClass[] selectedLipids;
  private CustomLipidClass[] selectedCustomLipidClasses;
  private final int minChainLength;
  private final int maxChainLength;
  private final int maxDoubleBonds;
  private final int minDoubleBonds;
  private final Boolean onlySearchForEvenChains;
  private final MZTolerance mzTolerance;
  private final MZTolerance mzToleranceMS2;
  private final Boolean searchForMSMSFragments;
  private final Boolean keepUnconfirmedAnnotations;
  private double minMsMsScore;
  private final IonizationType[] ionizationTypesToIgnore;
  private final ParameterSet parameters;
  private final FragmentScanSelection scanMergeSelect;

  public LipidAnnotationTask(ParameterSet parameters, FeatureList featureList,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureList = featureList;
    this.parameters = parameters;

    this.minChainLength = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getParameter(LipidAnnotationChainParameters.minChainLength)
        .getValue();
    this.maxChainLength = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getParameter(LipidAnnotationChainParameters.maxChainLength)
        .getValue();
    this.minDoubleBonds = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getParameter(LipidAnnotationChainParameters.minDBEs).getValue();
    this.maxDoubleBonds = parameters.getParameter(LipidAnnotationParameters.lipidChainParameters)
        .getEmbeddedParameters().getParameter(LipidAnnotationChainParameters.maxDBEs).getValue();
    this.onlySearchForEvenChains = parameters.getParameter(
            LipidAnnotationParameters.lipidChainParameters).getEmbeddedParameters()
        .getParameter(LipidAnnotationChainParameters.onlySearchForEvenChainLength).getValue();
    this.mzTolerance = parameters.getParameter(LipidAnnotationParameters.mzTolerance).getValue();
    Object[] selectedObjects = parameters.getParameter(LipidAnnotationParameters.lipidClasses)
        .getValue();
    this.searchForMSMSFragments = parameters.getParameter(
        LipidAnnotationParameters.searchForMSMSFragments).getValue();
    if (searchForMSMSFragments.booleanValue()) {
      var ms2Params = parameters.getParameter(LipidAnnotationParameters.searchForMSMSFragments)
          .getEmbeddedParameters();
      this.mzToleranceMS2 = ms2Params.getParameter(LipidAnnotationMSMSParameters.mzToleranceMS2)
          .getValue();
      this.keepUnconfirmedAnnotations = ms2Params.getParameter(
          LipidAnnotationMSMSParameters.keepUnconfirmedAnnotations).getValue();
      this.minMsMsScore = (ms2Params.getParameter(LipidAnnotationMSMSParameters.minimumMsMsScore)
          .getValue());

      this.scanMergeSelect = ms2Params.getParameter(
              LipidAnnotationMSMSParameters.spectraMergeSelect)
          .createFragmentScanSelection(getMemoryMapStorage());
    } else {
      this.mzToleranceMS2 = MZTolerance.FIFTEEN_PPM_OR_FIVE_MDA; // should not be used but still set
      this.keepUnconfirmedAnnotations = true; // keep old default
      scanMergeSelect = FragmentScanSelection.createAllInputFragmentScansSelect(
          getMemoryMapStorage()); // all input scans was and is the default
    }
    this.selectedCustomLipidClasses = null;
    if (parameters.getParameter(LipidAnnotationParameters.customLipidClasses).getValue()) {
      this.selectedCustomLipidClasses = LipidAnnotationParameters.customLipidClasses.getEmbeddedParameters()
          .getParameter(CustomLipidClassParameters.customLipidClassChoices).getChoices();
    }

    // Convert Objects to LipidClasses
    List<ILipidClass> selectedLipidClassesList = new java.util.ArrayList<>(
        Arrays.stream(selectedObjects).filter(o -> o instanceof LipidClasses)
            .map(o -> (ILipidClass) o).toList());

    // add custom lipid classes if available
    if (selectedCustomLipidClasses != null) {
      selectedLipidClassesList.addAll(Arrays.asList(selectedCustomLipidClasses));
    }
    this.selectedLipids = selectedLipidClassesList.toArray(new ILipidClass[0]);
    if (parameters.getParameter(LipidAnnotationParameters.advanced).getValue()) {
      AdvancedParametersParameter<AdvancedLipidAnnotationParameters> advancedParam = parameters.getParameter(
          LipidAnnotationParameters.advanced);
      this.ionizationTypesToIgnore = advancedParam.getValueOrDefault(
          AdvancedLipidAnnotationParameters.IONS_TO_IGNORE, new IonizationType[]{});
    } else {
      ionizationTypesToIgnore = null;
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalSteps == 0) {
      return 0;
    }
    return (finishedSteps) / totalSteps;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Annotate lipids in " + featureList;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting lipid annotation in " + featureList);

    List<FeatureListRow> rows = featureList.getRows();
    if (featureList instanceof ModularFeatureList) {
      featureList.addRowType(new LipidMatchListType());
    }
    totalSteps = rows.size();
    Set<PolarityType> polarityTypes = getPolarityTypes();

    // build lipid species database
    List<LipidIon> lipidDatabase = LipidAnnotationUtils.buildLipidDatabase(selectedLipids,
        minChainLength, maxChainLength, minDoubleBonds, maxDoubleBonds, onlySearchForEvenChains,
        ionizationTypesToIgnore, polarityTypes);
    List<LipidIon> sortedLipidDatabase = lipidDatabase.stream()
        .sorted(Comparator.comparingDouble(LipidIon::mz)).toList();

    rows.parallelStream().forEach(row -> {
      Range<Double> mzTolRange = mzTolerance.getToleranceRange(row.getAverageMZ());
      double lowerEdge = mzTolRange.lowerEndpoint();
      double upperEdge = mzTolRange.upperEndpoint();
      int index = BinarySearch.binarySearch(lowerEdge, DefaultTo.GREATER_EQUALS,
          sortedLipidDatabase.size(), i -> sortedLipidDatabase.get(i).mz());
      if (index >= 0) {
        for (int i = index; i < sortedLipidDatabase.size(); i++) {
          if (isCanceled()) {
            return;
          }

          LipidAnnotationUtils.findPossibleLipid(sortedLipidDatabase.get(i), row, parameters,
              mzTolerance, mzToleranceMS2, searchForMSMSFragments, minMsMsScore,
              keepUnconfirmedAnnotations,
              sortedLipidDatabase.get(i).lipidAnnotation().getLipidClass().getCoreClass(),
              scanMergeSelect);

          if (upperEdge < sortedLipidDatabase.get(i).mz()) {
            break;
          }
        }
      }
      finishedSteps++;
    });

    // Add task description to featureList
    (featureList).addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Lipid annotation", LipidAnnotationModule.class,
            parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished lipid annotation task for " + featureList);
  }

  @NotNull
  private Set<PolarityType> getPolarityTypes() {
    Set<PolarityType> polarityTypes = new HashSet<>();
    List<RawDataFile> rawDataFiles = featureList.getRawDataFiles();
    for (RawDataFile raw : rawDataFiles) {
      List<PolarityType> dataPolarity = raw.getDataPolarity();
      polarityTypes.addAll(dataPolarity);
    }
    return polarityTypes;
  }

}
