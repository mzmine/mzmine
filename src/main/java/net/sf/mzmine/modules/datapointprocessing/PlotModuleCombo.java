package net.sf.mzmine.modules.datapointprocessing;

import java.util.List;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;

public class PlotModuleCombo {
  SpectraPlot plot;
  List<Class <DataPointProcessingModule>> modules;
  
  PlotModuleCombo (List<Class<DataPointProcessingModule>> modules, SpectraPlot plot){
    setPlot(plot);
    setModules(modules);
  }

  public SpectraPlot getPlot() {
    return plot;
  }

  public List<Class <DataPointProcessingModule>> getModules() {
    return modules;
  }

  public void setPlot(SpectraPlot plot) {
    this.plot = plot;
  }

  public void setModules(List<Class <DataPointProcessingModule>> modules) {
    this.modules = modules;
  }
  
  public boolean addModule(Class<DataPointProcessingModule> module) {
    return this.modules.add(module);
  }
  
  public boolean removeTask(Class<DataPointProcessingModule> module) {
    return this.modules.remove(module);
  }
}