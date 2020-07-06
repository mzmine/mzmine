package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import dulab.adap.datamodel.Peak;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import java.util.List;

public class AnClique {

  //TODO is RawDatafile required later?

  private List<PeakData> peakData;
  private RawDataFile dataFile;
  private NetworkCliqueMS network = new NetworkCliqueMS();

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


}
