package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import javax.annotation.concurrent.Immutable;

@Immutable
public class IsotopeInfo {
    public Integer feature;
    public Integer charge;
    public Integer grade;
    public Integer cluster;

    public IsotopeInfo(Integer feature, Integer charge, Integer grade, Integer cluster){
      this.feature = feature;
      this.cluster = cluster;
      this.grade = grade;
      this.charge = charge;
    }
}
