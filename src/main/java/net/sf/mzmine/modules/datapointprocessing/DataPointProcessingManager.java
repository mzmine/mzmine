package net.sf.mzmine.modules.datapointprocessing;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;

public class DataPointProcessingManager {

  public class PlotTaskCombo {
    SpectraPlot plot;
    List<DataPointProcessingTask> tasks;
    
    PlotTaskCombo (SpectraPlot plot, List<DataPointProcessingTask> tasks){
      setPlot(plot);
      setTasks(tasks);
    }

    public SpectraPlot getPlot() {
      return plot;
    }

    public List<DataPointProcessingTask> getTasks() {
      return tasks;
    }

    public void setPlot(SpectraPlot plot) {
      this.plot = plot;
    }

    public void setTasks(List<DataPointProcessingTask> tasks) {
      this.tasks = tasks;
    }
  }
  
  List<PlotTaskCombo> ptc;
  
  DataPointProcessingManager(){
    ptc = new ArrayList<PlotTaskCombo>();
  }
  
  public boolean addPlotTaskCombo(PlotTaskCombo c) {
    return ptc.add(c);
  }
  
  public boolean removePlotTaskCombo(PlotTaskCombo c) {
    return ptc.remove(c);
  }
}
