/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.ms2.MSMSIdentityList;
import io.github.mzmine.datamodel.identities.ms2.MSMSIonRelationIdentity;
import io.github.mzmine.datamodel.identities.ms2.MSMSIonRelationIdentity.Relation;
import io.github.mzmine.datamodel.identities.ms2.MSMSMultimerIdentity;
import io.github.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IonIdentity {

  private NumberFormat netIDForm = new DecimalFormat("#000");

  private IonType ionType;
  // identifier like [M+H]+
  private String adduct;
  // partner rowIDs
  private ConcurrentHashMap<FeatureListRow, IonIdentity> partner = new ConcurrentHashMap<>();
  // network id (number)
  private IonNetwork network;

  /**
   * List of MSMS identities. e.g., multimers/monomers that were found in MS/MS data
   */
  private MSMSIdentityList msmsIdent;

  // possible formulas for this neutral mass
  private List<ResultFormula> molFormulas;

  // mark as beeing deleted
  private boolean isDeleted;

  /**
   * Create the identity.
   *
   * @param ionType type of adduct.
   */
  public IonIdentity(IonType ionType) {
    super();
    this.ionType = ionType;
    this.adduct = ionType.toString(false);
  }


  /**
   * Adds new identities or just adds the rows to identities as links
   *
   * @param row1 row to add the identity to
   * @param row2 identified by this row
   */
  public static IonIdentity[] addAdductIdentityToRow(FeatureListRow row1, IonType row1ID,
      FeatureListRow row2, IonType row2ID) {
    // already added?
    IonIdentity a = getAdductEqualIdentity(row1, row1ID);
    IonIdentity b = getAdductEqualIdentity(row2, row2ID);

    IonNetwork net = null;

    // create new
    if (a == null) {
      a = new IonIdentity(row1ID);
      row1.addIonIdentity(a, false);
    } else {
      net = a.getNetwork();
    }
    if (b == null) {
      b = new IonIdentity(row2ID);
      row2.addIonIdentity(b, false);
    } else {
      // if both were in networks
      if (net != null) {
        // combine networks
        IonNetwork netB = b.getNetwork();
        for (Entry<FeatureListRow, IonIdentity> e : netB.entrySet()) {
          net.put(e.getKey(), e.getValue());
        }
      } else {
        net = b.getNetwork();
      }
    }

    // no network so far
    if (net == null) {
      net = new IonNetwork(null, -1);
    }

    net.put(row1, a);
    net.put(row2, b);
    a.addPartnerRow(row2, b);
    b.addPartnerRow(row1, a);
    return new IonIdentity[]{a, b};
  }

  /**
   * Find equal identity that was already added
   *
   * @return equal identity or null
   */
  public static IonIdentity getAdductEqualIdentity(FeatureListRow row, IonType adduct) {
    if (!row.hasIonIdentity()) {
      return null;
    }
    // is old?
    for (IonIdentity a : row.getIonIdentities()) {
      // equals? add row2 to partners
      if (a.equalsAdduct(adduct)) {
        return a;
      }
    }
    return null;
  }

  /**
   * Get adduct type
   *
   * @return
   */
  public IonType getIonType() {
    return ionType;
  }

  public String getAdduct() {
    return adduct;
  }

  /**
   * Comma separated
   *
   * @return
   */
  public String getPartnerRows() {
    return getPartnerRows(",");
  }

  /**
   * @param delimiter
   * @return
   */
  public String getPartnerRows(String delimiter) {
    return partner.keySet().stream().map(FeatureListRow::getID).map(String::valueOf)
        .collect(Collectors.joining(delimiter));
  }

  public void addPartnerRow(FeatureListRow row, IonIdentity pid) {
    partner.put(row, pid);
  }

  public void resetLinks() {
    partner.clear();
  }

  public String getIDString() {
    StringBuilder b = new StringBuilder();
    // if (getNetID() != -1) {
    // b.append("Net");
    // b.append(getNetIDString());
    // b.append(" ");
    // }
    b.append(adduct);

    // xmer and multimer
    if (getMSMSMultimerCount() > 0 && (getIonType().getModCount() > 0 && getMSMSModVerify() > 0)) {
      b.append(" (MS/MS:xmer, insource frag)");
    }
    // MSMS backed id for multimers
    else if (getMSMSMultimerCount() > 0) {
      b.append(" (MS/MS:xmer)");
    }
    // MSMS backed id for insource frag
    else if (getIonType().getModCount() > 0 && getMSMSModVerify() > 0) {
      b.append(" (MS/MS:insource frag)");
    }

    b.append(" identified by ID=");
    b.append(getPartnerRows());

    return b.toString();
  }

  @Override
  public String toString() {
    return getIDString();
  }

  public boolean equalsAdduct(IonType ion) {
    return ion.equals(this.ionType);
  }

  public int[] getPartnerRowsID() {
    if (partner.isEmpty()) {
      return new int[0];
    }

    return partner.keySet().stream().mapToInt(FeatureListRow::getID).toArray();
  }

  public ConcurrentHashMap<FeatureListRow, IonIdentity> getPartner() {
    return partner;
  }

  /**
   * Network number
   */
  public void setNetwork(IonNetwork net) {
    network = net;
  }

  /**
   * Network number
   *
   * @return
   */
  public int getNetID() {
    return network == null ? -1 : network.getID();
  }

  public String getNetIDString() {
    return netIDForm.format(getNetID());
  }


  /**
   * Checks whether partner ids contain a certain id
   *
   * @param id
   * @return
   */
  public boolean hasPartnerID(int id) {
    return Arrays.stream(getPartnerRowsID()).anyMatch(pid -> pid == id);
  }

  public void setMSMSIdentities(MSMSIdentityList msmsIdent) {
    this.msmsIdent = msmsIdent;
  }

  public void addMSMSIdentity(AbstractMSMSIdentity ident) {
    if (this.msmsIdent == null) {
      msmsIdent = new MSMSIdentityList();
    }
    msmsIdent.add(ident);
  }

  public MSMSIdentityList getMSMSIdentities() {
    return msmsIdent;
  }

  /**
   * Count of signals that verify this multimer identity
   *
   * @return
   */
  public int getMSMSMultimerCount() {
    if (msmsIdent == null || msmsIdent.isEmpty()) {
      return 0;
    }

    return (int) msmsIdent.stream().filter(id -> id instanceof MSMSMultimerIdentity).count();
  }

  public int getMSMSModVerify() {
    if (msmsIdent == null || msmsIdent.isEmpty()) {
      return 0;
    }

    return (int) msmsIdent.stream().filter(id -> id instanceof MSMSIonRelationIdentity
                                                 && ((MSMSIonRelationIdentity) id).getRelation()
                                                     .equals(Relation.NEUTRAL_LOSS)).count();
  }

  public IonNetwork getNetwork() {
    return network;
  }

  /**
   * deletes from network
   */
  public void delete(FeatureListRow row) {
    if (isDeleted()) {
      return;
    }
    setDeleted(true);
    if (network != null) {
      network.remove(row);
    }
    row.removeIonIdentity(this);
    // remove from partners
    partner.entrySet().stream().forEach(e -> e.getValue().delete(e.getKey()));
  }


  public void setDeleted(boolean state) {
    isDeleted = state;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  /**
   * Score is the network size plus MSMS verifiers
   *
   * @return
   */
  public int getScore() {
    if (network == null) {
      return partner.size();
    }
    return network.size() + (getMSMSMultimerCount() > 0 ? 1 : 0) + (getMSMSModVerify() > 0 ? 1 : 0);
  }

  public List<ResultFormula> getMolFormulas() {
    return molFormulas;
  }

  /**
   * The first formula should be the best
   *
   * @param molFormulas
   */
  public void setMolFormulas(List<ResultFormula> molFormulas) {
    this.molFormulas = molFormulas;
  }

  public void addMolFormula(ResultFormula formula) {
    addMolFormula(formula, false);
  }

  public void addMolFormula(ResultFormula formula, boolean asBest) {
    if (molFormulas == null) {
      molFormulas = new ArrayList<>();
    }

    if (!molFormulas.isEmpty()) {
      molFormulas.remove(formula);
    }

    if (asBest) {
      this.molFormulas.add(0, formula);
    } else {
      this.molFormulas.add(formula);
    }
  }

  /**
   * Best molecular formula (first in list)
   *
   * @return
   */
  public ResultFormula getBestMolFormula() {
    return molFormulas == null || molFormulas.isEmpty() ? null : molFormulas.get(0);
  }


  public void setBestMolFormula(ResultFormula formula) {
    addMolFormula(formula, true);
  }

  public void removeMolFormula(ResultFormula formula) {
    if (molFormulas != null && !molFormulas.isEmpty()) {
      molFormulas.remove(formula);
    }
  }

  /**
   * Likyhood to be true. the higher the better. Used to compare. MSMS multimer and modification
   * verification is used.
   *
   * @return
   */
  public int getLikelyhood() {
    // M+?
    if (ionType.isUndefinedAdductParent()) {
      return 0;
    }
    // M-H2O+?
    else if (ionType.isUndefinedAdduct()) {
      return 1;
    } else {
      int score = getMSMSMultimerCount() > 0 ? 3 : 0;
      score += getMSMSModVerify() > 0 ? 1 : 0;
      if (getNetwork() != null) {
        score += getNetwork().size() - 1;
      } else {
        score += partner.size();
      }

      if (ionType.getMolecules() == 1) {
        score += 0.5;
      }
      return score;
    }
  }
}
