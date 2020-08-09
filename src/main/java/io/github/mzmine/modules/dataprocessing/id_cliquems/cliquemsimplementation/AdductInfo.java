package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.Immutable;

@Immutable
public class AdductInfo {

  public Integer feature;
  public List<String> annotations;
  public List<Double> masses;
  public List<Double> scores;

  public AdductInfo(Integer feature, List<String> annotations, List<Double> masses, List<Double> scores){
    this.feature = feature;
    this.annotations = annotations;
    this.masses = masses;
    this.scores = scores;
  }
}
