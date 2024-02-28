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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.relations;


import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
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
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Find relationships between {@link IonNetwork}s
 */
public class IonNetRelationsTask extends AbstractTask {

  // Logger
  private static final Logger logger = Logger.getLogger(IonNetRelationsTask.class.getName());
  private final ModularFeatureList featureList;
  private final AtomicDouble stageProgress = new AtomicDouble(0);
  private final IonModification[] mods;
  private final MZTolerance mzTol;
  private final boolean searchCondensed;
  private final boolean searchHeteroCondensed;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public IonNetRelationsTask(final ParameterSet parameterSet, final ModularFeatureList featureLists,
      @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);

    this.featureList = featureLists;
    mods = parameterSet.getParameter(IonNetRelationsParameters.adducts).getValue()[1];
    mzTol = parameterSet.getParameter(IonNetRelationsParameters.mzTol).getValue();
    // tolerances
    searchCondensed = parameterSet.getParameter(IonNetRelationsParameters.searchCondensedMultimer)
        .getValue();
    searchHeteroCondensed = parameterSet.getParameter(
        IonNetRelationsParameters.searchCondensedHeteroMultimer).getValue();
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

    IonModification mod = checkForModifications(mzTol, mods, a.getNeutralMass(),
        b.getNeutralMass());
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
      double neutralMassA, double neutralMassB) {
    // b > a
    for (IonModification mod : mods) {
      // e.g. -H2O ~ -18
      if (mzTol.checkWithinTolerance(neutralMassB, neutralMassA + mod.getAbsMass())) {
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

    // retention time of condensed should be higher
    if (b.getAvgRT() < a.getAvgRT()) {
      return false;
    }

    IonModification[] mod = checkForCondensedModifications(mzTol, mods, a.getNeutralMass(),
        b.getNeutralMass());
    // always at least water loss
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

    // condense a and subtract water
    double calcMassB = massA * 2 - IonModification.H2O.getAbsMass();
    // check -H2O -> diff = 0
    if (mzTol.checkWithinTolerance(massB, calcMassB)) {
      return new IonModification[]{IonModification.H2O};
    }

    for (IonModification mod : mods) {
      // e.g. +O ~ +16 or -O16
      if (mzTol.checkWithinTolerance(massB, calcMassB + mod.getMass())) {
        return new IonModification[]{IonModification.H2O, mod};
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
    // retention time of condensed should be higher
    if (c.getAvgRT() < a.getAvgRT() || c.getAvgRT() < b.getAvgRT()) {
      return false;
    }

    IonModification[] mod = checkForHeteroCondensed(mzTol, mods, a.getNeutralMass(),
        b.getNeutralMass(), c.getNeutralMass());
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
    double calcMassC = massA + massB - water.getAbsMass();
    // check -H2O -> calcMassC = 0
    if (mzTol.checkWithinTolerance(massC, calcMassC)) {
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
    return "Identification of relationships between ion identity networks in "
           + featureList.getName() + " ";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      logger.info(
          "Starting to search for relations (modifications) between ion identity networks ");

      // get all ion identity networks
      // filter out ? networks with unknown adducttypes
      IonNetwork[] nets = IonNetworkLogic.streamNetworks(featureList, true)
          .filter(net -> !net.isUndefined()).toArray(IonNetwork[]::new);

      if (nets.length == 0) {
        logger.warning("No ion identity networks found. Run ion networking");
        setStatus(TaskStatus.FINISHED);
        return;
      }

      // clear all
      for (IonNetwork net : nets) {
        net.clearRelation();
      }

      // check for modifications
      if (mods != null && mods.length > 0) {
        int counter = checkForModifications(mzTol, mods, nets);
        logger.info("Found " + counter + " modifications");
      }

      // check for condensed formulas
      // mass*2 - H2O and - modifications
      if (searchCondensed) {
        int counter2 = checkForCondensedModifications(mzTol, mods, nets);
        logger.info("Found " + counter2 + " condensed molecules");
      }

      if (searchHeteroCondensed) {
        int counter2 = checkForHeteroCondensed(mzTol, mods, nets);
        logger.info("Found " + counter2
                    + " condensed molecules (hetero - two different neutral molecules)");
      }

      // show all as identity
      showAllIdentities(nets);

      setStatus(TaskStatus.FINISHED);
    } catch (Exception t) {
      logger.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  private void showAllIdentities(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      // TODO add? show?
      //      net.addRelationsIdentityToRows();
    }
  }
}
