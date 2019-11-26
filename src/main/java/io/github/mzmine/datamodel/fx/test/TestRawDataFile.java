package io.github.mzmine.datamodel.fx.test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;

public class TestRawDataFile implements RawDataFile {

  private String name;

  public TestRawDataFile(String name) {
    this.name = name;
  }

  /**
   * HashMap was not working
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null)
      return false;
    if (super.equals(obj) || obj == this)
      return true;
    if (!obj.getClass().isAssignableFrom(this.getClass()) && !(obj instanceof RawDataFile))
      return false;
    RawDataFile raw = (RawDataFile) obj;
    if (raw.getName().equals(this.getName()))
      return true;
    // TODO make sure this RawDataFile is the correct
    return false;
  }

  @Override
  public int hashCode() {
    // TODO make this unique - as name can change
    return getName().hashCode();
  }

  @Override
  @Nonnull
  public RawDataFile clone() throws CloneNotSupportedException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public String getName() {
    return name;
  }

  @Override
  public void setName(@Nonnull String name) {
    this.name = name;
  }

  @Override
  public int getNumOfScans() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int getNumOfScans(int msLevel) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  @Nonnull
  public int[] getMSLevels() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public int[] getScanNumbers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public int[] getScanNumbers(int msLevel) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public int[] getScanNumbers(int msLevel, @Nonnull Range<Double> rtRange) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public @Nullable Scan getScan(int scan) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getDataMZRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getDataRTRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getDataMZRange(int msLevel) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @Nonnull
  public Range<Double> getDataRTRange(int msLevel) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getDataMaxBasePeakIntensity(int msLevel) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getDataMaxTotalIonCurrent(int msLevel) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

}
