/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The mutable form of an {@link IonNetwork}, used while networks are assembled and merged. Call
 * {@link #setNetworkToAllRows()} once the members are final to obtain the immutable
 * {@link SimpleIonNetwork} that is attached to the rows and saved to the project.
 */
public final class BuildingIonNetwork implements IonNetwork {

  private final @NotNull List<IonNetworkNode> nodes = new ArrayList<>();
  private final @NotNull List<ResultFormula> molFormulas = new ArrayList<>();
  private int id;
  // kept in sync with the nodes so that repeated ion library searches do not recompute it
  private double neutralMass;

  public BuildingIonNetwork() {
    this(-1);
  }

  public BuildingIonNetwork(final int id) {
    this.id = id;
  }

  /**
   * Thaw a finished network so that rows can be added or removed again. The ion identities keep
   * pointing at the original network until {@link #toSimple()} re-points them.
   */
  public BuildingIonNetwork(@NotNull final IonNetwork network) {
    this.id = network.getID();
    nodes.addAll(network.getNodes());
    molFormulas.addAll(network.getMolFormulas());
    updateNeutralMass();
  }

  /**
   * Freeze into the immutable form and re-point the ion identities of all rows to it.
   */
  public @NotNull SimpleIonNetwork toSimple() {
    return toSimple(id);
  }

  /**
   * Freeze into the immutable form under the given ID and re-point the ion identities of all rows
   * to it.
   */
  public @NotNull SimpleIonNetwork toSimple(final int id) {
    return new SimpleIonNetwork(id, nodes, molFormulas);
  }

  @Override
  public @NotNull IonNetwork setNetworkToAllRows() {
    // never set a building network to rows
    final SimpleIonNetwork simple = toSimple();
    return simple.setNetworkToAllRows();
  }

  /**
   * Add a row with its ion identity, replacing any ion this network already assigned to that row.
   *
   * @return the added ion identity
   */
  public @NotNull IonIdentity put(@NotNull final FeatureListRow row,
      @NotNull final IonIdentity ion) {
    removeNode(row);
    nodes.add(new IonNetworkNode(row, ion));
    ion.setNetwork(this);
    updateNeutralMass();
    return ion;
  }

  public void remove(@NotNull final FeatureListRow row) {
    removeNode(row);
    updateNeutralMass();
  }

  private void removeNode(@NotNull final FeatureListRow row) {
    nodes.removeIf(node -> {
      if (node.row().equals(row)) {
        node.ion().setNetwork(null);
        return true;
      }
      return false;
    });
  }

  /**
   * Merge another network into this one, skipping nodes that are already present.
   */
  public void addAll(@NotNull final IonNetwork other) {
    // avoid duplicates
    nodes.removeAll(other.getNodes());
    nodes.addAll(other.getNodes());

    for (final IonNetworkNode node : other.getNodes()) {
      node.ion().setNetwork(this);
    }
    updateNeutralMass();
  }

  public void clear() {
    nodes.clear();
    updateNeutralMass();
  }

  /**
   * Refresh the cached neutral mass. Called after every change to the nodes so that
   * {@link #getNeutralMass()} never returns a stale value.
   */
  private void updateNeutralMass() {
    neutralMass = IonNetwork.calcNeutralMass(nodes);
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public @NotNull IonNetwork withID(final int id) {
    this.id = id;
    return this;
  }

  @Override
  public @NotNull List<IonNetworkNode> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  @Override
  public double getNeutralMass() {
    return neutralMass;
  }

  @Override
  public @NotNull List<ResultFormula> getMolFormulas() {
    return Collections.unmodifiableList(molFormulas);
  }

  @Override
  public void addMolFormulas(@NotNull final List<ResultFormula> formulas) {
    molFormulas.removeAll(formulas);
    molFormulas.addAll(formulas);
  }

  @Override
  public void setBestMolFormula(@NotNull final ResultFormula formula) {
    molFormulas.remove(formula);
    molFormulas.addFirst(formula);
  }

  @Override
  public String toString() {
    return "BuildingIonNetwork %d with %d rows".formatted(id, nodes.size());
  }
}
