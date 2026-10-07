/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_lipidannotationcleanup;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.annotation_modules.LipidAnalysisType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.scoring.LipidQcScoringUtils;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task that removes duplicate lipid annotations appearing on multiple feature list rows, keeping
 * the best candidate per annotation string. Winner selection is governed by
 * {@link IonizationPreference} rules resolved hierarchically against the lipid class; classes with
 * no matching rule default to score-first comparison.
 */
public class LipidAnnotationCleanupTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(LipidAnnotationCleanupTask.class.getName());

  private final @NotNull ModularFeatureList featureList;
  private final @NotNull List<IonizationPreference> ionizationPreferences;
  private final @NotNull MultiRowAnnotationCleanupRowHandlingMode rowHandlingMode;
  private final LipidAnalysisType analysisType;
  private final @NotNull DuplicateAnnotationScopeFilter duplicateScope;

  protected LipidAnnotationCleanupTask(@Nullable final MemoryMapStorage storage,
      @NotNull final Instant moduleCallDate,
      @NotNull final LipidAnnotationCleanupParameters parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      @NotNull final FeatureList featureList) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureList = (ModularFeatureList) featureList;
    this.ionizationPreferences = parameters.getValue(
        LipidAnnotationCleanupParameters.ionizationPreferences);
    this.rowHandlingMode = parameters.getValue(LipidAnnotationCleanupParameters.rowHandlingMode);
    analysisType = parameters.getValue(LipidAnnotationCleanupParameters.lipidAnalysisType);
    duplicateScope = parameters.getValue(LipidAnnotationCleanupParameters.duplicateScope);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }

  @Override
  protected void process() {
    // resolve hierarchical preference rules into a flat class-label → ionization map
    // only classes with a matching rule get an entry; classes without an entry fall through
    // to score-first comparison in the planner (isPreferredIonization returns false for both
    // candidates → preferredCmp=0 → scoreCmp decides)
    final Map<String, IonizationType> preferredIonByClass = new LinkedHashMap<>();
    for (final FeatureListRow row : featureList.getRows()) {
      for (final MatchedLipid match : row.getLipidMatches()) {
        final ILipidClass lipidClass = match.getLipidAnnotation().getLipidClass();
        if (lipidClass == null) {
          continue;
        }
        final String classLabel = MultiRowAnnotationCleanupPlanner.lipidClassLabel(match);
        if (preferredIonByClass.containsKey(classLabel)) {
          continue;
        }
        final IonizationType preferred = IonizationPreference.resolve(lipidClass,
            ionizationPreferences);
        if (preferred != null) {
          preferredIonByClass.put(classLabel, preferred);
        }
      }
    }

    // alwaysKeepHighestScore=false, keepHighestScoreByLipidClass=empty
    // classes WITH a preferredIon entry use ionization-first comparison
    // classes WITHOUT an entry: isPreferredIonization() returns false → falls through to score
    final MultiRowAnnotationCleanupOptions options = new MultiRowAnnotationCleanupOptions(
        preferredIonByClass, Set.of(), false, rowHandlingMode, duplicateScope);

    final MultiRowAnnotationCleanupPlan plan = MultiRowAnnotationCleanupPlanner.buildCleanupPlan(
        featureList, analysisType, options);

    if (!plan.hasRemovals()) {
      logger.info("No multi-row lipid annotations to clean up in " + featureList.getName());
      return;
    }

    logger.info("Removing " + plan.removedAnnotationCount() + " duplicate lipid annotations across "
        + plan.affectedRowCount() + " rows in " + featureList.getName());

    MultiRowAnnotationCleanupPlanner.applyCleanupPlan(plan);
    LipidQcScoringUtils.rescoreOverallQualityScores(featureList);
  }

  @Override
  public String getTaskDescription() {
    return "Cleaning up multi-row lipid annotations in " + featureList.getName();
  }
}
