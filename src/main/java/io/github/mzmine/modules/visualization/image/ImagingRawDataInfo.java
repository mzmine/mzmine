package io.github.mzmine.modules.visualization.image;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.ImagingRawDataFile;

public class ImagingRawDataInfo {

  private ImagingRawDataFile rawDataFile;
  private String name;
  private Integer numberOfScans;
  private Range<Double> dataMzRange;

  public ImagingRawDataInfo(ImagingRawDataFile rawDataFile) {
    this.rawDataFile = rawDataFile;
    init();
  }

  private void init() {
    name = rawDataFile.getName();
    numberOfScans = rawDataFile.getNumOfScans();
    dataMzRange = rawDataFile.getDataMZRange();
  }

  public ImagingRawDataFile getRawDataFile() {
    return rawDataFile;
  }

  public void setRawDataFile(ImagingRawDataFile rawDataFile) {
    this.rawDataFile = rawDataFile;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getNumberOfScans() {
    return numberOfScans;
  }

  public void setNumberOfScans(Integer numberOfScans) {
    this.numberOfScans = numberOfScans;
  }

  public Range<Double> getDataMzRange() {
    return dataMzRange;
  }

  public void setDataMzRange(Range<Double> dataMzRange) {
    this.dataMzRange = dataMzRange;
  }

}
