package net.sf.mzmine.modules.datapointprocessing.datamodel;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;

public class PlotModuleCombo {
  SpectraPlot plot;
  List<MZmineProcessingStep<DataPointProcessingModule>> steps;

  public PlotModuleCombo(List<MZmineProcessingStep<DataPointProcessingModule>> steps, SpectraPlot plot) {
    setPlot(plot);
    setSteps(steps);
  }

  public PlotModuleCombo() {
    steps = new ArrayList<MZmineProcessingStep<DataPointProcessingModule>>();
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
  public List<MZmineProcessingStep<DataPointProcessingModule>> getsteps() {
    return steps;
  }

  public void setPlot(SpectraPlot plot) {
    this.plot = plot;
  }

  public void setSteps(List<MZmineProcessingStep<DataPointProcessingModule>> steps) {
    this.steps = steps;
  }

//  public Class<? extends ParameterSet> getModuleParameterSetClass(MZmineProcessingStep<DataPointProcessingModule> module){
//    return parameters.get(steps.indexOf(module));
//  }

  /**
   * 
   * @return Returns true if the module list is initialized and > 0.
   */
  public boolean stepsValid() {
    if(steps != null && steps.size() > 0)
      return true;
    return false;
  }

  public boolean addStep(MZmineProcessingStep<DataPointProcessingModule> module) {
    return this.steps.add(module);
  }

  public boolean removeTask(MZmineProcessingStep<DataPointProcessingModule> module) {
    return this.steps.remove(module);
  }

  /**
   * 
   * @param current A pointer to the current module. 
   * @return Returns true if there is one or more steps, false if not.
   */
  public boolean hasNextStep(MZmineProcessingStep<DataPointProcessingModule> current) {
    if (steps.contains(current)) {
      int index = steps.indexOf(current);
      if (index + 1 < steps.size()) {
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
  public MZmineProcessingStep<DataPointProcessingModule> getNextStep(MZmineProcessingStep<DataPointProcessingModule> current) {
    if (hasNextStep(current))
      return steps.get(steps.indexOf(current) + 1);
    return null;
  }

  /**
   * 
   * @return Returns the first module in this PlotModuleCombo. If the list of steps is not
   *         initialised, the return is null.
   */
  public MZmineProcessingStep<DataPointProcessingModule> getFirstStep() {
    if (steps.size() > 0) {
      return steps.get(0);
    }
    return null;
  }
  
}
