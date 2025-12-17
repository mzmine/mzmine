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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.collections.CollectionUtils;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class IonNetworkLogic {

  /**
   * Compare for likelyhood comparison and sorting
   *
   * @param a ion a
   * @param b ion b
   * @return same as comparable: -1 0 1 if the first argument is less, equal or better
   */
  public static int compareIonIdentitiesLikelyhood(IonIdentity a, IonIdentity b) {
    if (a == null && b == null) {
      return 0;
    } else if (a == null) {
      return -1;
    } else if (b == null) {
      return 1;
    }
    // M-H2O+? (one is? undefined
    final boolean undefinedA = a.getIonType().isUndefinedAdduct();
    final boolean undefinedB = b.getIonType().isUndefinedAdduct();
    if (undefinedA && !undefinedB) {
      return -1;
    } else if (!undefinedA && undefinedB) {
      return 1;
    }

    // network size, MSMS modification and multimer (2M) verification
    int result = Integer.compare(a.getLikelyhood(), b.getLikelyhood());
    if (result != 0) {
      return result;
    }
    // if a has less nM molecules in cluster
    result = Integer.compare(b.getIonType().molecules(), a.getIonType().molecules());
    if (result != 0) {
      return result;
    }

    return compareCharge(a, b);
  }

  /**
   * @return True if b is a better choice
   */
  private static int compareCharge(IonIdentity a, IonIdentity b) {
    int ca = a.getIonType().absTotalCharge();
    int cb = b.getIonType().absTotalCharge();
    return Integer.compare(ca, cb);
  }


  public static void resetNetworkIDs(List<IonNetwork> nets) {
    for (int i = 0; i < nets.size(); i++) {
      nets.get(i).setID(i);
    }
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
   * Set the network to all its children rows
   *
   * @param nets
   */
  public static void setNetworksToAllAnnotations(Collection<IonNetwork> nets) {
    nets.stream().forEach(n -> n.setNetworkToAllRows());
  }

  /**
   * Sort all ion identities of a row by the likelyhood of being true.
   *
   * @param row
   * @return list of annotations or null
   */
  public static List<IonIdentity> sortIonIdentities(FeatureListRow row, boolean useGroup) {
    List<IonIdentity> ident = row.getIonIdentities();
    if (ident == null || ident.isEmpty()) {
      return null;
    }

    RowGroup group = useGroup ? row.getGroup() : null;

    // best is first
    final List<IonIdentity> sorted = ident.stream().sorted(
            ((Comparator<IonIdentity>) (a, b) -> compareIonIdentitiesLikelyhood(a, b)).reversed())
        .toList();
    row.setIonIdentities(sorted);
    return ident;
  }

  /**
   * Sort all ion identities of all rows
   *
   * @param pkl
   * @return
   */
  public static void sortIonIdentities(FeatureList pkl, boolean useGroup) {
    for (FeatureListRow r : pkl.getRows()) {
      sortIonIdentities(r, useGroup);
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
   * Renumber all networks in a feature list in ascending order of the retention time (0-based)
   *
   * @param featureList
   */
  public static void renumberNetworks(ModularFeatureList featureList) {
    AtomicInteger netID = new AtomicInteger(0);
    IonNetworkLogic.streamNetworks(featureList,
            new IonNetworkSorter(SortingProperty.RT, SortingDirection.Ascending), false)
        .forEach(n -> n.setID(netID.getAndIncrement()));
  }
}
