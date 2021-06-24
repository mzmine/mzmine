package io.github.mzmine.modules.dataprocessing.gapfill_peakfinder;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;

/**
 * Contains additional information for a data point found during gap filling.
 */
public interface GapDataPoint extends DataPoint {

  double getRT();

  Scan getScan();
}
