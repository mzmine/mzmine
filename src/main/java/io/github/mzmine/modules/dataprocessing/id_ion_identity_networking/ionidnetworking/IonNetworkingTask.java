/*
 * Copyright 2006-2021 The MZmine Development Team
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
 *
 */

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.MZmineRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityModularType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary.CheckMode;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
      final ModularFeatureList featureLists, MinimumFeatureFilter minFeaturesFilter) {
    super(featureLists.getMemoryMapStorage());
    this.project = project;
    this.featureList = featureLists;
    parameters = parameterSet;

    adductCheckMode = parameterSet.getParameter(IonNetworkingParameters.CHECK_MODE).getValue();
    // tolerances
    mzTolerance = parameterSet.getParameter(IonNetworkingParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(IonNetworkingParameters.MIN_HEIGHT).getValue();
    checkMode = parameterSet.getParameter(IonNetworkingParameters.CHECK_MODE).getValue();

    performAnnotationRefinement = parameterSet
        .getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = parameterSet.getParameter(IonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();
  }

  public IonNetworkingTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists) {
    this(project, parameterSet, featureLists, null);
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
      setStatus(TaskStatus.PROCESSING);
      // create library
      LOG.info("Creating annotation library");
      // add types
      featureList.addRowType(new IonIdentityModularType());

      IonLibraryParameterSet p = parameters.getParameter(IonNetworkingParameters.LIBRARY)
          .getEmbeddedParameters();
      library = new IonNetworkLibrary(p, mzTolerance);
      annotateGroups(library);
      featureList.getAppliedMethods()
          .add(new SimpleFeatureListAppliedMethod(IonNetworkingModule.class, parameters));
      setStatus(TaskStatus.FINISHED);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MZmineRuntimeException(t);
    }
  }


  private void annotateGroups(IonNetworkLibrary library) {
    // get groups
    List<RowGroup> groups = featureList.getGroups();

    if (groups == null || groups.isEmpty()) {
      throw new MZmineRuntimeException(
          "Run grouping before: No groups found for peakList + " + featureList.getName());
    }
    //
    AtomicInteger compared = new AtomicInteger(0);
    AtomicInteger annotPairs = new AtomicInteger(0);
    // for all groups
    groups.parallelStream().forEach(g -> {
      if (!this.isCanceled()) {
        annotateGroup(g, compared, annotPairs);
        stageProgress.addAndGet(1d / groups.size());
      }
    });
    LOG.info("Corr: A total of " + compared.get() + " row2row adduct comparisons with " + annotPairs
        .get() + " annotation pairs");

    refineAndFinishNetworks();
  }

  /**
   * Annotates all rows in a group
   *
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(RowGroup g, AtomicInteger compared, AtomicInteger annotPairs) {
    for (int i = 0; i < g.size() - 1; i++) {
      // check against existing networks
      for (int k = i + 1; k < g.size(); k++) {
        // only if row i and k are correlated
        if (g.isCorrelated(i, k)) {
          compared.incrementAndGet();
          // check for adducts in library
          List<IonIdentity[]> id = library
              .findAdducts(featureList, g.get(i), g.get(k), adductCheckMode, minHeight);
          if (!id.isEmpty()) {
            annotPairs.incrementAndGet();
          }
        }
      }
      // finished.incrementAndGet();
    }
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
      IonNetworkRefinementTask ref = new IonNetworkRefinementTask(project, refineParam,
          featureList);
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
