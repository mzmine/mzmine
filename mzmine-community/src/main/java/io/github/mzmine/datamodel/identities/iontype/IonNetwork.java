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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An annotation network full of ions that point to the same neutral molecule (neutral mass).
 * <p>
 * A network is only reachable through the {@link IonIdentity#getNetwork()} back reference of the
 * rows it contains. {@link BuildingIonNetwork} is the mutable form used while networks are
 * assembled and merged, {@link SimpleIonNetwork} is the immutable result that is attached to the
 * rows and saved to the project.
 * <p>
 * Average RT and summed height are computed on demand from the current nodes. The neutral mass is
 * cached by both implementations because it is queried far more often, and refreshed whenever the
 * nodes change.
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public sealed interface IonNetwork permits BuildingIonNetwork, SimpleIonNetwork {

  /**
   * Network ID, unique within a feature list. -1 while the network is still being built.
   */
  int getID();

  /**
   * A network with the given ID. All ion identities of this network are re-pointed to the returned
   * instance, so the rows always reference the renumbered network afterwards.
   *
   * @param id the new network ID
   * @return the renumbered network, may be a new instance
   */
  @NotNull IonNetwork withID(int id);

  /**
   * @return unmodifiable list of the (row, ion) pairs of this network
   */
  @NotNull List<IonNetworkNode> getNodes();

  /**
   * Possible formulas for the neutral molecule described by this network, best first.
   *
   * @return unmodifiable list of formulas
   */
  @NotNull List<ResultFormula> getMolFormulas();

  /**
   * Append formulas, skipping those already present. The first formula should be the best.
   */
  void addMolFormulas(@NotNull List<ResultFormula> formulas);

  /**
   * Move a formula to the front of the list, marking it as the best one.
   */
  void setBestMolFormula(@NotNull ResultFormula formula);

  /**
   * Best molecular formula (first in list)
   *
   * @return null if no formula was assigned to this network
   */
  default @Nullable ResultFormula getBestMolFormula() {
    final List<ResultFormula> formulas = getMolFormulas();
    return formulas.isEmpty() ? null : formulas.getFirst();
  }

  default int size() {
    return getNodes().size();
  }

  /**
   * @return a list copy of the rows
   */
  default @NotNull List<FeatureListRow> getRows() {
    final List<IonNetworkNode> nodes = getNodes();
    final List<FeatureListRow> rows = new ArrayList<>(nodes.size());
    for (final IonNetworkNode node : nodes) {
      rows.add(node.row());
    }
    return rows;
  }

  default @NotNull Stream<FeatureListRow> streamRows() {
    return getNodes().stream().map(IonNetworkNode::row);
  }

  default @NotNull Stream<IonIdentity> streamIons() {
    return getNodes().stream().map(IonNetworkNode::ion);
  }

  /**
   * The ion types are undefined M+?
   */
  default boolean isUndefined() {
    return streamIons().map(IonIdentity::getIonType).anyMatch(IonType::isUndefinedAdduct);
  }

  default boolean containsKey(@Nullable FeatureListRow row) {
    return get(row) != null;
  }

  /**
   * The ion identity that this network assigns to a row.
   *
   * @return null if the row is not part of this network
   */
  default @Nullable IonIdentity get(@Nullable FeatureListRow row) {
    if (row == null) {
      return null;
    }
    for (final IonNetworkNode node : getNodes()) {
      if (row.equals(node.row())) {
        return node.ion();
      }
    }
    return null;
  }

  default void forEach(@NotNull BiConsumer<FeatureListRow, IonIdentity> consumer) {
    for (final IonNetworkNode node : getNodes()) {
      consumer.accept(node.row(), node.ion());
    }
  }

  /**
   * Point the ion identities of all rows back to this network.
   */
  @NotNull
  default IonNetwork setNetworkToAllRows() {
    streamIons().forEach(ion -> ion.setNetwork(this));
    return this;
  }

  /**
   * Neutral mass of the center molecule which is described by all members of this network.
   * <p>
   * Implementations cache this value because it is queried often, e.g. by every ion library search
   * and by sorting. The cache is refreshed whenever the nodes change.
   */
  double getNeutralMass();

  /**
   * Average of the neutral masses that the given nodes point to. Used by the implementations to
   * (re)fill their neutral mass cache.
   *
   * @param nodes the (row, ion) pairs of a network
   * @return 0 for an empty network or if no row has an m/z
   */
  static double calcNeutralMass(@NotNull final List<IonNetworkNode> nodes) {
    double mass = 0;
    int counted = 0;
    for (final IonNetworkNode node : nodes) {
      // rows without m/z cannot contribute a neutral mass - average over the remaining ones
      // instead of failing, because this is computed eagerly while networks are built
      final Double mz = node.row().getAverageMZ();
      if (mz == null) {
        continue;
      }
      mass += node.ion().getIonType().getMass(mz);
      counted++;
    }
    return counted == 0 ? 0 : mass / counted;
  }

  /**
   * Average retention time of all rows of this network.
   */
  default double getAvgRT() {
    final List<IonNetworkNode> nodes = getNodes();
    if (nodes.isEmpty()) {
      return 0;
    }
    double rt = 0;
    int n = 0;
    for (final IonNetworkNode node : nodes) {
      final Float averageRT = node.row().getAverageRT();
      if (averageRT != null) {
        rt += averageRT;
        n++;
      }
    }
    return rt / n;
  }

  /**
   * Summed height of the most intense feature of each row.
   */
  default double getHeightSum() {
    double heightSum = 0;
    for (final IonNetworkNode node : getNodes()) {
      final Float height = node.row().getMaxHeight();
      heightSum += height == null || Float.isNaN(height) ? 0 : height;
    }
    return heightSum;
  }

  /**
   * Remove the ion identities of this network from all its rows. The network is unreachable
   * afterwards because rows are the only owners of ion identities.
   */
  default void delete() {
    for (final IonNetworkNode node : getNodes()) {
      node.row().removeIonIdentity(node.ion());
      node.ion().setNetwork(null);
    }
  }

  /// @return the lowest row ID or -1 if empty network
  int getLowestID();
}
