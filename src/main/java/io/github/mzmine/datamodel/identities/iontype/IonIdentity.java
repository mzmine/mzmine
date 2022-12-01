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

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.ms2.MSMSIonRelationIdentity;
import io.github.mzmine.datamodel.identities.ms2.MSMSIonRelationIdentity.Relation;
import io.github.mzmine.datamodel.identities.ms2.MSMSMultimerIdentity;
import io.github.mzmine.datamodel.identities.ms2.interf.MsMsIdentity;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingTask;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction.FormulaPredictionIonNetworkModule;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;

/**
 * IonIdentities are connected to {@link IonNetwork}s and represent different ion species (M+H,
 * M+Na, 2M+H, ...) for the same molecule. Typically {@link CorrelateGroupingTask} is performed
 * before identifying ion identities. They can be used to predict molecular formulas in {@link
 * FormulaPredictionIonNetworkModule} and they are part of the Ion Idententity Molecular Networking
 * workflow on https://gnps.ucsd.edu/, which is accessible through {@link
 * GnpsFbmnExportAndSubmitModule}.
 */
public class IonIdentity implements Comparable<IonIdentity> {

  private static final NumberFormat netIDForm = new DecimalFormat("#000");
  // partner rowIDs
  private final ConcurrentHashMap<FeatureListRow, IonIdentity> partner = new ConcurrentHashMap<>();
  // possible formulas for this neutral mass
  @NotNull
  private final List<ResultFormula> molFormulas;
  private final IonType ionType;
  // identifier like [M+H]+
  private final String adduct;
  // network id (number)
  private IonNetwork network;
  /**
   * List of MSMS identities. e.g., multimers/monomers that were found in MS/MS data
   */
  private List<MsMsIdentity> msmsIdent;
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
    molFormulas = FXCollections.observableArrayList();
  }


  /**
   * Adds new identities or just adds the rows to identities as links
   *
   * @param row1 row to add the identity to
   * @param row2 identified by this row
   */
  public static IonIdentity[] addAdductIdentityToRow(MZTolerance mzTolerance, FeatureListRow row1,
      IonType row1ID, FeatureListRow row2, IonType row2ID) {
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
      net = new IonNetwork(mzTolerance, -1);
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
  public String getPartnerRowsString() {
    return getPartnerRowsString(",");
  }

  /**
   * @param delimiter
   * @return
   */
  public String getPartnerRowsString(String delimiter) {
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
    if (getNetID() != -1) {
      b.append("Net");
      b.append(getNetIDString());
      b.append(" ");
    }
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
    return adduct;
  }

  public boolean equalsAdduct(IonType ion) {
    return ion.equals(this.ionType);
  }

  public Set<FeatureListRow> getPartnerRows() {
    return partner.keySet();
  }

  public ConcurrentHashMap<FeatureListRow, IonIdentity> getPartner() {
    return partner;
  }

  /**
   * Network number
   *
   * @return -1 if not part of a network
   */
  public int getNetID() {
    return network == null ? -1 : network.getID();
  }

  public String getNetIDString() {
    return netIDForm.format(getNetID());
  }

  /**
   * Checks whether partner ids contain a certain id
   */
  public boolean hasPartnerRow(FeatureListRow row) {
    return getPartnerRows().stream().anyMatch(r -> r == row);
  }

  public void addAllMsMsIdentities(List<MsMsIdentity> msmsIdent) {
    if (this.msmsIdent == null) {
      msmsIdent = new ArrayList<>();
    }
    this.msmsIdent.addAll(msmsIdent);
  }

  public void addMsMsIdentity(MsMsIdentity ident) {
    if (this.msmsIdent == null) {
      msmsIdent = new ArrayList<>();
    }
    msmsIdent.add(ident);
  }

  public List<MsMsIdentity> getMSMSIdentities() {
    return msmsIdent;
  }

  public void setMSMSIdentities(List<MsMsIdentity> msmsIdent) {
    this.msmsIdent = msmsIdent;
  }

  /**
   * Count of signals that verify this multimer identity
   *
   * @return
   */
  public int getMSMSMultimerCount() {
    if (msmsIdent == null || msmsIdent.isEmpty()) {
      return -1;
    }

    return (int) msmsIdent.stream().filter(id -> id instanceof MSMSMultimerIdentity).count();
  }

  public int getMSMSModVerify() {
    if (msmsIdent == null || msmsIdent.isEmpty()) {
      return 0;
    }

    return (int) msmsIdent.stream().filter(
        id -> id instanceof MSMSIonRelationIdentity && ((MSMSIonRelationIdentity) id).getRelation()
            .equals(Relation.NEUTRAL_LOSS)).count();
  }

  public IonNetwork getNetwork() {
    return network;
  }

  /**
   * Network number
   */
  public void setNetwork(IonNetwork net) {
    network = net;
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

  public boolean isDeleted() {
    return isDeleted;
  }

  public void setDeleted(boolean state) {
    isDeleted = state;
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

  @NotNull
  public List<ResultFormula> getMolFormulas() {
    return molFormulas == null ? List.of() : molFormulas;
  }

  public void clearMolFormulas() {
    molFormulas.clear();
  }

  /**
   * The first formula should be the best
   *
   * @param molFormulas
   */
  public synchronized void addMolFormulas(List<ResultFormula> molFormulas) {
    this.molFormulas.removeAll(molFormulas);
    this.molFormulas.addAll(molFormulas);
  }

  /**
   * The first formula should be the best
   *
   * @param molFormulas
   */
  public synchronized void addMolFormulas(ResultFormula... molFormulas) {
    this.molFormulas.removeAll(List.of(molFormulas));
    this.molFormulas.addAll(List.of(molFormulas));
  }

  public synchronized void addMolFormula(ResultFormula formula) {
    addMolFormula(formula, false);
  }

  public synchronized void addMolFormula(ResultFormula formula, boolean asBest) {
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

  @Override
  public int compareTo(@NotNull IonIdentity ion) {
    return toString().compareTo(ion.toString());
  }

}
