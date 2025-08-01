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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingModule;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary.CheckMode;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class IonNetworkingTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(IonNetworkingTask.class.getName());
  private final ModularFeatureList featureList;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private AtomicDouble stageProgress = new AtomicDouble(0);
  private IonNetworkLibrary library;
  private boolean neverStop = false;

  private double minHeight;
  private IonNetworkLibrary.CheckMode checkMode;

  private CheckMode adductCheckMode;

  private boolean performAnnotationRefinement;
  private IonNetworkRefinementParameters refineParam;

  private MinimumFeatureFilter minFeaturesFilter;

  private MZTolerance mzTolerance;


  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public IonNetworkingTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, MinimumFeatureFilter minFeaturesFilter,
      @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);
    this.project = project;
    this.featureList = featureLists;
    parameters = parameterSet;

    adductCheckMode = parameterSet.getParameter(IonNetworkingParameters.CHECK_MODE).getValue();
    // tolerances
    mzTolerance = parameterSet.getParameter(IonNetworkingParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(IonNetworkingParameters.MIN_HEIGHT).getValue();
    checkMode = parameterSet.getParameter(IonNetworkingParameters.CHECK_MODE).getValue();

    performAnnotationRefinement = parameterSet.getParameter(
        IonNetworkingParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = parameterSet.getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();
  }

  public IonNetworkingTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, @NotNull Instant moduleCallDate) {
    this(project, parameterSet, featureLists, null, moduleCallDate);
  }

  @Override
  public double getFinishedPercentage() {
    return getStatus().equals(TaskStatus.FINISHED) ? 1 : stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Identification of adducts, in-source fragments and clusters in " + featureList.getName()
           + " ";
  }

  @Override
  public void run() {
    try {
      if (featureList.isEmpty()) {
        // nothing to do
        setStatus(TaskStatus.FINISHED);
        return;
      }
      List<RowGroup> groups = featureList.getGroups();
      if (groups == null || groups.isEmpty()) {
        // check if processing contains metaCorrelate - otherwise error out

        final boolean missesGroupingStep = featureList.getAppliedMethods().stream()
            .noneMatch(m -> m.getModule() instanceof CorrelateGroupingModule);

        if (missesGroupingStep) {
          error("Run %s step before: No groups found for feature List %s".formatted(
              CorrelateGroupingModule.NAME, featureList.getName()));
        } else {
          setStatus(TaskStatus.FINISHED);
          return;
        }
      }

      setStatus(TaskStatus.PROCESSING);
      // create library
      LOG.info("Creating annotation library");
      // add types
      featureList.addRowType(new IonIdentityListType());

      PolarityType polarity = FeatureListUtils.getPolarity(featureList, PolarityType.ANY);

      IonLibraryParameterSet p = parameters.getParameter(IonNetworkingParameters.LIBRARY)
          .getEmbeddedParameters();
      library = new IonNetworkLibrary(p, polarity, mzTolerance);
      annotateGroups(library);
      featureList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(IonNetworkingModule.class, parameters,
              getModuleCallDate()));
      setStatus(TaskStatus.FINISHED);
    } catch (Exception t) {
      // just nothing found. no exception actually
      setStatus(TaskStatus.FINISHED);
    }
  }


  private void annotateGroups(IonNetworkLibrary library) {
    // get groups
    List<RowGroup> groups = featureList.getGroups();

    if (groups == null || groups.isEmpty()) {
      throw new MSDKRuntimeException(
          "Run grouping before: No groups found for peakList + " + featureList.getName());
    }
    //
    AtomicInteger compared = new AtomicInteger(0);
    // for all groups
    long annotPairs = groups.parallelStream().mapToLong(g -> {
      if (this.isCanceled()) {
        return 0;
      }
      final long annotations = annotateGroup(g, compared);
      stageProgress.addAndGet(1d / groups.size());
      return annotations;
    }).sum();
    LOG.info("Corr: A total of " + compared.get() + " row2row adduct comparisons with " + annotPairs
             + " annotation pairs");

    refineAndFinishNetworks();
  }

  /**
   * Annotates all rows in a group
   *
   * @param g
   * @param compared
   */
  private long annotateGroup(RowGroup g, AtomicInteger compared) {
    long annotations = 0;
    for (int i = 0; i < g.size() - 1; i++) {
      // check against existing networks
      for (int k = i + 1; k < g.size(); k++) {
        // only if row i and k are correlated
        if (g.isCorrelated(i, k)) {
          compared.incrementAndGet();
          // check for adducts in library
          List<IonIdentity[]> id = library.findAdducts(featureList, g.get(i), g.get(k),
              adductCheckMode, minHeight);
          if (!id.isEmpty()) {
            annotations++;
          }
        }
      }
      // finished.incrementAndGet();
    }
    return annotations;
  }


  private void refineAndFinishNetworks() {
    // create network IDs
    LOG.info("Corr: create annotation network numbers");
    IonNetworkLogic.renumberNetworks(featureList);

    // recalc annotation networks
    IonNetworkLogic.recalcAllAnnotationNetworks(featureList, true);

    if (isCanceled()) {
      return;
    }

    // refinement
    if (performAnnotationRefinement) {
      LOG.info("Corr: Refine annotations");
      IonNetworkRefinementTask ref = new IonNetworkRefinementTask(project, refineParam, featureList,
          getModuleCallDate());
      ref.refine();
    }
    if (isCanceled()) {
      return;
    }

    // recalc annotation networks
    IonNetworkLogic.recalcAllAnnotationNetworks(featureList, true);

    // show all annotations with the highest count of links
    LOG.info("Corr: show most likely annotations");
    IonNetworkLogic.sortIonIdentities(featureList, true);
  }
}
