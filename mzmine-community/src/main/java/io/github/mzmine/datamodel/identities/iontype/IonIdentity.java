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

package io.github.mzmine.datamodel.identities.iontype;

import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingTask;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.formula.prediction.FormulaPredictionIonNetworkModule;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.GnpsFbmnExportAndSubmitModule;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.NotNull;

/**
 * IonIdentities are connected to {@link IonNetwork}s and represent different ion species (M+H,
 * M+Na, 2M+H, ...) for the same molecule. Typically {@link CorrelateGroupingTask} is performed
 * before identifying ion identities. They can be used to predict molecular formulas in
 * {@link FormulaPredictionIonNetworkModule} and they are part of the Ion Idententity Molecular
 * Networking workflow on https://gnps.ucsd.edu/, which is accessible through
 * {@link GnpsFbmnExportAndSubmitModule}.
 */
public class IonIdentity implements Comparable<IonIdentity> {

  @NotNull
  private final List<ResultFormula> molFormulas;
  @NotNull
  private final IonType ionType;
  // network id (number)
  private IonNetwork network;
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
    molFormulas = FXCollections.observableArrayList();
  }

  /**
   * Get adduct type
   *
   * @return
   */
  @NotNull
  public IonType getIonType() {
    return ionType;
  }

  public String getName() {
    return toString();
  }

  @Override
  public String toString() {
    return ionType.toString();
  }

  public boolean equalsIonType(IonType ion) {
    return Objects.equals(ion, ionType);
  }

  /**
   * Network number
   *
   * @return -1 if not part of a network
   */
  public int getNetID() {
    return network == null ? -1 : network.getID();
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
   * Score is the network size plus MSMS verifiers
   *
   * @return
   */
  public int getScore() {
    if (network == null) {
      return 0;
    }
    return network.size();
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
  public Optional<ResultFormula> getBestMolFormula() {
    return molFormulas.isEmpty() ? Optional.empty() : Optional.of(molFormulas.get(0));
  }


  public void setBestMolFormula(ResultFormula formula) {
    addMolFormula(formula, true);
  }

  public void removeMolFormula(ResultFormula formula) {
    if (molFormulas != null && !molFormulas.isEmpty()) {
      molFormulas.remove(formula);
    }
  }

  @Override
  public int compareTo(@NotNull IonIdentity ion) {
    return toString().compareTo(ion.toString());
  }

  /**
   *
   * @return likelyhood of this ion ID being true, a score where higher is better
   */
  public int getLikelyhood() {
    if (network == null) {
      return -1;
    }
    return network.size();
  }
}
