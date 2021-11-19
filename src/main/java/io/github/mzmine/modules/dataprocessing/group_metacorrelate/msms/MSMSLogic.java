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

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms;


import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.ms2.MSMSIonRelationIdentity;
import io.github.mzmine.datamodel.identities.ms2.MSMSMultimerIdentity;
import io.github.mzmine.datamodel.identities.ms2.interf.MsMsIdentity;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MSMSLogic {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MSMSLogic.class.getName());

  /**
   * Checks the MSMS scan for matches of x-mers to the x-mer precursorMZ
   *
   * @param scan
   * @param precursorMZ
   * @param adduct      only the basic information is taken (charge and deltaMass, molecules are
   *                    then added from 1-maxM)
   * @param mzTolerance
   * @return List of identities. The first is always the one for the precursor
   */
  public static List<MsMsIdentity> checkMultiMolCluster(Scan scan, double precursorMZ, IonType adduct,
      MZTolerance mzTolerance, double minHeight) {
    return checkMultiMolCluster(scan, precursorMZ, adduct, adduct.getMolecules(),
        mzTolerance, minHeight);
  }

  /**
   * Checks the MSMS scan for matches of x-mers to the x-mer precursorMZ
   *
   * @param scan
   * @param precursorMZ
   * @param adduct      only the basic information is taken (charge and deltaMass, molecules are
   *                    then added from 1-maxM)
   * @param maxM        maximum M
   * @param mzTolerance
   * @return List of identities. The first is always the one for the precursor
   */
  public static List<MsMsIdentity> checkMultiMolCluster(Scan scan, double precursorMZ, IonType adduct,
      int maxM, MZTolerance mzTolerance, double minHeight) {
    MassList masses = scan.getMassList();
    if (masses == null) {
      return null;
    }

    // generate all M adducts 3M+X -> 2M+X -> M+X
    List<IonType> list = new ArrayList<>();
    for (int i = 1; i <= maxM; i++) {
      IonType m = new IonType(i, adduct);
      list.add(m);
    }

    // result best with the highest number of identities
    List<MsMsIdentity> ident = null;
    List<MsMsIdentity> best = null;

    // datapoints of masslist
    DataPoint[] dps = masses.getDataPoints();

    // find precursor in MSMS or create dummy
    DataPoint precursorDP = findDPAt(dps, precursorMZ, mzTolerance);
    if (precursorDP == null) {
      precursorDP = new SimpleDataPoint(precursorMZ, 1);
    }

    // check each adduct againt all other
    for (int i = 1; i < list.size(); i++) {
      ident = new ArrayList<>();
      IonType b = list.get(i);
      double massb = b.getMass(precursorMZ);
      for (int k = 0; k < i; k++) {
        IonType a = list.get(k);

        // calc mz for neutral mass with this adduct type
        double mza = a.getMZ(massb);

        // check with precursor mz
        DataPoint dp = findDPAt(dps, mza, mzTolerance, minHeight);
        if (dp != null) {
          // id found
          // find out if there are already some identities
          MSMSMultimerIdentity ia = null;
          MSMSMultimerIdentity ib = null;
          for (MsMsIdentity o : ident) {
            MSMSMultimerIdentity old = (MSMSMultimerIdentity) o;
            if (old.getType().equals(a)) {
              ia = old;
            }
            if (old.getType().equals(b)) {
              ib = old;
            }
          }

          // create new if empty
          if (ib == null) {
            ib = new MSMSMultimerIdentity(mzTolerance, precursorDP, b);
            ident.add(ib);
          }
          if (ia == null) {
            ia = new MSMSMultimerIdentity(mzTolerance, dp, a);
            ident.add(ia);
          }

          // add this reference to both
          ia.addLink(ib);
          ib.addLink(ia);
        }
      }
      // highest number of identities
      if (!ident.isEmpty() && (best == null || best.size() < ident.size())) {
        best = ident;
      }
    }
    return best;
  }


  /**
   * Checks the MSMS scan for matches of x-mers to the x-mer precursorMZ
   *
   * @param adduct      only the basic information is taken (charge and deltaMass, molecules are
   *                    then added from 1-maxM)
   * @param mzTolerance
   * @return List of identities. The first is always the one for the precursor
   */
  public static List<MsMsIdentity> checkNeutralLoss(DataPoint[] dps, IonType adduct,
      MZTolerance mzTolerance, double minHeight) {
    if (dps == null || dps.length == 0) {
      return null;
    }

    // delta
    double dmz = adduct.getMassDifference();

    // result best with the highest number of identities
    List<MsMsIdentity> ident = new ArrayList<>();

    // check all data points
    for (DataPoint dp : dps) {
      double mz = dp.getMZ();

      // check with precursor mz
      DataPoint loss = findDPAt(dps, mz - dmz, mzTolerance, minHeight);
      if (loss != null) {
        // id found
        MSMSIonRelationIdentity relation =
            new MSMSIonRelationIdentity(mzTolerance, loss, adduct, dp);
        ident.add(relation);
      }
    }
    return ident;
  }

  /**
   * Heighest dp within mzTolerance
   *
   * @param dps
   * @param precursorMZ
   * @param mzTolerance
   * @return
   */
  public static DataPoint findDPAt(DataPoint[] dps, double precursorMZ, MZTolerance mzTolerance) {
    return findDPAt(dps, precursorMZ, mzTolerance, 0);
  }

  /**
   * Heighest dp within mzTolerance
   *
   * @param dps
   * @param precursorMZ
   * @param mzTolerance
   * @return
   */
  public static DataPoint findDPAt(DataPoint[] dps, double precursorMZ, MZTolerance mzTolerance,
      double minHeight) {
    DataPoint best = null;
    for (DataPoint dp : dps) {
      if (dp.getIntensity() >= minHeight
          && (best == null || dp.getIntensity() > best.getIntensity())
          && mzTolerance.checkWithinTolerance(dp.getMZ(), precursorMZ)) {
        best = dp;
      }
    }
    return best;
  }

}
