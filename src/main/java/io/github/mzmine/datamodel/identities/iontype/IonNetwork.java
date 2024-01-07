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
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkRelation;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * An annotation network full of ions that point to the same neutral molecule (neutral mass)
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class IonNetwork extends HashMap<FeatureListRow, IonIdentity>
    implements Comparable<IonNetwork> {

  // possible formulas for this neutral mass
  private final ObservableList<ResultFormula> molFormulas = FXCollections.observableArrayList();
  // MZtolerance on MS1 to generate this network
  private MZTolerance mzTolerance;
  // network id
  private int id;
  // neutral mass of central molecule which is described by all members of this network
  private Double neutralMass = null;
  // maximum absolute deviation from neutral mass average
  private Double maxDev = null;
  // average retention time of network
  private double avgRT;
  // summed height
  private double heightSum = 0;
  // can be used to stream all networks only once
  // lowest row id
  private int lowestID = -1;
  // relationship to other IonNetworks (neutral molecules)
  // marks as modification of:
  private Map<IonNetwork, IonNetworkRelation> relations;

  public IonNetwork(MZTolerance mzTolerance, int id) {
    super();
    this.mzTolerance = mzTolerance;
    this.id = id;
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  /**
   * The ion types are undefined M+?
   *
   * @return
   */
  public boolean isUndefined() {
    return values().stream().map(IonIdentity::getIonType).anyMatch(IonType::isUndefinedAdduct);
  }

  /**
   * Network ID
   *
   * @return
   */
  public int getID() {
    return id;
  }

  public void setID(int i) {
    id = i;
    setNetworkToAllRows();
  }

  @NotNull
  public List<ResultFormula> getMolFormulas() {
    return molFormulas;
  }

  public Map<IonNetwork, IonNetworkRelation> getRelations() {
    return Objects.requireNonNullElse(relations, Map.of());
  }

  /**
   * Add a relation to another ion network. This relation could be a modification
   *
   * @param net
   * @param rel
   */
  public void addRelation(IonNetwork net, IonNetworkRelation rel) {
    if (relations == null) {
      relations = new TreeMap<>();
    }
    relations.put(net, rel);
  }

  /**
   * Remove a relation to another ion network. This relation could be a modification
   */
  public void removeRelation(IonNetwork net) {
    if (relations == null) {
      return;
    }
    relations.remove(net);
  }

  /**
   * Clear
   */
  public void clearRelation() {
    relations = null;
  }

  /**
   * Create relations identity
   */
  public String concatRelationshipsToString() {
    String name = "";
    if (relations != null) {
      name = relations.values().stream().filter(Objects::nonNull).map(rel -> rel.getName(this))
          .collect(Collectors.joining(", "));
    }

    return name;
  }

  public void clearMolFormulas() {
    molFormulas.clear();
  }

  /**
   * The first formula should be the best
   *
   * @param molFormulas
   */
  public void addMolFormulas(List<ResultFormula> molFormulas) {
    this.molFormulas.removeAll(molFormulas);
    this.molFormulas.addAll(molFormulas);
  }

  /**
   * The first formula should be the best
   *
   * @param molFormulas
   */
  public void addMolFormulas(ResultFormula... molFormulas) {
    this.molFormulas.removeAll(molFormulas);
    this.molFormulas.addAll(molFormulas);
  }

  public void addMolFormula(ResultFormula formula) {
    addMolFormula(formula, false);
  }

  public void addMolFormula(ResultFormula formula, boolean asBest) {
    if (!molFormulas.isEmpty()) {
      molFormulas.remove(formula);
    }

    if (asBest) {
      this.molFormulas.add(0, formula);
    } else {
      this.molFormulas.add(formula);
    }
  }

  public void removeMolFormula(ResultFormula formula) {
    if (molFormulas != null && !molFormulas.isEmpty()) {
      molFormulas.remove(formula);
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

  /**
   * Neutral mass of center molecule which is described by all members of this network
   */
  public double getNeutralMass() {
    return neutralMass == null ? calcNeutralMass() : neutralMass;
  }

  @Override
  public IonIdentity put(FeatureListRow key, IonIdentity value) {
    IonIdentity e = super.put(key, value);
    if (key.getID() < lowestID || lowestID == -1) {
      lowestID = key.getID();
    }

    value.setNetwork(this);

    fireChanged();
    return e;
  }

  @Override
  public IonIdentity remove(Object key) {
    IonIdentity e = super.remove(key);
    if (e != null && key instanceof FeatureListRow && ((FeatureListRow) key).getID() <= lowestID) {
      recalcMinID();
    }

    if (e != null) {
      e.setNetwork(null);
      fireChanged();
    }
    return e;
  }

  /**
   * Finds the minimum row id
   */
  public int recalcMinID() {
    lowestID = keySet().stream().mapToInt(FeatureListRow::getID).min().orElse(-1);
    return lowestID;
  }

  @Override
  public void clear() {
    super.clear();
    lowestID = -1;
    fireChanged();
  }

  @Override
  public IonIdentity replace(FeatureListRow key, IonIdentity value) {
    IonIdentity e = super.replace(key, value);
    if (key.getID() < lowestID || lowestID == -1) {
      lowestID = key.getID();
    }

    value.setNetwork(this);
    fireChanged();
    return e;
  }

  public void fireChanged() {
    resetNeutralMass();
    resetMaxDev();
  }

  public void resetNeutralMass() {
    neutralMass = null;
  }

  /**
   * Maximum absolute deviation from central neutral mass
   */
  public void resetMaxDev() {
    maxDev = null;
  }

  /**
   * Calculates and sets the neutral mass average and average rt
   *
   * @return
   */
  public double calcNeutralMass() {
    neutralMass = null;
    if (size() == 0) {
      return 0;
    }

    double mass = 0;
    avgRT = 0;
    heightSum = 0;
    for (Entry<FeatureListRow, IonIdentity> e : entrySet()) {
      mass += e.getValue().getIonType().getMass(e.getKey().getAverageMZ());
      avgRT += e.getKey().getAverageRT();
      // sum of heighest peaks heights
      double height = e.getKey().getMaxDataPointIntensity();
      heightSum += Double.isNaN(height) ? 1 : height;
    }
    avgRT = avgRT / size();
    neutralMass = mass / size();
    return neutralMass;
  }

  public double getAvgRT() {
    if (neutralMass == null) {
      calcNeutralMass();
    }
    return avgRT;
  }

  public double getHeightSum() {
    if (neutralMass == null) {
      calcNeutralMass();
    }
    return heightSum;
  }

  /**
   * calculates the maximum deviation from the average mass
   *
   * @return
   */
  public double calcMaxDev() {
    maxDev = null;
    if (size() == 0) {
      return 0;
    }

    neutralMass = getNeutralMass();
    if (neutralMass == null || neutralMass == 0) {
      return 0;
    }

    double max = 0;
    for (Entry<FeatureListRow, IonIdentity> e : entrySet()) {
      double mass = getMass(e);
      max = Math.max(Math.abs(neutralMass - mass), max);
    }
    maxDev = max;
    return maxDev;
  }

  /**
   * Neutral mass of entry
   *
   * @param e
   * @return
   */
  public double getMass(Entry<FeatureListRow, IonIdentity> e) {
    return e.getValue().getIonType().getMass(e.getKey().getAverageMZ());
  }

  public double getMaxDev() {
    return maxDev == null ? calcMaxDev() : maxDev;
  }

  /**
   * All rows point to the same neutral mass
   *
   * @param mzTol
   * @return
   */
  public boolean checkAllWithinMZTol(MZTolerance mzTol) {
    double neutralMass = getNeutralMass();
    double maxDev = getMaxDev();
    return mzTol.checkWithinTolerance(neutralMass, neutralMass + maxDev);
  }

  public int[] getAllIDs() {
    return keySet().stream().mapToInt(e -> e.getID()).toArray();
  }

  public void setNetworkToAllRows() {
    values().stream().forEach(id -> id.setNetwork(this));
  }

  /**
   * Checks the calculated neutral mass of the ion annotation against the avg neutral mass
   *
   * @param row
   * @param pid
   * @return
   */
  public boolean checkForAnnotation(FeatureListRow row, IonType pid) {
    return mzTolerance.checkWithinTolerance(calcNeutralMass(), pid.getMass(row.getAverageMZ()));
  }

  /**
   * Checks for links and adds those as partner rows
   *
   * @param row
   * @param pid
   */
  public void addAllLinksTo(FeatureListRow row, IonIdentity pid) {
    double nmass = pid.getIonType().getMass(row.getAverageMZ());
    this.entrySet().stream().forEach(e -> {
      if (e.getKey().getID().equals(row.getID())) {
        double pmass = getMass(e);
        if (mzTolerance.checkWithinTolerance(pmass, nmass)) {
          // add to both
          pid.addPartnerRow(e.getKey(), e.getValue());
          e.getValue().addPartnerRow(row, pid);
        }
      }
    });
  }

  public void delete() {
    entrySet().stream().forEach(e -> {
      e.getKey().removeIonIdentity(e.getValue());
    });
    clear();
  }

  /**
   * row has smallest id?
   *
   * @param row
   * @return
   */
  public boolean hasSmallestID(FeatureListRow row) {
    return row.getID() == lowestID;
  }

  /**
   * Correlation group id (if existing) is always the one of the first entry
   *
   * @return correlation group id or -1
   */
  public int getCorrID() {
    if (isEmpty()) {
      return -1;
    }
    return keySet().iterator().next().getGroupID();
  }

  /**
   * Checks if all entries are in the same correlation group
   *
   * @return correlation group id or -1
   */
  public boolean allSameCorrGroup() {
    if (isEmpty()) {
      return true;
    }
    int cid = getCorrID();
    for (FeatureListRow r : keySet()) {
      if (r.getGroupID() != cid) {
        return false;
      }
    }
    return true;
  }

  public MZTolerance getMZTolerance() {
    return mzTolerance;
  }

  public void recalcConnections() {
    // Do not need to do this?
    // for (Entry<PeakListRow, ESIAdductIdentity> a : entrySet()) {
    // ESIAdductIdentity adduct = a.getValue();
    // if (adduct.getA().getAbsCharge() > 0)
    // adduct.resetLinks();
    // }

    // add all links
    for (Entry<FeatureListRow, IonIdentity> a : entrySet()) {
      IonIdentity adduct = a.getValue();
      if (adduct.getIonType().getAbsCharge() > 0) {
        addAllLinksTo(a.getKey(), adduct);
      }
    }
  }

  @Override
  public int compareTo(IonNetwork net) {
    // -1 if this is better
    return Integer.compare(net.size(), this.size());
  }

}
