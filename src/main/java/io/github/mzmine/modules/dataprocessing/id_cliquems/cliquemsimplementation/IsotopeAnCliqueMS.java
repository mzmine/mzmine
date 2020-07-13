package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

import java.util.ArrayList;
import java.util.List;
import javafx.util.Pair;

public class IsotopeAnCliqueMS {
  private final List<Pair<Double, Pair<Double,Integer>>> isoData;

  private final List<Integer> pfeature = new ArrayList<>();
  private final List<Integer> ifeature = new ArrayList<>();
  private final List<Integer> pcharge = new ArrayList<>();
  private final List<Integer> icharge = new ArrayList<>();

  public IsotopeAnCliqueMS(List<Pair<Double, Pair<Double,Integer>>> isoData){
    this.isoData = isoData;
  }

  private boolean errorRange( Double mz1, Double mz2, Double reference, Double ppm ){
    boolean result = false;
    Double error = Math.abs(mz2 - mz1 - reference)/(mz1+reference);
    if( error <= Math.sqrt(2.0) * ppm * 0.000001)
      result = true;
    return result;
  }

  private IsoTest isIsotope(Double mz1, Double mz2, Integer maxCharge, Double ppm, Double isom){

    IsoTest finalIso = new IsoTest();

    for(int charge1 = 1; charge1 <= maxCharge; charge1++) {
      for(int charge2 = 1; charge2 <= maxCharge; charge2++) {
        IsoTest currentIso = new IsoTest();
        Double cmz1,cmz2;
        cmz1 = mz1*charge1;
        cmz2 = mz2*charge2;
        // if isotope mass is bigger than parental mass
        if(cmz2 > cmz1){
          currentIso.isIso = errorRange(cmz1,cmz2,isom,ppm);
          if(currentIso.isIso){
              finalIso.isIso = true;
              finalIso.pCharge = charge1;
              finalIso.iCharge = charge2;
          }
        }

      }}
    return finalIso;
  }

  private IsoTest isIsotope(Double mz1, Double mz2, Integer maxCharge, Double ppm){
    return isIsotope(mz1,mz2,maxCharge,ppm,1.003355);
  }

  public void getIsotopes(Integer maxCharge, Double ppm, Double  isom ){


    for(int id1 = 0; id1 < isoData.size() ; id1++) {
      for(int id2 = 1; id2 < isoData.size() ; id2++) {
        if(id2>id1){
          Double mz1 = isoData.get(id1).getValue().getKey();
          Double mz2 = isoData.get(id2).getValue().getKey();
          IsoTest iTest = isIsotope(mz1, mz2, maxCharge, ppm, isom);
          if(iTest.isIso){
            pfeature.add(isoData.get(id1).getValue().getValue());
            ifeature.add(isoData.get(id2).getValue().getValue());
            pcharge.add(iTest.pCharge);
            icharge.add(iTest.iCharge);
          }
        }
      }
    }

  }


  public List<Integer> getPfeature() {
    return pfeature;
  }

  public List<Integer> getIfeature() {
    return ifeature;
  }

  public List<Integer> getPcharge() {
    return pcharge;
  }

  public List<Integer> getIcharge() {
    return icharge;
  }


  private class IsoTest{
    boolean isIso = false;
    Integer pCharge;
    Integer iCharge;
  }

}
