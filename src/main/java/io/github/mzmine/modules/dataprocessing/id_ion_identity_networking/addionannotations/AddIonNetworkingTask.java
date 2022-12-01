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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkSorter;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementParameters;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement.IonNetworkRefinementTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Add row/ionidentites to existing networks
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class AddIonNetworkingTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(AddIonNetworkingTask.class.getName());
  private final ModularFeatureList featureList;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final double minHeight;
  private final boolean performAnnotationRefinement;
  private final IonNetworkRefinementParameters refineParam;
  private final MZTolerance mzTolerance;
  private AtomicDouble stageProgress = new AtomicDouble(0);
  private IonNetworkLibrary library;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public AddIonNetworkingTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);
    this.project = project;
    this.featureList = featureLists;
    parameters = parameterSet;

    mzTolerance = parameterSet.getParameter(AddIonNetworkingParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(AddIonNetworkingParameters.MIN_HEIGHT).getValue();

    performAnnotationRefinement =
        parameterSet.getParameter(AddIonNetworkingParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = parameterSet.getParameter(AddIonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();
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
      library = new IonNetworkLibrary(
          parameters.getParameter(AddIonNetworkingParameters.LIBRARY).getEmbeddedParameters(),
          mzTolerance);
      annotateGroups(library);

      setStatus(TaskStatus.FINISHED);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  private void annotateGroups(IonNetworkLibrary library) {
    LOG.info("Starting adduct detection on groups of peaklist " + featureList.getName());
    // get groups
    List<RowGroup> groups = featureList.getGroups();

    if (groups == null || groups.isEmpty()) {
      throw new MSDKRuntimeException(
          "Run grouping before: No groups found for peakList + " + featureList.getName());
    }
    //
    AtomicInteger compared = new AtomicInteger(0);
    AtomicInteger annotPairs = new AtomicInteger(0);
    // for all groups
    groups.parallelStream().forEach(g -> {
      if (!this.isCanceled()) {
        annotateGroup(library, g, compared, annotPairs);
        stageProgress.addAndGet(1d / groups.size());
      }
    });
    LOG.info("Corr: A total of " + compared.get() + " row2row adduct comparisons with "
             + annotPairs.get() + " annotation pairs");

    refineAndFinishNetworks();
  }

  /**
   * Annotates all rows in a group
   *
   * @param library
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(IonNetworkLibrary library, RowGroup g,
      // AtomicInteger finished,
      AtomicInteger compared, AtomicInteger annotPairs) {
    // all networks of this group
    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(g.getRows(), false);

    for (int i = 0; i < g.size(); i++) {
      FeatureListRow row = g.get(i);
      // min height
      if (g.get(i).getBestFeature().getHeight() >= minHeight) {
        for (IonNetwork net : nets) {
          if (!net.isUndefined()) {
            // only if not already in network
            if (!net.containsKey(row)) {
              // check against existing networks
              if (isCorrelated(g, g.get(i), net)) {
                compared.incrementAndGet();
                // check for adducts in library
                IonIdentity id = library.findAdducts(g.get(i), net);
                if (id != null) {
                  annotPairs.incrementAndGet();
                }
              }
            }
          }
        }
      }
      // finished.incrementAndGet();
    }
  }


  /**
   * minimum correlation between row and network
   *
   * @param g   group
   * @param a   feature list row
   * @param net ion identity network
   * @return true if correlation is greater than 0.5
   */
  private boolean isCorrelated(RowGroup g, FeatureListRow a, IonNetwork net) {
    int n = net.size();
    int correlated = 0;
    for (FeatureListRow b : net.keySet()) {
      if (g.isCorrelated(a, b)) {
        correlated++;
      }
    }
    return (double) correlated / (double) n >= 0.5;
  }

  private void refineAndFinishNetworks() {
    // create network IDs
    LOG.info("Corr: create annotation network numbers");
    AtomicInteger netID = new AtomicInteger(0);
    IonNetworkLogic
        .streamNetworks(featureList,
            new IonNetworkSorter(SortingProperty.RT, SortingDirection.Ascending), false)
        .forEach(n -> {
          n.setMzTolerance(library.getMzTolerance());
          n.setID(netID.getAndIncrement());
        });

    // recalc annotation networks
    IonNetworkLogic.recalcAllAnnotationNetworks(featureList, true);

    if (isCanceled()) {
      return;
    }

    // refinement
    if (performAnnotationRefinement) {
      LOG.info("Corr: Refine annotations");
      IonNetworkRefinementTask ref = new IonNetworkRefinementTask(project, refineParam,
          featureList, getModuleCallDate());
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
