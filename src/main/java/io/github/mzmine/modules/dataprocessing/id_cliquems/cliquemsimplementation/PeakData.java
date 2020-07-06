package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;

public class PeakData {



  private double mz ;
  private double mzmin ;
  private double mzmax ;
  private double rt;
  private double rtmin ;
  private double rtmax ;
  private double intensity;

  public double getMz() {
    return mz;
  }

  public double getMzmin() {
    return mzmin;
  }

  public double getMzmax() {
    return mzmax;
  }

  public double getRt() {
    return rt;
  }

  public double getRtmin() {
    return rtmin;
  }

  public double getRtmax() {
    return rtmax;
  }

  public double getIntensity() {
    return intensity;
  }

  PeakData(double mz, double mzmin, double mzmax, double rt, double rtmin, double rtmax, double intensity){
    this.mz = mz ;
    this.mzmin =  mzmin ;
    this.mzmax = mzmax ;
    this.rt = rt ;
    this.rtmin = rtmin ;
    this.rtmax = rtmax ;
    this.intensity = intensity;
  }

  PeakData(PeakData p){
    this.mz = p.mz ;
    this.mzmin =  p.mzmin ;
    this.mzmax = p.mzmax ;
    this.rt = p.rt ;
    this.rtmin = p.rtmin ;
    this.rtmax = p.rtmax ;
    this.intensity = p.intensity;
  }

}
