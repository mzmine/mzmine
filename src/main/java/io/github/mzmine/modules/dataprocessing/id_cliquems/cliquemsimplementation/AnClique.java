package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import dulab.adap.datamodel.Peak;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.util.Pair;

public class AnClique {

  //TODO is RawDatafile required later in annotation?

  private List<PeakData> peakData;
  private RawDataFile dataFile;
  private NetworkCliqueMS network = new NetworkCliqueMS();
  boolean cliquesFound = false;
  public HashMap<Integer,List<Integer>> cliques = new HashMap<>();

  AnClique(List<PeakData> peakData, RawDataFile file){
    this.peakData = peakData;
    this.dataFile = file;
  }

  public List<PeakData> getPeakList(){
    return peakData;
  }

  public void changePeakDataList(List<PeakData> pd){
    this.peakData = pd;
  }

  public RawDataFile getRawDataFile(){
    return  dataFile;
  }

  public NetworkCliqueMS getNetwork(){
    return network;
  }

  public void computeCliqueFromResult(){
    List<Pair<Integer,Integer>> nodeCliqueList = this.network.getResultNode_clique();
    for(Pair<Integer,Integer> p : nodeCliqueList){
      if(this.cliques.containsKey(p.getValue())){
        this.cliques.get(p.getValue()).add(p.getKey());
      }
      else{
        List<Integer> l = new ArrayList<>();
        l.add(p.getKey());
        this.cliques.put(p.getValue(), l);
      }
    }
  }


}
