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

import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The finished, immutable form of an {@link IonNetwork}. Its nodes are sorted by row ID and cannot
 * change anymore, which makes it safe to hand out and to write to the project. Use a
 * {@link BuildingIonNetwork} to add or remove rows.
 * <p>
 * Only the list of molecular formulas stays mutable: formulas are annotations of the neutral
 * molecule that are assigned long after the network membership is final.
 */
public final class SimpleIonNetwork implements IonNetwork {

  private static final Comparator<IonNetworkNode> BY_ROW_ID = Comparator.comparingInt(
      node -> node.row().getID());

  private final int id;
  private final @NotNull List<IonNetworkNode> nodes;
  private final @NotNull List<ResultFormula> molFormulas = new ArrayList<>();
  private final double neutralMass;

  /**
   * @param id       network ID, unique within a feature list
   * @param nodes    the (row, ion) pairs, copied and sorted by row ID
   * @param formulas possible formulas of the neutral molecule, best first
   */
  public SimpleIonNetwork(final int id, @NotNull final Collection<IonNetworkNode> nodes,
      @NotNull final Collection<ResultFormula> formulas) {
    this.id = id;
    final List<IonNetworkNode> sorted = new ArrayList<>(nodes);
    sorted.sort(BY_ROW_ID);
    this.nodes = Collections.unmodifiableList(sorted);
    this.neutralMass = IonNetwork.calcNeutralMass(this.nodes);
    this.molFormulas.addAll(formulas);
  }

  public SimpleIonNetwork(final int id, @NotNull final Collection<IonNetworkNode> nodes) {
    this(id, nodes, List.of());
  }

  @Override
  public int getID() {
    return id;
  }

  @Override
  public @NotNull IonNetwork withID(final int id) {
    if (id == this.id) {
      return this;
    }
    final SimpleIonNetwork renumbered = new SimpleIonNetwork(id, nodes, molFormulas);
    renumbered.setNetworkToAllRows();
    return renumbered;
  }

  @Override
  public @NotNull List<IonNetworkNode> getNodes() {
    return nodes;
  }

  @Override
  public double getNeutralMass() {
    return neutralMass;
  }

  @Override
  public int getLowestID() {
    if (nodes.isEmpty()) {
      return -1;
    }
    return nodes.getFirst().row().getID();
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
    return "IonNetwork %d with %d rows".formatted(id, nodes.size());
  }
}
