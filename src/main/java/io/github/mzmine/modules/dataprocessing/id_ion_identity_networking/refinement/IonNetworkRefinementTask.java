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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.refinement;


import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class IonNetworkRefinementTask extends AbstractTask {
  // Logger.
  private static final Logger LOG = Logger.getLogger(IonNetworkRefinementTask.class.getName());

  private int finishedRows;
  private int totalRows;
  private final ModularFeatureList featureList;

  private final ParameterSet parameters;
  private final MZmineProject project;

  // >= trueThreshold delete all other occurance in networks
  private int trueThreshold = 4;
  // delete all other xmers when one was confirmed in MSMS
  private boolean deleteWithoutMonomer = true;
  private boolean deleteSmallerNets = true;
  private boolean filterMinSize = true;
  private boolean deleteSmallNoMajor = false;
  private boolean keepRowOnlyWithID = false;
  private int minNetworkSize = 2;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public IonNetworkRefinementTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);

    this.project = project;
    this.featureList = featureLists;
    parameters = parameterSet;

    finishedRows = 0;
    totalRows = 0;

    // tolerances
    deleteWithoutMonomer =
        parameterSet.getParameter(IonNetworkRefinementParameters.DELETE_WITHOUT_MONOMER).getValue();
    deleteSmallerNets =
        parameterSet.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD).getValue();
    trueThreshold = parameterSet.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD)
        .getEmbeddedParameter().getValue();

    filterMinSize =
        parameterSet.getParameter(IonNetworkRefinementParameters.MIN_NETWORK_SIZE).getValue();
    minNetworkSize = parameterSet.getParameter(IonNetworkRefinementParameters.MIN_NETWORK_SIZE)
        .getEmbeddedParameter().getValue();
    deleteSmallNoMajor =
        parameterSet.getParameter(IonNetworkRefinementParameters.DELETE_SMALL_NO_MAJOR).getValue();
    keepRowOnlyWithID =
        parameterSet.getParameter(IonNetworkRefinementParameters.DELETE_ROWS_WITHOUT_ID).getValue();
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
    refine();
    setStatus(TaskStatus.FINISHED);
  }


  public void refine() {
    // sort and refine
    refine(featureList, deleteSmallerNets, deleteWithoutMonomer, trueThreshold, deleteSmallNoMajor,
        filterMinSize, minNetworkSize, keepRowOnlyWithID);
  }

  /**
   * Delete all without monomer or 1 monomer and >=3 multimers. Delete all smaller networks if one
   * network is the best for all
   * 
   * @param trueThreshold
   * @param minNetworkSize
   * @param filterMinSize
   * @param deleteSmallNoMajor
   */
  public static void refine(ModularFeatureList pkl, boolean deleteSmallerNets, boolean deleteWithoutMonomer,
      int trueThreshold, boolean deleteSmallNoMajor, boolean filterMinSize, int minNetworkSize,
      boolean keepRowOnlyWithID) {
    // sort
    IonNetworkLogic.sortIonIdentities(pkl, true);

    long count = IonNetworkLogic.streamNetworks(pkl).count();
    LOG.info("Ion identity networks before refinement: " + count);

    // min size
    deleteSmallOrWithoutMajor(pkl, filterMinSize, minNetworkSize, deleteSmallNoMajor,
        keepRowOnlyWithID);

    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, false);
    if (deleteWithoutMonomer)
      deleteAllWithoutMonomer(nets);
    if (deleteSmallerNets)
      deleteSmallerNetworks(nets, trueThreshold);

    // remove all identities where networks were marked as deleted
    deleteAllIonsOfDeletedNetworks(pkl.getRows());
    // TODO new network refinement

    count = IonNetworkLogic.streamNetworks(pkl).count();
    LOG.info("Ion identity networks after refinement: " + count);
  }

  /**
   * Delete all ion identities of as deleted marked networks
   *
   * @param rows
   */
  private static void deleteAllIonsOfDeletedNetworks(List<FeatureListRow> rows) {
    for (FeatureListRow row : rows) {
      if (row.hasIonIdentity()) {
        for (int i = 0; i < row.getIonIdentities().size(); i++) {
          IonIdentity id = row.getIonIdentities().get(i);
          if (id.isDeleted() || id.getNetwork() == null || id.getNetwork().isEmpty()) {
            id.delete(row);
            i--;
          }
        }
      }
    }
  }

  /**
   * Delete all smaller networks if one network is the preferred in all rows
   * 
   * @param nets
   * @param trueThreshold
   */
  private static void deleteSmallerNetworks(IonNetwork[] nets, int trueThreshold) {
    for (IonNetwork net : nets) {
      // not deleted
      if (net.size() > 0) {
        // delete small ones
        if (net.size() >= trueThreshold && trueThreshold > 1) {
          if (isBestNet(net)) {
            deleteAllOther(net);
          }
        }
      }
    }
  }

  /**
   * Delete all networks without monomer or with 1 monomer and >=3 multimers
   * 
   * @param nets
   */
  private static void deleteAllWithoutMonomer(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      // not deleted
      if (net.size() > 0) {
        int monomer = 0;
        int multimer = 0;
        for (Entry<FeatureListRow, IonIdentity> e : net.entrySet()) {
          if (e.getValue().getIonType().getMolecules() == 1)
            monomer++;
          else if (e.getValue().getIonType().getMolecules() > 1)
            multimer++;
        }
        // no monomer
        // 1 monomer and >=3 multimers --> delete
        if (monomer == 0 || (monomer == 1 && multimer >= 3))
          net.delete();
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
    for (FeatureListRow row : net.keySet()) {
      IonIdentity id = row.getBestIonIdentity();
      // is best of all rows
      if (id != null && !id.getNetwork().equals(net)) {
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
    for (FeatureListRow row : net.keySet()) {
      Stream.of(IonNetworkLogic.getAllNetworks(row)).forEach(o -> {
        if (net.getID() != o.getID()) {
          o.delete();
        }
      });
    }
  }

  private static int getLinks(IonIdentity best) {
    int links = best.getPartnerRows().size();
    if (best.getMSMSMultimerCount() > 0)
      links++;
    return links;
  }


  /**
   * Delete all networks smaller min size
   * 
   * @param pkl
   * @param minNetSize
   * @param deleteSmallNoMajor
   * @throws Exception
   */
  public static void deleteSmallOrWithoutMajor(FeatureList pkl, boolean filterMinSize, int minNetSize,
      boolean deleteSmallNoMajor, boolean deleteRowsWithoutIon) {
    if (filterMinSize || deleteSmallNoMajor) {
      // need to convert to array first to avoid concurren mod exception
      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, false);
      Arrays.stream(nets).forEach(net -> {
        if ((filterMinSize && net.size() < minNetSize)
            || (deleteSmallNoMajor && !hasMajorIonID(net)))
          net.delete();
      });
    }

    // remove all rows without ion identity?
    if (deleteRowsWithoutIon)
      MZmineCore.runLater(() -> {
        for (int i = 0; i < pkl.getNumberOfRows();)
          if (pkl.getRow(i).hasIonIdentity())
            i++;
          else
            pkl.removeRow(i);
      });
  }

  private static boolean hasMajorIonID(IonNetwork net) {
    return net.values().stream().map(IonIdentity::getIonType).anyMatch(ion -> {
      return equalsType(ion, IonModification.H, 3) || equalsType(ion, IonModification.NA, 0)
          || equalsType(ion, IonModification.NH4, 1) || equalsType(ion, IonModification.H_NEG, 2)
          || equalsType(ion, IonModification.CL, 0) || equalsType(ion, IonModification.FA, 0);
    });
  }

  /**
   * @param testedIon        tested ion
   * @param adduct           the target adduct
   * @param maxModifications maximum number of ion modifications (-2H2O == 2)
   * @return true if adducts equal and modification is within max
   */
  private static boolean equalsType(IonType testedIon, IonModification adduct,
      int maxModifications) {
    return testedIon.getAdduct().equals(adduct) && testedIon.getModCount() <= maxModifications;
  }


  /**
   * Delete xmers if one was verified by msms
   *
   * @param row
   * @param best
   * @param all
   * @return
   */
  private static boolean deleteXmersOnMSMS(FeatureListRow row, IonIdentity best, List<IonIdentity> all,
      int trueThreshold) {
    // check best first
    if (best.getMSMSMultimerCount() > 0) {
      // delete rest of annotations
      for (int i = 1; i < all.size();)
        row.getIonIdentities().get(i).delete(row);

      row.setBestIonIdentity(best);
      return true;
    } else {
      // check rest
      for (IonIdentity other : all) {
        if (other.getMSMSMultimerCount() > 0) {
          row.setBestIonIdentity(other);

          // delete rest of annotations
          for (int i = 1; i < row.getIonIdentities().size(); i++) {
            IonIdentity e = row.getIonIdentities().get(i);
            if (!other.equals(e) && (trueThreshold <= 1 || getLinks(e) < trueThreshold))
              e.delete(row);
          }
          return true;
        }
      }
    }
    return false;
  }


}
