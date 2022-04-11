/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */


package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.datamodel.RawDataFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.util.Pair;

/**
 * AnClique Class keeps all data structures related to grouping, annotating isotopes and adducts
 * <p>
 * See the AnClique class https://github.com/osenan/cliqueMS/blob/master/R/allClasses.R for the R
 * class corresponding to this class
 */
public class AnClique {


  private List<PeakData> peakDataList;
  private RawDataFile dataFile;
  private NetworkCliqueMS network = new NetworkCliqueMS();
  boolean cliquesFound = false;
  boolean isoFound = false;
  boolean anFound = false;
  private List<IsotopeInfo> isoInfos;
  private List<AdductInfo> adInfos;

  // key - clique ID, value - List of nodes which are part of the clique.
  public HashMap<Integer, List<Integer>> cliques = new HashMap<>();

  AnClique(List<PeakData> peakDataList, RawDataFile file) {
    this.peakDataList = peakDataList;
    this.dataFile = file;
  }

  public List<PeakData> getPeakDataList() {
    return peakDataList;
  }

  public void changePeakDataList(List<PeakData> pd) {
    this.peakDataList = pd;
  }

  public RawDataFile getRawDataFile() {
    return dataFile;
  }

  public NetworkCliqueMS getNetwork() {
    return network;
  }

  public void setIsoInfos(List<IsotopeInfo> isoInfos) {
    this.isoInfos = isoInfos;
  }

  public List<IsotopeInfo> getIsoInfos() {
    return isoInfos;
  }

  public List<AdductInfo> getAdInfos() {
    return adInfos;
  }

  public void setAdInfos(
      List<AdductInfo> adInfos) {
    this.adInfos = adInfos;
  }

  public void computeCliqueFromResult() {
    List<Pair<Integer, Integer>> nodeCliqueList = this.network.getResultNodeClique();

    //firstly, generate hash from nodeID to peakData
    HashMap<Integer, PeakData> nodeToPeak = new HashMap<>();
    for (PeakData pd : this.peakDataList) {
      nodeToPeak.put(pd.getNodeID(), pd);
    }

    for (Pair<Integer, Integer> p : nodeCliqueList) {
      if (this.cliques.containsKey(p.getValue())) {
        this.cliques.get(p.getValue()).add(p.getKey());
      } else {
        List<Integer> l = new ArrayList<>();
        l.add(p.getKey());
        this.cliques.put(p.getValue(), l);
      }

      //update clique IDs in peakData
      nodeToPeak.get(p.getKey()).setCliqueID(p.getValue());
    }
  }
}
