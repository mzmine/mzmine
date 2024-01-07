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

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.networks.IonNetworkSorter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class IonNetworkLogic {

  private static final Logger LOG = Logger.getLogger(IonNetworkLogic.class.getName());

  /**
   * Compare for likelyhood comparison and sorting
   *
   * @param a ion a
   * @param b ion b
   * @return same as comparable: -1 0 1 if the first argument is less, equal or better
   */
  public static int compareRows(IonIdentity a, IonIdentity b, RowGroup g) {
    if (a == null && b == null) {
      return 0;
    }
    // M+? (undefined
    else if (a == null || a.getIonType().isUndefinedAdductParent()) {
      return -1;
    } else if (b == null || b.getIonType().isUndefinedAdductParent()) {
      return 1;
    }
    // M-H2O+? (one is? undefined
    else if (a.getIonType().isUndefinedAdduct() && !b.getIonType().isUndefinedAdduct()) {
      return -1;
    } else if (!a.getIonType().isUndefinedAdduct() && b.getIonType().isUndefinedAdduct()) {
      return 1;
    }

    // network size, MSMS modification and multimer (2M) verification
    int result = Integer.compare(a.getLikelyhood(), b.getLikelyhood());
    if (result != 0) {
      return result;
    }
    // if a has less nM molecules in cluster
    result = Integer.compare(b.getIonType().getMolecules(), a.getIonType().getMolecules());
    if (result != 0) {
      return result;
    }

    int bLinks = getLinksTo(b, g);
    int aLinks = getLinksTo(a, g);
    result = Integer.compare(aLinks, bLinks);
    if (result != 0) {
      return result;
    }

    return compareCharge(a, b);
  }

  /**
   * @param g can be null. can be used to limit the number of links
   * @return number of links to an ion identity
   */
  public static int getLinksTo(IonIdentity ion, RowGroup g) {
    // TODO change to real links after refinement
    if (g == null) {
      return ion.getPartnerRows().size();
    } else {
      int c = 0;
      for (FeatureListRow row : ion.getPartnerRows()) {
        if (g.contains(row)) {
          c++;
        }
      }

      return c;
    }
  }

  /**
   * @param a
   * @param b
   * @return True if b is a better choice
   */
  private static int compareCharge(IonIdentity a, IonIdentity b) {
    int ca = a.getIonType().getAbsCharge();
    int cb = b.getIonType().getAbsCharge();
    return Integer.compare(ca, cb);
  }


  /**
   * Create list of AnnotationNetworks and set net ID
   *
   * @return
   */
  public static List<IonNetwork> createAnnotationNetworks(FeatureList pkl, MZTolerance mzTolerance,
      boolean useGrouping) {
    if (useGrouping && pkl.getGroups() != null) {
      List<IonNetwork> nets = new ArrayList<>();
      for (RowGroup g : pkl.getGroups()) {
        nets.addAll(createAnnotationNetworks(g.getRows(), mzTolerance));
      }

      return nets;
    } else {
      return createAnnotationNetworks(pkl.getRows(), mzTolerance);
    }
  }


  /**
   * Method 2: all that point to the same molecule (even without edge)
   *
   * @param rows
   * @return
   */
  public static List<IonNetwork> createAnnotationNetworks(List<FeatureListRow> rows,
      MZTolerance mzTolerance) {
    // bin neutral masses to annotation networks
    List<IonNetwork> nets = new ArrayList<>(binNeutralMassToNetworks(rows, mzTolerance));

    // add network to all identities
    setNetworksToAllAnnotations(nets);

    // fill in neutral losses [M-H2O] is not iserted yet
    // they might be if [M+H2O+X]+ was also annotated by another link
    fillInNeutralLosses(rows, nets, mzTolerance);

    resetNetworkIDs(nets);
    return nets;
  }


  public static void resetNetworkIDs(List<IonNetwork> nets) {
    for (int i = 0; i < nets.size(); i++) {
      nets.get(i).setID(i);
    }
  }

  /**
   * Need to reset networks to annotations afterwards
   *
   * @param nets
   */
  private static void splitByGroups(List<IonNetwork> nets) {
    int size = nets.size();
    for (int i = 0; i < size; i++) {
      IonNetwork net = nets.get(i);
      if (!net.allSameCorrGroup()) {
        nets.addAll(splitByGroup(net));
        nets.remove(i);
        i--;
        size--;
      }
    }
  }

  /**
   * Split network into correlation groups. need to reset network to ids afterwards
   *
   * @param net
   * @return
   */
  private static Collection<IonNetwork> splitByGroup(IonNetwork net) {
    Map<Integer, IonNetwork> map = new HashMap<>();
    for (Entry<FeatureListRow, IonIdentity> e : net.entrySet()) {
      Integer id = e.getKey().getGroupID();
      if (id != -1) {
        IonNetwork nnet = map.get(id);
        if (nnet == null) {
          // new network for group
          nnet = new IonNetwork(net.getMZTolerance(), -1);
          map.put(id, nnet);
        }
        nnet.put(e.getKey(), e.getValue());
      } else {
        // delete id if no corr group
        e.getValue().delete(e.getKey());
      }
    }
    return map.values();
  }

  /**
   * fill in neutral losses [M-H2O+?] is not inserted yet. they might be if [M+H2O+X]+ was also
   * annotated by another link
   *
   * @param rows
   * @param nets
   */
  private static void fillInNeutralLosses(List<FeatureListRow> rows, Collection<IonNetwork> nets,
      MZTolerance mzTolerance) {
    for (FeatureListRow row : rows) {
      if (row.hasIonIdentity()) {
        for (AtomicInteger index = new AtomicInteger(0); index.get() < row.getIonIdentities()
            .size(); index.incrementAndGet()) {
          IonIdentity neutral = row.getIonIdentities().get(index.get());
          // only if charged (neutral losses do not point to the real neutral mass)
          if (!neutral.getIonType().isModifiedUndefinedAdduct()) {
            continue;
          }

          // all partners
          ConcurrentHashMap<FeatureListRow, IonIdentity> partnerIDs = neutral.getPartner();
          for (Entry<FeatureListRow, IonIdentity> p : partnerIDs.entrySet()) {
            FeatureListRow partner = p.getKey();
            if (partner == null) {
              continue;
            }

            IonNetwork[] partnerNets = IonNetworkLogic.getAllNetworks(partner);
            // create new net if partner was in no network
            if (partnerNets == null || partnerNets.length == 0) {
              // create new and put both
              IonNetwork newNet = new IonNetwork(mzTolerance, nets.size());
              nets.add(newNet);
              newNet.put(row, neutral);
              newNet.put(partner, p.getValue());
              newNet.setNetworkToAllRows();
            } else {
              // add neutral loss to nets
              // do not if its already in this network (e.g. as adduct)
              Arrays.stream(partnerNets).filter(pnet -> !pnet.containsKey(row)).forEach(pnet -> {
                // try to find real annotation
                IonType pid = pnet.get(partner).getIonType();
                // modified
                pid = pid.createModified(neutral.getIonType().getModification());

                IonIdentity realID = neutral;
                if (pnet.checkForAnnotation(row, pid)) {
                  realID.setMSMSIdentities(neutral.getMSMSIdentities());
                  // create new
                  realID = new IonIdentity(pid);
                  row.addIonIdentity(realID, true);
                  index.incrementAndGet();
                  realID.setNetwork(pnet);
                  // set partners
                  pnet.addAllLinksTo(row, realID);
                  // put
                  pnet.put(row, realID);
                }
              });
            }
          }
        }
      }
    }
  }

  /**
   * All annotation networks of all annotations of row
   *
   * @param row
   * @return
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
   * Binning of all neutral masses described by all annotations of rows with 0.1 Da binning width
   * (masses should be very different)
   *
   * @param rows
   * @return AnnotationNetworks
   */
  private static Collection<IonNetwork> binNeutralMassToNetworks(List<FeatureListRow> rows,
      MZTolerance mzTolerance) {
    Map<Integer, IonNetwork> map = new HashMap<>();
    for (FeatureListRow row : rows) {
      if (!row.hasIonIdentity()) {
        continue;
      }

      for (IonIdentity adduct : row.getIonIdentities()) {
        // only if charged (neutral losses do not point to the real neutral mass)
        if (adduct.getIonType().getAbsCharge() == 0) {
          continue;
        }

        double mass = adduct.getIonType().getMass(row.getAverageMZ());
        // bin to 0.1
        Integer nmass = (int) Math.round(mass * 10.0);

        IonNetwork net = map.get(nmass);
        if (net == null) {
          // create new
          net = new IonNetwork(mzTolerance, map.size());
          map.put(nmass, net);
        }
        // add row and id to network
        net.put(row, adduct);
      }
    }
    return map.values();
  }

  /**
   * Neutral mass of AnnotationNetwork entry (ion and peaklistrow)
   *
   * @param e
   * @return
   */
  public static double calcMass(Entry<FeatureListRow, IonIdentity> e) {
    return e.getValue().getIonType().getMass(e.getKey().getAverageMZ());
  }

  /**
   * Add all rows of a network
   *
   * @param current
   * @param row
   * @param rows
   * @return false if this network has already been created
   */
  private static boolean addRow(IonNetwork current, FeatureListRow row, List<FeatureListRow> rows,
      int masterID) {
    if (row.hasIonIdentity()) {
      for (IonIdentity adduct : row.getIonIdentities()) {
        // try to add all
        if (current.isEmpty()) {
          current.put(row, adduct);
        }

        // add all connection for ids>rowID
        ConcurrentHashMap<FeatureListRow, IonIdentity> ids = adduct.getPartner();
        for (Entry<FeatureListRow, IonIdentity> entry : ids.entrySet()) {
          int id = entry.getKey().getID();
          if (id != masterID) {
            if (id > masterID) {
              FeatureListRow row2 = entry.getKey();
              IonIdentity adduct2 = entry.getValue();
              // new row found?
              if (!current.containsKey(row2)) {
                current.put(row2, adduct2);
                boolean isNewNet = addRow(current, row2, rows, masterID);
                if (!isNewNet) {
                  return false;
                }
              }
            } else {
              // id was smaller - trash this network, its already added
              return false;
            }
          }
        }
      }
    }
    // is new network
    return true;
  }

  public static FeatureListRow findRowByID(int id, List<FeatureListRow> rows) {
    if (rows == null) {
      return null;
    } else {
      for (FeatureListRow r : rows) {
        if (r.getID() == id) {
          return r;
        }
      }

      return null;
    }
  }

  /**
   * All MS annotation connections to all ions annotation
   *
   * @return
   */
  public static List<FeatureListRow> findAllAnnotationConnections(List<FeatureListRow> rows,
      FeatureListRow row) {
    if (!row.hasIonIdentity()) {
      return List.of();
    }

    return row.getIonIdentities().stream().flatMap(ion -> ion.getPartner().keySet().stream())
        .collect(Collectors.toList());
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
    final List<IonIdentity> sorted = ident.stream()
        .sorted(((Comparator<IonIdentity>) (a, b) -> compareRows(a, b, group)).reversed()).toList();
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
   * @param peakList
   * @param removeEmpty
   */
  public static void recalcAllAnnotationNetworks(FeatureList peakList, boolean removeEmpty) {
    List<IonNetwork> list = streamNetworks(peakList, false).collect(Collectors.toList());
    for (IonNetwork n : list) {
      if (removeEmpty && n.size() < 2) {
        n.delete();
      } else {
        n.recalcConnections();
      }
    }
  }

  /**
   * All annnotaion networks of the peaklist
   *
   * @param peakList
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
      @Nullable IonNetworkSorter sorter,
      boolean onlyBest) {
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
    Stream<IonNetwork> stream = null;
    if (onlyBest) {
      stream = rows.stream().filter(FeatureListRow::hasIonIdentity)
          // map to IonNetwork of best ion identity
          .map(r -> {
            IonNetwork net = r.getBestIonIdentity().getNetwork();
            if (net.hasSmallestID(r)) {
              return net;
            } else {
              return null;
            }
          }).filter(Objects::nonNull)
          // filter that all PeakListRows have this set to best Ion identity
          .filter(net -> net.keySet().stream()
              .allMatch(r -> r.hasIonIdentity() && r.getBestIonIdentity().getNetwork() != null
                             && r.getBestIonIdentity().getNetwork().getID() == net.getID()));
    }
    // get all IOnNetworks
    else {
      stream = rows.stream()//
          // .filter(r -> {
          // if (r.getID() == 1003)
          // return true;
          // else
          // return false;
          // }) //
          .filter(FeatureListRow::hasIonIdentity) //
          .flatMap(r -> r.getIonIdentities().stream().map(IonIdentity::getNetwork)
              .filter(Objects::nonNull).filter(net -> net.hasSmallestID(r)));
    }
    if (sorter != null) {
      stream = stream.sorted(sorter);
    }
    return stream;
  }

  /**
   * Best annotation network in group
   *
   * @param group
   * @return
   */
  public static IonNetwork getBestNetwork(RowGroup group) {
    return group.stream().filter(FeatureListRow::hasIonIdentity).flatMap(
        r -> r.getIonIdentities().stream().map(IonIdentity::getNetwork).filter(Objects::nonNull))
        .min(Comparator.naturalOrder()).orElse(null);
  }

  /**
   * Renumber all networks in a feature list in ascending order of the retention time (0-based)
   *
   * @param featureList
   */
  public static void renumberNetworks(ModularFeatureList featureList) {
    AtomicInteger netID = new AtomicInteger(0);
    IonNetworkLogic
        .streamNetworks(featureList,
            new IonNetworkSorter(SortingProperty.RT, SortingDirection.Ascending), false)
        .forEach(n -> n.setID(netID.getAndIncrement()));
  }
}
