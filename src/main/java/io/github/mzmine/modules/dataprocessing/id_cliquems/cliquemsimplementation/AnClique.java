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
