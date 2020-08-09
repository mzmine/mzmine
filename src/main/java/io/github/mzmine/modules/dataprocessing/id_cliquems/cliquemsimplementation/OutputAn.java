package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OutputAn {

  private int NUM_ANNO = 5;

  public List<Integer> features;
  public List<HashMap<Integer, String>> ans = new ArrayList<>();
  public List<HashMap<Integer, Double>> masses  = new ArrayList<>();
  public List<HashMap<Integer, Double>> scores = new ArrayList<>();

  public OutputAn(List<Integer> features){
    this.features = features;

    for (int i = 0; i < NUM_ANNO; i++) {
      HashMap<Integer, String> tempAn = new HashMap<>();
      ans.add(tempAn);
      HashMap<Integer, Double> tempmass = new HashMap<>();
      masses.add(tempmass);
      HashMap<Integer, Double> tempScore = new HashMap<>();
      scores.add(tempScore);
    }

    for(Integer itv : features) {
      for (int i = 0; i < NUM_ANNO; i++) {
        this.ans.get(i).put(itv,"NA");
        this.masses.get(i).put(itv,0.0);
        this.scores.get(i).put(itv,0.0);
      }
    }
  }

}
