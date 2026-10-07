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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.collections.CollectionUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonNetworkLogic {

  /**
   * Orders ion identities so that the most likely explanation comes first. This is a total order:
   * two ion identities only compare equal when their {@link IonType} is equal, so sorting is
   * reproducible and independent of the order in which the ion identities were added to a row.
   *
   * @param ranking the user defined ranking, read from {@link FeatureList#getPreferences()}
   */
  public static @NotNull Comparator<IonIdentity> bestFirstSorter(
      @NotNull final IonTypeRanking ranking) {
    return ((Comparator<IonIdentity>) (a, b) -> compareIonIdentitiesLikelyhood(ranking, a,
        b)).reversed();
  }

  /**
   * Compare for likelyhood comparison and sorting. The criteria are applied in this order:
   * undefined adduct, network size (score), the {@link IonTypeRanking#score(IonType)} which covers
   * the frequency of all ion parts as well as the multimer and charge penalties, and finally mass
   * and name to make the order total.
   *
   * @param ranking the user defined ion type ranking
   * @param a       ion a
   * @param b       ion b
   * @return same as comparable: -1 0 1 if the first argument is less, equal or better
   */
  public static int compareIonIdentitiesLikelyhood(@NotNull final IonTypeRanking ranking,
      final IonIdentity a, final IonIdentity b) {
    if (a == null && b == null) {
      return 0;
    } else if (a == null) {
      return -1;
    } else if (b == null) {
      return 1;
    }
    final IonType typeA = a.getIonType();
    final IonType typeB = b.getIonType();

    // M-H2O+? (one is? undefined
    final boolean undefinedA = typeA.isUndefinedAdduct();
    final boolean undefinedB = typeB.isUndefinedAdduct();
    if (undefinedA && !undefinedB) {
      return -1;
    } else if (!undefinedA && undefinedB) {
      return 1;
    }

    // network size, MSMS modification and multimer (2M) verification
    int result = Integer.compare(a.getScore(), b.getScore());
    if (result != 0) {
      return result;
    }

    // the ranking covers ion part frequency, in-source modifications and multimers
    result = Double.compare(ranking.score(typeA), ranking.score(typeB));
    if (result != 0) {
      return result;
    }

    // decision: the remaining criteria only make the order total so that equally likely ions are
    // always sorted the same way, independent of insertion order. Smaller mass difference first,
    // then alphabetically by name.
    result = Double.compare(typeB.absTotalMass(), typeA.absTotalMass());
    if (result != 0) {
      return result;
    }
    return typeB.name().compareTo(typeA.name());
  }


  /**
   * All annotation networks of all annotations of row
   *
   * @return all networks of row
   */
  public static IonNetwork[] getAllNetworks(FeatureListRow row) {
    if (!row.hasIonIdentity()) {
      return new IonNetwork[0];
    }
    return row.getIonIdentities().stream().map(IonIdentity::getNetwork).filter(Objects::nonNull)
        .distinct().toArray(IonNetwork[]::new);
  }

  /**
   * Sort all ion identities of a row by the likelyhood of being true. Uses the ranking defined in
   * the preferences of the row's feature list.
   *
   * @param row the row to sort
   * @return list of annotations or null
   */
  public static List<IonIdentity> sortIonIdentities(FeatureListRow row) {
    return sortIonIdentities(row, row.getFeatureList().getPreferences().getIonTypeRanking());
  }

  /**
   * Sort all ion identities of a row by the likelyhood of being true.
   *
   * @param row     the row to sort
   * @param ranking the user defined ion type ranking
   * @return list of annotations or null
   */
  public static List<IonIdentity> sortIonIdentities(FeatureListRow row,
      @NotNull final IonTypeRanking ranking) {
    List<IonIdentity> ident = row.getIonIdentities();
    if (ident == null || ident.isEmpty()) {
      return null;
    }

    // best is first
    final List<IonIdentity> sorted = ident.stream().sorted(bestFirstSorter(ranking)).toList();
    row.setIonIdentities(sorted);
    return ident;
  }

  /**
   * Sort all ion identities of all rows with the ranking defined in the feature list preferences
   *
   * @param pkl the feature list
   */
  public static void sortIonIdentities(FeatureList pkl) {
    final IonTypeRanking ranking = pkl.getPreferences().getIonTypeRanking();
    for (FeatureListRow r : pkl.getRows()) {
      sortIonIdentities(r, ranking);
    }
  }

  /**
   * Delete empty networks
   *
   */
  public static void removeEmptyNetworks(FeatureList peakList) {
    List<IonNetwork> list = streamNetworks(peakList, false).toList();
    for (IonNetwork n : list) {
      if (n.size() < 2) {
        n.delete();
      }
    }
  }

  /**
   * All annotation networks of the featurelist
   *
   * @return
   */
  public static IonNetwork[] getAllNetworks(FeatureList peakList, boolean onlyBest) {
    return streamNetworks(peakList, onlyBest).toArray(IonNetwork[]::new);
  }

  public static IonNetwork[] getAllNetworks(FeatureList peakList, @Nullable IonNetworkSorter sorter,
      boolean onlyBest) {
    return streamNetworks(peakList, sorter, onlyBest).toArray(IonNetwork[]::new);
  }

  public static IonNetwork[] getAllNetworks(List<FeatureListRow> rows, boolean onlyBest) {
    return streamNetworks(rows, onlyBest).toArray(IonNetwork[]::new);
  }

  public static IonNetwork[] getAllNetworks(List<FeatureListRow> rows,
      @Nullable IonNetworkSorter sorter, boolean onlyBest) {
    return streamNetworks(rows, sorter, onlyBest).toArray(IonNetwork[]::new);
  }

  /**
   * Stream all AnnotationNetworks of this peakList
   *
   * @return
   */
  public static Stream<IonNetwork> streamNetworks(FeatureList peakList, boolean onlyBest) {
    return IonNetworkLogic.streamNetworks(peakList, null, onlyBest);
  }

  public static Stream<IonNetwork> streamNetworks(List<FeatureListRow> rows, boolean onlyBest) {
    return IonNetworkLogic.streamNetworks(rows, null, onlyBest);
  }

  /**
   * Stream all networks
   *
   * @return
   */
  public static Stream<IonNetwork> streamNetworks(FeatureList peakList) {
    return IonNetworkLogic.streamNetworks(peakList, null, false);
  }

  /**
   * Stream all networks
   */
  public static Stream<IonNetwork> streamNetworks(List<FeatureListRow> rows) {
    return IonNetworkLogic.streamNetworks(rows, null, false);
  }

  /**
   * Stream all AnnotationNetworks of this peakList
   *
   * @param peakList
   * @param sorter
   * @return
   */
  public static Stream<IonNetwork> streamNetworks(FeatureList peakList,
      @Nullable IonNetworkSorter sorter, boolean onlyBest) {
    return streamNetworks(peakList.getRows(), sorter, onlyBest);
  }

  /**
   * Stream all AnnotationNetworks of this peakList
   *
   * @param rows
   * @param sorter
   * @param onlyBest
   * @return
   */
  public static Stream<IonNetwork> streamNetworks(List<FeatureListRow> rows,
      @Nullable IonNetworkSorter sorter, boolean onlyBest) {
    // ion networks are mutable, so streaming over them and calling distinct may create leaks
    // if the network is changed during the streaming
    // this is why we need to collect all distinct networks in a list and return this list
    return getAllNetworksList(rows, sorter, onlyBest).stream();
  }

  /**
   * Stream all AnnotationNetworks of this peakList
   *
   * @param rows
   * @param sorter
   * @param onlyBest needs to be the best ion identity for all ions in this network
   * @return
   */
  public static List<IonNetwork> getAllNetworksList(List<FeatureListRow> rows,
      @Nullable IonNetworkSorter sorter, boolean onlyBest) {
    final List<IonNetwork> results;
    if (onlyBest) {
      // ion networks are mutable, so streaming over them and calling distinct may create leaks
      // if the network is changed during the streaming
      // this is why we need to collect all distinct networks in a list and return this list
      results = rows.stream()
          // map to IonNetwork of best ion identity
          .map(r -> {
            final IonIdentity ion = r.getBestIonIdentity();
            if (ion == null) {
              return null;
            }
            return ion.getNetwork();
          }).filter(Objects::nonNull).distinct()
          // filter that all rows have this set to best Ion identity
          .filter(net -> net.getNodes().stream().allMatch(node -> {
            final IonIdentity ion = node.row().getBestIonIdentity();
            return ion != null && net.equals(ion.getNetwork());
          }))
          // modifiable for sorting
          .collect(CollectionUtils.toArrayList());
    }
    // get all IOnNetworks
    else {
      results = rows.stream()//
          .flatMap(r -> r.getIonIdentities().stream().map(IonIdentity::getNetwork)
              .filter(Objects::nonNull)).distinct()
          // modifiable for sorting
          .collect(CollectionUtils.toArrayList());
    }
    if (sorter != null) {
      results.sort(sorter);
    }
    return results;
  }

  /**
   * Renumber all networks of a feature list in ascending order of the retention time (0-based). The
   * ion identities of all rows are re-pointed to the renumbered networks.
   *
   * @return the renumbered networks in ascending retention time order
   */
  public static @NotNull List<IonNetwork> renumberNetworks(
      @NotNull ModularFeatureList featureList) {
    final List<IonNetwork> nets = getAllNetworksList(featureList.getRows(),
        new IonNetworkSorter(SortingProperty.RT, SortingDirection.Ascending), false);
    final List<IonNetwork> renumbered = new ArrayList<>(nets.size());
    for (int i = 0; i < nets.size(); i++) {
      renumbered.add(nets.get(i).withID(i));
    }
    return renumbered;
  }

  /**
   * Recreates the ion identity networks of {@code sourceRows} on their copied rows and replaces the
   * ion identities of those copies.
   * <p>
   * A copied row initially still holds the {@link IonIdentity} instances of its source row, and
   * those point through {@link IonIdentity#getNetwork()} at the rows of the original feature list.
   * Left alone, the copy would describe networks of foreign rows, which is why every copy of a
   * feature list that copies rows has to call this.
   * <p>
   * A network shrinks to the rows that were copied and is only dropped once it is empty, when all
   * of its rows were filtered out. A network of a single row is kept on purpose: filtering away the
   * other members must not take the ion annotation of the surviving row with it.
   * <p>
   * {@code sourceRows} may be the rows of the target itself, for a module that filters in place.
   * Networks that lost no row are then left untouched, ion identities and all, so that anything
   * holding on to them stays valid.
   *
   * @param sourceRows the rows of the original feature list
   * @param rowMapping maps a source row to its copy, or to null if that row was not copied
   */
  public static void remapIonNetworks(@NotNull final List<FeatureListRow> sourceRows,
      @NotNull final Function<FeatureListRow, ? extends FeatureListRow> rowMapping) {
    // keyed by identity on purpose: rows of one network often share an ion type, and those ion
    // identities compare equal by IonIdentity#compareTo. Each one still has to map to its own copy.
    final Map<IonIdentity, IonIdentity> ionMapping = new IdentityHashMap<>();

    for (final IonNetwork net : getAllNetworksList(sourceRows, null, false)) {
      // a network whose every row maps to itself needs no work, which is the case when a feature
      // list is processed in place. Keep it and its ion identities, so anything holding on to them
      // stays valid. A row mapped to null was filtered and counts as a change.
      boolean unchanged = true;
      for (final IonNetworkNode node : net.getNodes()) {
        if (rowMapping.apply(node.row()) != node.row()) {
          unchanged = false;
          break;
        }
      }
      if (unchanged) {
        continue;
      }

      final List<IonNetworkNode> newNodes = new ArrayList<>(net.size());
      final List<IonIdentity> sourceIons = new ArrayList<>(net.size());
      for (final IonNetworkNode node : net.getNodes()) {
        final FeatureListRow newRow = rowMapping.apply(node.row());
        if (newRow == null) {
          // the row was filtered out, the remaining members keep their annotation
          continue;
        }
        newNodes.add(new IonNetworkNode(newRow, copyIon(node.ion())));
        sourceIons.add(node.ion());
      }
      if (newNodes.isEmpty()) {
        // all member rows are gone, so the network disappears with them
        continue;
      }
      // re-points the copied ions at the new network
      new SimpleIonNetwork(net.getID(), newNodes, net.getMolFormulas()).setNetworkToAllRows();
      for (int i = 0; i < newNodes.size(); i++) {
        ionMapping.put(sourceIons.get(i), newNodes.get(i).ion());
      }
    }

    for (final FeatureListRow sourceRow : sourceRows) {
      final FeatureListRow newRow = rowMapping.apply(sourceRow);
      if (newRow == null) {
        continue;
      }
      final List<IonIdentity> sourceIons = sourceRow.getIonIdentities();
      if (sourceIons.isEmpty()) {
        continue;
      }
      // keep the order, the first ion identity is the preferred one
      final List<IonIdentity> newIons = new ArrayList<>(sourceIons.size());
      for (final IonIdentity sourceIon : sourceIons) {
        final IonIdentity mapped = ionMapping.get(sourceIon);
        if (mapped != null) {
          newIons.add(mapped);
        } else if (newRow == sourceRow) {
          // processed in place and this network was left untouched above, so the ion identity
          // already points at the right rows
          newIons.add(sourceIon);
        } else if (sourceIon.getNetwork() == null) {
          // not part of any network, so there is nothing to remap - still copy it so that the
          // copied row does not share a mutable ion identity with its source
          newIons.add(copyIon(sourceIon));
        }
        // otherwise every row of its network is gone and the ion identity is dropped with it
      }
      if (newRow == sourceRow && newIons.equals(sourceIons)) {
        // nothing changed for this row, do not replace its list
        continue;
      }
      newRow.setIonIdentities(newIons.isEmpty() ? null : List.copyOf(newIons));
    }
  }

  /**
   * A copy without the network back reference, which is set once the new network exists.
   */
  private static @NotNull IonIdentity copyIon(@NotNull final IonIdentity ion) {
    final IonIdentity copy = new IonIdentity(ion.getIonType());
    copy.addMolFormulas(ion.getMolFormulas());
    return copy;
  }
}
