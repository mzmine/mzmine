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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.MZmineRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkCondensedRelation;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkHeteroCondensedRelation;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkModificationRelation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Find relationships between {@link IonNetwork}s
 */
public class IonNetRelationsTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(IonNetRelationsTask.class.getName());
  private final ModularFeatureList featureList;
  private final ParameterSet parameters;
  private final MZmineProject project;
  private AtomicDouble stageProgress = new AtomicDouble(0);
  private IonModification[] mods;
  private MZTolerance mzTol;
  private boolean searchCondensed;
  private boolean searchHeteroCondensed;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public IonNetRelationsTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists) {
    super(featureLists.getMemoryMapStorage());

    this.project = project;
    this.featureList = featureLists;
    parameters = parameterSet;

    mods = parameterSet.getParameter(IonNetRelationsParameters.ADDUCTS).getValue()[1];
    mzTol = parameterSet.getParameter(IonNetRelationsParameters.MZ_TOL).getValue();
    // tolerances
    searchCondensed =
        parameterSet.getParameter(IonNetRelationsParameters.SEARCH_CONDENSED_MOL).getValue();
    searchHeteroCondensed =
        parameterSet.getParameter(IonNetRelationsParameters.SEARCH_CONDENSED_HETERO_MOL).getValue();
  }

  public static int checkForModifications(MZTolerance mzTol, IonModification[] mods,
      IonNetwork[] nets) {
    int counter = 0;
    for (int i = 0; i < nets.length - 1; i++) {
      for (int j = i + 1; j < nets.length; j++) {
        if (checkForModifications(mzTol, mods, nets[i], nets[j])) {
          counter++;
        }
      }
    }
    return counter;
  }

  private static boolean checkForModifications(MZTolerance mzTol, IonModification[] mods,
      IonNetwork a, IonNetwork b) {
    // ensure a.mass < b.mass
    if (a.getNeutralMass() > b.getNeutralMass()) {
      IonNetwork tmp = a;
      a = b;
      b = tmp;
    }

    IonModification mod =
        checkForModifications(mzTol, mods, a.getNeutralMass(), b.getNeutralMass());
    if (mod != null) {
      IonNetworkModificationRelation rel = new IonNetworkModificationRelation(a, b, mod);
      a.addRelation(b, rel);
      b.addRelation(a, rel);
      return true;
    } else {
      return false;
    }
  }

  public static IonModification checkForModifications(MZTolerance mzTol, IonModification[] mods,
      double a, double b) {
    // ensure a.mass < b.mass
    if (a > b) {
      double tmp = a;
      a = b;
      b = tmp;
    }
    double diff = Math.abs(b - a);

    for (IonModification mod : mods) {
      // e.g. -H2O ~ -18
      if (mzTol.checkWithinTolerance(diff, mod.getAbsMass())) {
        return mod;
      }
    }
    return null;
  }

  public static int checkForCondensedModifications(MZTolerance mzTol, IonModification[] mods,
      IonNetwork[] nets) {
    int counter = 0;
    for (int i = 0; i < nets.length - 1; i++) {
      for (int j = i + 1; j < nets.length; j++) {
        if (checkForCondensedModifications(mzTol, mods, nets[i], nets[j])) {
          counter++;
        }
      }
    }
    return counter;
  }

  /**
   * Search for condensed molecules: e.g., two sugars 2 C6H12O6 --> C12H22O11 + H2O (modifications
   * possible, e.g., when molecules where different (deoxycholic acid + cholic acid: mod= -O)
   *
   * @param mzTol
   * @param mods
   * @param a
   * @param b
   * @return
   */
  public static boolean checkForCondensedModifications(MZTolerance mzTol, IonModification[] mods,
      IonNetwork a, IonNetwork b) {
    // ensure a.mass < b.mass
    if (a.getNeutralMass() > b.getNeutralMass()) {
      IonNetwork tmp = a;
      a = b;
      b = tmp;
    }

    IonModification[] mod =
        checkForCondensedModifications(mzTol, mods, a.getNeutralMass(), b.getNeutralMass());
    if (mod != null) {
      IonNetworkCondensedRelation rel = new IonNetworkCondensedRelation(a, b, mod);
      a.addRelation(b, rel);
      b.addRelation(a, rel);
      return true;
    }
    return false;
  }

  public static IonModification[] checkForCondensedModifications(MZTolerance mzTol,
      IonModification[] mods, double massA, double massB) {
    // ensure a.mass < b.mass
    if (massA > massB) {
      double tmp = massA;
      massA = massB;
      massB = tmp;
    }

    IonModification water = IonModification.H2O;
    // condense a and subtract water
    double diff = Math.abs(massB - (massA * 2 - water.getAbsMass()));
    // check -H2O -> diff = 0
    if (mzTol.checkWithinTolerance(diff, 0d)) {
      return new IonModification[]{water};
    }

    for (IonModification mod : mods) {
      // e.g. -H2O ~ -18
      if (mzTol.checkWithinTolerance(diff, mod.getAbsMass())) {
        return new IonModification[]{water, mod};
      }
    }
    return null;
  }

  /**
   * Checks for any modification of the type X+Y --> XY -H2O (condensation)
   *
   * @param mzTol
   * @param mods
   * @param nets
   * @return
   */
  public static int checkForHeteroCondensed(MZTolerance mzTol, IonModification[] mods,
      IonNetwork[] nets) {
    int counter = 0;
    for (int i = 0; i < nets.length - 2; i++) {
      for (int j = i + 1; j < nets.length - 1; j++) {
        for (int k = j + 1; k < nets.length; k++) {
          if (checkForHeteroCondensed(mzTol, mods, nets[i], nets[j], nets[k])) {
            counter++;
          }
        }
      }
    }
    return counter;
  }

  /**
   * Checks for any modification of the type X+Y --> XY -H2O (condensation)
   *
   * @param mzTol
   * @param mods
   * @param a
   * @param b
   * @param c
   * @return
   */
  public static boolean checkForHeteroCondensed(MZTolerance mzTol, IonModification[] mods,
      IonNetwork a, IonNetwork b, IonNetwork c) {
    // ensure c has the highest mass and is the multimer
    if (b.getNeutralMass() > c.getNeutralMass()) {
      IonNetwork tmp = c;
      c = b;
      b = tmp;
    }
    if (a.getNeutralMass() > c.getNeutralMass()) {
      IonNetwork tmp = a;
      a = c;
      c = tmp;
    }

    IonModification[] mod =
        checkForCondensedModifications(mzTol, mods, a.getNeutralMass(), b.getNeutralMass());
    if (mod != null) {
      IonNetworkHeteroCondensedRelation rel = new IonNetworkHeteroCondensedRelation(a, b, c);
      a.addRelation(c, rel);
      b.addRelation(c, rel);
      c.addRelation(a, rel);
      return true;
    }
    return false;
  }

  public static IonModification[] checkForHeteroCondensed(MZTolerance mzTol, IonModification[] mods,
      double massA, double massB, double massC) {
    // ensure c.mass is heighest
    if (massB > massC) {
      double tmp = massC;
      massC = massB;
      massB = tmp;
    }
    if (massA > massC) {
      double tmp = massC;
      massC = massA;
      massA = tmp;
    }

    // skip if mass a and b are the same
    // to limit the number of isomers grouped to condensed structures
    // massA and massB are going to be represented by a condensed homo-structure
    if (mzTol.checkWithinTolerance(massA, massB)) {
      return null;
    }

    IonModification water = IonModification.H2O;
    // condense a and subtract water
    double diff = Math.abs(massC - (massA + massB - water.getAbsMass()));
    // check -H2O -> diff = 0
    if (mzTol.checkWithinTolerance(diff, 0d)) {
      return new IonModification[]{water};
    }

    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return getStatus().equals(TaskStatus.FINISHED) ? 1 : stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Identification of relationships between ion identity networks in " + featureList
        .getName()
           + " ";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      LOG.info("Starting to search for relations (modifications) between ion identity networks ");

      // get all ion identity networks
      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(featureList, true);

      // filter out ? networks with unknown adducttypes
      nets = Arrays.stream(nets).filter(net -> !net.isUndefined()).toArray(IonNetwork[]::new);

      if (nets.length == 0) {
        setErrorMessage("No ion identity networks found. Run ion networking");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // clear all
      Arrays.stream(nets).forEach(IonNetwork::clearRelation);

      // check for modifications
      int counter = checkForModifications(mzTol, mods, nets);
      LOG.info("Found " + counter + " modifications");

      // check for condensed formulas
      // mass*2 - H2O and - modifications
      if (searchCondensed) {
        int counter2 = checkForCondensedModifications(mzTol, mods, nets);
        LOG.info("Found " + counter2 + " condensed molecules");
      }

      if (searchHeteroCondensed) {
        int counter2 = checkForHeteroCondensed(mzTol, mods, nets);
        LOG.info("Found " + counter2
                 + " condensed molecules (hetero - two different neutral molecules)");
      }

      // show all as identity
      showAllIdentities(nets);

      setStatus(TaskStatus.FINISHED);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MZmineRuntimeException(t);
    }
  }

  private void showAllIdentities(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      // TODO add? show?
//      net.addRelationsIdentityToRows();
    }
  }
}
