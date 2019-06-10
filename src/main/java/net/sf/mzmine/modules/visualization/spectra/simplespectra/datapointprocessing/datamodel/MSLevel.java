package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import java.util.Arrays;

/**
 * Used by datapointprocessing methods to differentiate between MS^1 and MS/MS. Also used to
 * dynamically load processing queues and create elements on the interface.
 * 
 * CAUTION: The method croppedValues() is used to dynamically create and load different processing
 * queues and everything regarding them on the interface. When adding new Types to this enum, make
 * sure MSANY stays the last one. MSANY is used by methods, that are applicable on any MS-level,
 * but it does not get it's own processing queue. This is why croppedValues() exists and cuts off
 * the last value of the values() method.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public enum MSLevel {
  MSONE("MS"), MSMS("MS/MS"), MSANY("MS-any");

  final String name;

  MSLevel(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static MSLevel[] cropValues() {
    MSLevel[] values = MSLevel.values();
    return Arrays.copyOf(values, values.length-1);
  }
};