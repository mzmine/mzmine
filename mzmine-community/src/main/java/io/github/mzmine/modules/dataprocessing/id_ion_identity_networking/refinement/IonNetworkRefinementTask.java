/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement;


import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonType.IonTypeStringFlavor;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkNode;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class IonNetworkRefinementTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(IonNetworkRefinementTask.class.getName());
  private final Set<String> mainIonNames;

  private int finishedRows;
  private final int totalRows;
  @NotNull
  private final ParameterSet parameters;
  private final ModularFeatureList featureList;

  // >= trueThreshold delete all other occurance in networks
  private final int trueThreshold;
  // delete all other xmers when one was confirmed in MSMS
  private final boolean requireMonomer;
  private final boolean deleteSmallerNets;
  private final boolean filterMinSize;
  private final boolean requireMajorIon;
  private final boolean keepRowOnlyWithID;
  private final int minNetworkSize;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public IonNetworkRefinementTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);
    this.parameters = parameterSet;
    this.featureList = featureLists;

    finishedRows = 0;
    totalRows = featureLists.getNumberOfRows();

    // tolerances
    requireMonomer = parameterSet.getValue(IonNetworkRefinementParameters.DELETE_WITHOUT_MONOMER);
    deleteSmallerNets = parameterSet.getValue(IonNetworkRefinementParameters.TRUE_THRESHOLD);
    trueThreshold = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        IonNetworkRefinementParameters.TRUE_THRESHOLD, 10);

    filterMinSize = parameterSet.getValue(IonNetworkRefinementParameters.MIN_NETWORK_SIZE);
    minNetworkSize = parameterSet.getEmbeddedParameterValueIfSelectedOrElse(
        IonNetworkRefinementParameters.MIN_NETWORK_SIZE, 1);

    final Optional<IonLibrary> main = parameters.getOptionalValue(
        IonNetworkRefinementParameters.mainIonLibrary);
    requireMajorIon = main.isPresent();
    if (requireMajorIon) {
      // use the string names to ignore the mass that may be slightly different
      mainIonNames = main.get().ions().stream().map(IonNetworkRefinementTask::uniqueID)
          .collect(Collectors.toSet());
    } else {
      mainIonNames = Set.of();
    }

    keepRowOnlyWithID = parameterSet.getValue(
        IonNetworkRefinementParameters.DELETE_ROWS_WITHOUT_ID);
  }

  /**
   * Use name as unique ID to ignore the mass which may be slightly different depending on ion type
   * definition.
   *
   * @return name as unique id
   */
  private static String uniqueID(IonType n) {
    return n.toString(IonTypeStringFlavor.SIMPLE_DEFAULT);
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows / totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Refinement of annotations " + featureList.getName() + " ";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    refine();

    featureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IonNetworkRefinementModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }


  /**
   * Delete all without monomer or 1 monomer and >=3 multimers. Delete all smaller networks if one
   * network is the best for all
   */
  public void refine() {
    if (keepRowOnlyWithID) {
      final List<FeatureListRow> toRemove = featureList.getRows().stream()
          .filter(r -> !r.hasIonIdentity()).toList();
      featureList.removeRows(toRemove);
      LOG.info("Removed %d rows keeping %d with ion identity from %s".formatted(toRemove.size(),
          featureList.getNumberOfRows(), featureList.getName()));
    }

    // sort
    IonNetworkLogic.sortIonIdentities(featureList, true);

    List<IonNetwork> nets = IonNetworkLogic.getAllNetworksList(featureList.getRows(), null, false);

    int count = nets.size();
    LOG.info("Ion identity networks before refinement: " + count);

    if (deleteSmallerNets) {
      deleteSmallerNetworks(nets, trueThreshold);
    }

    for (IonNetwork net : nets) {
      final boolean keep = checkKeepNetwork(net);
      if (!keep) {
        net.delete();
      }
    }

    count = IonNetworkLogic.getAllNetworksList(featureList.getRows(), null, false).size();
    IonNetworkLogic.sortIonIdentities(featureList, true);
    LOG.info("Ion identity networks after refinement: " + count);
  }

  private boolean checkKeepNetwork(IonNetwork net) {
    return checkNetSize(net) //
        && (!requireMonomer || hasMonomer(net)) //
        && (!requireMajorIon || hasMajorIon(net));
  }

  private boolean hasMajorIon(IonNetwork net) {
    for (IonNetworkNode node : net.getNodes()) {
      if (mainIonNames.contains(uniqueID(node.ion().getIonType()))) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @param net
   * @return true if multiple monomers or if 1 monomer and not too many multimers
   */
  public static boolean hasMonomer(IonNetwork net) {
    int mono = 0;
    for (IonNetworkNode node : net.getNodes()) {
      if (node.ion().getIonType().molecules() == 1) {
        mono++;
      }
    }
    return mono > 1 || (mono == 1 && net.size() < 4);
  }


  private boolean checkNetSize(IonNetwork net) {
    return net.size() >= minNetworkSize;
  }

  /**
   * Delete all smaller networks if one network is the preferred in all rows
   *
   * @param nets
   * @param trueThreshold
   */
  private static void deleteSmallerNetworks(List<IonNetwork> nets, int trueThreshold) {
    if (trueThreshold < 2) {
      return;
    }
    for (IonNetwork net : nets) {
      // delete small ones
      if (net.size() >= trueThreshold) {
        if (isBestNet(net)) {
          deleteAllOther(net);
        }
      }
    }
  }


  /**
   * Is best network in all rows?
   *
   * @param net
   * @return
   */
  private static boolean isBestNet(IonNetwork net) {
    for (var node : net.getNodes()) {
      IonIdentity id = node.row().getBestIonIdentity();
      // is best of all rows
      if (id != null && !net.equals(id.getNetwork())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Keep net but delete all other networks from all rows
   *
   * @param net
   */
  private static void deleteAllOther(IonNetwork net) {
    for (var node : net.getNodes()) {
      Stream.of(IonNetworkLogic.getAllNetworks(node.row())).forEach(o -> {
        if (net.getID() != o.getID()) {
          o.delete();
        }
      });
    }
  }

}
