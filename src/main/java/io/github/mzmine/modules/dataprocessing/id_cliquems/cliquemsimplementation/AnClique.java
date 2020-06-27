package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;

public class AnClique {

  PeakList peakList;
  RawDataFile dataFile;

  AnClique(PeakList peakList){
    this.peakList = peakList;
    this.dataFile = peakList.getRawDataFile(0);
  }


}
