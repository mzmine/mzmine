package io.github.mzmine.modules.io.tdfimport.datamodel;

import io.github.mzmine.datamodel.PolarityType;

public class TIMSFrameInfo {

  private final int id;
  private final double time;
  private final PolarityType polarityType;
  private final int scanMode;
  private final int msmsType;
  private final int maxIntensity;
  private final int test;

  public TIMSFrameInfo(int id, double time, PolarityType polarityType,
      int scanMode, int msmsType, int maxIntensity, int test) {
    this.id = id;
    this.time = time;
    this.polarityType = polarityType;
    this.scanMode = scanMode;
    this.msmsType = msmsType;
    this.maxIntensity = maxIntensity;
    this.test = test;
  }
}
