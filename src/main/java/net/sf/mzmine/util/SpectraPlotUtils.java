package net.sf.mzmine.util;

import java.util.List;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;

public class SpectraPlotUtils {

  /**
   * Removes all label generators of datasets that are not of the given type.
   * 
   * @param plot Plot to apply this method to.
   * @param ignore List of class objects of the instances to ignore.
   */
  public static void clearDatasetLabelGenerators(SpectraPlot plot,
      List<Class<? extends XYDataset>> ignore) {
    for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
      XYDataset dataset = plot.getXYPlot().getDataset(i);
      // check if object of dataset is an instance of ignore.class
      boolean remove = true;
      for (Class<? extends XYDataset> datasetClass : ignore) {
        if ((datasetClass.isInstance(dataset)))
          remove = false;
      }

      if (remove)
        plot.getXYPlot().getRendererForDataset(dataset).setDefaultItemLabelGenerator(null);
    }
  }

  /**
   * Removes all label generators of datasets that are not of the given type.
   * 
   * @param plot Plot to apply this method to.
   * @param ignore Class object of the instances to ignore.
   */
  public static void clearDatasetLabelGenerators(SpectraPlot plot, Class<? extends XYDataset> ignore) {
    for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
      XYDataset dataset = plot.getXYPlot().getDataset(i);
      // check if object of dataset is an instance of ignore.class
      if (!(ignore.isInstance(dataset)))
        plot.getXYPlot().getRendererForDataset(dataset).setDefaultItemLabelGenerator(null);
    }
  }
}
