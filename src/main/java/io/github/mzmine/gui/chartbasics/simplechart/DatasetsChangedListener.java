package io.github.mzmine.gui.chartbasics.simplechart;

import java.util.Map;
import org.jfree.data.xy.XYDataset;

@FunctionalInterface
public interface DatasetsChangedListener {

  public void datasetsChanged(Map<Integer, XYDataset> newDatasets);
}
