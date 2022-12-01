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

package io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.checkmsms;


import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.ms2.MSMSIonRelationIdentity;
import io.github.mzmine.datamodel.identities.ms2.interf.MsMsIdentity;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.MSMSLogic;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class IonNetworkMSMSCheckTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(IonNetworkMSMSCheckTask.class.getName());

  private int finishedRows;
  private final int totalRows;
  private final ModularFeatureList featureList;
  private final MZTolerance mzTolerance;

  private double minHeight;
  private boolean checkMultimers;
  private boolean checkNeutralLosses;
  private NeutralLossCheck neutralLossCheck;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   */
  public IonNetworkMSMSCheckTask(final MZmineProject project, final ParameterSet parameterSet,
      final ModularFeatureList featureLists, @NotNull Instant moduleCallDate) {
    super(featureLists.getMemoryMapStorage(), moduleCallDate);
    this.featureList = featureLists;

    finishedRows = 0;
    totalRows = 0;

    // tolerances
    mzTolerance = parameterSet.getParameter(IonNetworkMSMSCheckParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(IonNetworkMSMSCheckParameters.MIN_HEIGHT).getValue();
    checkMultimers =
        parameterSet.getParameter(IonNetworkMSMSCheckParameters.CHECK_MULTIMERS).getValue();
    checkNeutralLosses =
        parameterSet.getParameter(IonNetworkMSMSCheckParameters.CHECK_NEUTRALLOSSES).getValue();
    neutralLossCheck = parameterSet.getParameter(IonNetworkMSMSCheckParameters.CHECK_NEUTRALLOSSES)
        .getEmbeddedParameter().getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 1 : finishedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "MSMS-check IIN multimers and in-source fragments in " + featureList.getName() + " ";
  }

  @Override
  public void run() {
    doCheck();
    finishedRows = totalRows;
    setStatus(TaskStatus.FINISHED);
  }

  public void doCheck() {
    doCheck(true, featureList, mzTolerance, minHeight, checkMultimers, checkNeutralLosses,
        neutralLossCheck);
  }

  public static void doCheck(boolean parallel, ModularFeatureList pkl,
      MZTolerance mzTolerance, double minHeight, boolean checkMultimers, boolean checkNeutralLosses,
      NeutralLossCheck neutralLossCheck) {
    // do parallel or not
    pkl.stream(parallel).forEach(
        row -> doCheck(pkl, row, mzTolerance, minHeight, checkMultimers, checkNeutralLosses,
            neutralLossCheck));
  }

  public static void doCheck(ModularFeatureList pkl, FeatureListRow row,
      MZTolerance mzTolerance, double minHeight, boolean checkMultimers, boolean checkNeutralLosses,
      NeutralLossCheck neutralLossCheck) {

    // has annotations?
    List<IonIdentity> ident = row.getIonIdentities();
    if (ident == null || ident.isEmpty()) {
      return;
    }

    // has MS/MS
    try {
      // check for 2M+X-->1M+X in MS2 of this row
      if (checkMultimers) {
        checkMultimers(row, ident, mzTolerance, minHeight);
      }

      // check for neutral loss in all rows of this IonNetwork
      if (checkNeutralLosses) {
        checkNeutralLosses(pkl, neutralLossCheck, row, ident, mzTolerance, minHeight);
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  /**
   * Check for neutral loss in MSMS of all other rows in annotation or correlation group
   *
   * @param neutralLossCheck
   * @param row
   * @param ident
   * @param mzTolerance
   * @param minHeight
   */
  public static void checkNeutralLosses(ModularFeatureList pkl, NeutralLossCheck neutralLossCheck,
      FeatureListRow row, List<IonIdentity> ident, MZTolerance mzTolerance,
      double minHeight) {
    if (ident == null || ident.isEmpty()) {
      return;
    }

    int c = 0;
    for (IonIdentity ad : ident) {
      // do not test the unmodified
      if (ad.getIonType().getModCount() <= 0) {
        continue;
      }

      IonNetwork net = ad.getNetwork();
      IonType mod = ad.getIonType();

      // for all rows in network
      FeatureListRow[] rows = null;
      if (net != null) {
        rows = net.keySet().toArray(new FeatureListRow[0]);
      } else {
        rows = ad.getPartner().keySet().toArray(new FeatureListRow[0]);
      }

      // check group for correlation
      RowGroup group = row.getGroup();

      if (rows != null) {
        for (FeatureListRow parent : rows) {
          if (parent == null || parent.getID().equals(row.getID())) {
            continue;
          }

          // only correlated rows in this group
          if (group == null || group.isCorrelated(row, parent)) {
            // has MS/MS
            Scan msmsScan = parent.getMostIntenseFragmentScan();
            if (msmsScan == null) {
              continue;
            }
            MassList masses = msmsScan.getMassList();
            if (masses == null) {
              continue;
            }

            DataPoint[] dps = masses.getDataPoints();
            Feature f = parent.getFeature(msmsScan.getDataFile());
            double precursorMZ = f.getMZ();
            boolean result = checkParentForNeutralLoss(neutralLossCheck, dps, ad, mod, mzTolerance,
                minHeight, precursorMZ);
            if (result) {
              c++;
            }
          }
        }
      }
    }

    // sort and get best
    IonNetworkLogic.sortIonIdentities(row, true);
    IonIdentity best = row.getBestIonIdentity();
    final int counter = c;
    if (c > 0) {
      LOG.info(() -> MessageFormat.format(
          "Found {0} MS/MS fragments for neutral loss identifiers of rowID=[1} m/z={2} RT={3} best:{4}",
          counter, row.getID(), row.getAverageMZ(), row.getAverageRT(),
          best == null ? "" : best.toString()));
    }
  }

  /**
   * @param neutralLossCheck
   * @param mod              the modification to search for
   * @param mzTolerance
   * @param minHeight
   * @param precursorMZ
   */
  public static boolean checkParentForNeutralLoss(NeutralLossCheck neutralLossCheck,
      DataPoint[] dps, IonIdentity identity, IonType mod, MZTolerance mzTolerance, double minHeight,
      double precursorMZ) {
    boolean result = false;
    // loss for precursor mz
    DataPoint loss = MSMSLogic.findDPAt(dps, precursorMZ, mzTolerance, minHeight);
    if (loss != null) {
      MSMSIonRelationIdentity relation =
          new MSMSIonRelationIdentity(mzTolerance, loss, mod, precursorMZ);
      identity.addMsMsIdentity(relation);
      result = true;
    }

    if (neutralLossCheck.equals(NeutralLossCheck.ANY_SIGNAL)) {
      List<MsMsIdentity> msmsIdent = MSMSLogic.checkNeutralLoss(dps, mod, mzTolerance, minHeight);

      // found?
      for (MsMsIdentity id : msmsIdent) {
        identity.addMsMsIdentity(id);
        result = true;
      }
    }
    return result;
  }

  /**
   * Check all best fragment scans of all features for precursor - M
   *
   * @param row
   * @param ident
   * @param mzTolerance
   * @param minHeight
   */
  public static void checkMultimers(FeatureListRow row, List<IonIdentity> ident,
      MZTolerance mzTolerance, double minHeight) {
    for (Feature f : row.getFeatures()) {
      Scan msmsScan = f.getMostIntenseFragmentScan();
      if (msmsScan != null) {
        for (int i = 0; i < ident.size(); i++) {
          IonIdentity adduct = ident.get(i);
          boolean isMultimer = checkMultimers(row, adduct, msmsScan, ident, mzTolerance,
              minHeight, f.getMZ());
          if (isMultimer) {
            break;
          }
        }
      }
    }
  }

  public static boolean checkMultimers(FeatureListRow row, IonIdentity adduct,
      Scan msmsScan, List<IonIdentity> ident, MZTolerance mzTolerance, double minHeight,
      double precursorMZ) {
    Feature f = row.getFeature(msmsScan.getDataFile());
    // only for M>1
    if (adduct.getIonType().getMolecules() > 1) {
      List<MsMsIdentity> msmsIdent = MSMSLogic.checkMultiMolCluster(msmsScan, precursorMZ,
          adduct.getIonType(), mzTolerance, minHeight);

      // found?
      if (msmsIdent != null && msmsIdent.size() > 0) {
        // add all
        for (MsMsIdentity msms : msmsIdent) {
          adduct.addMsMsIdentity(msms);
        }
        return true;
      }
    }
    return false;
  }
}
