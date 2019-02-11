package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.taskcontrol.AbstractTask;

public abstract class DataPointProcessingTask extends AbstractTask {
  
  SpectraPlot targetPlot;

  
  public SpectraPlot getTargetPlot() {
    return targetPlot;
  }

  public void setTargetPlot(SpectraPlot targetPlot) {
    this.targetPlot = targetPlot;
  }
  
}
