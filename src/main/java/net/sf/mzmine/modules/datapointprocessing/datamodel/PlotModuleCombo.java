package net.sf.mzmine.modules.datapointprocessing.datamodel;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;

public class PlotModuleCombo {
  SpectraPlot plot;
  List<Class<DataPointProcessingModule>> modules;
//  List<Class<? extends ParameterSet>> parameters;

  public PlotModuleCombo(List<Class<DataPointProcessingModule>> modules, SpectraPlot plot) {
    setPlot(plot);
    setModules(modules);
//    List<Class<? extends ParameterSet>> parameters = new ArrayList<Class<? extends ParameterSet>>();

//    for(Class<DataPointProcessingModule> module : modules) {
//      parameters.add(MZmineCore.getModuleInstance(module).getParameterSetClass());
//    }
  }

  public PlotModuleCombo() {
    modules = new ArrayList<Class<DataPointProcessingModule>>();
  }

  public boolean plotValid() {
    return (plot != null);
  }

  public SpectraPlot getPlot() {
    if (!plotValid())
      throw new MSDKRuntimeException("getPlot() called, although plot == null");
    return plot;
  }

  @Nonnull
  public List<Class<DataPointProcessingModule>> getModules() {
    return modules;
  }

  public void setPlot(SpectraPlot plot) {
    this.plot = plot;
  }

  public void setModules(List<Class<DataPointProcessingModule>> modules) {
    this.modules = modules;
  }

//  public Class<? extends ParameterSet> getModuleParameterSetClass(Class<DataPointProcessingModule> module){
//    return parameters.get(modules.indexOf(module));
//  }

  /**
   * 
   * @return Returns true if the module list is initialized and > 0.
   */
  public boolean modulesValid() {
    if(modules != null && modules.size() > 0)
      return true;
    return false;
  }

  public boolean addModule(Class<DataPointProcessingModule> module) {
    return this.modules.add(module);
  }

  public boolean removeTask(Class<DataPointProcessingModule> module) {
    return this.modules.remove(module);
  }

  /**
   * 
   * @param current A pointer to the current module. 
   * @return Returns true if there is one or more modules, false if not.
   */
  public boolean hasNextModule(Class<DataPointProcessingModule> current) {
    if (modules.contains(current)) {
      int index = modules.indexOf(current);
      if (modules.size() < index + 1) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param current A pointer to the current module.
   * @return Returns the next module in this PlotModuleCombo. If this pmc has no next module the
   *         return is null. Use hasNextModule to check beforehand.
   */
  public Class<DataPointProcessingModule> getNextModule(Class<DataPointProcessingModule> current) {
    if (hasNextModule(current))
      return modules.get(modules.indexOf(current) + 1);
    return null;
  }

  /**
   * 
   * @return Returns the first module in this PlotModuleCombo. If the list of modules is not
   *         initialised, the return is null.
   */
  public Class<DataPointProcessingModule> getFirstModule() {
    if (modules.size() > 0) {
      return modules.get(0);
    }
    return null;
  }
  
}
