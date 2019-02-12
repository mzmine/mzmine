package net.sf.mzmine.modules.datapointprocessing;

import java.util.ArrayList;
import java.util.List;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModulesList;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;

public class DataPointProcessingController {

  List<PlotModuleCombo> pmc;

  DataPointProcessingController() {
    pmc = new ArrayList<PlotModuleCombo>();

  }

  /**
   * 
   * @param task Tasks to be executed for the plot.
   * @param plot Plot the results shall be executed to.
   * @return PlotTaskCombo object if added successfully.
   * @throws MSDKRuntimeException if not added.
   */
  public PlotModuleCombo addPlotModuleCombo(List<Class<DataPointProcessingModule>> moduleList,
      SpectraPlot plot) throws MSDKRuntimeException {
    PlotModuleCombo modules = new PlotModuleCombo(moduleList, plot);
    if (addPlotModuleCombo(modules))
      return modules;
    throw new MSDKRuntimeException("Could not add task list for plot " + plot.toString() + ".");
  }

  /**
   * 
   * @param c
   * @return {@link java.util.Collection.add(E e)}
   */
  public boolean addPlotModuleCombo(PlotModuleCombo c) {
    if (pmc.contains(c))
      return false;
    return pmc.add(c);
  }

  /**
   * 
   * @param c
   * @return {@link java.util.Collection.remove(E e)}
   */
  public boolean removePlotTaskCombo(PlotModuleCombo c) {
    return pmc.remove(c);
  }

  public void executeAll(DataPoint[] dp) {
    for (PlotModuleCombo c : pmc) {
      MZmineProcessingModule module = MZmineCore.getModuleInstance(c.getModules().get(0));

      if (module instanceof DataPointProcessingModule) {

        Task task = ((DataPointProcessingModule) module).createTask(dp, pmc.get(0).getPlot(), new DataPointProcessingListener() {
          @Override
          public void handle(DataPointProcessingEvent event) {
            
          }
        });

        MZmineCore.getTaskController().addTask(task);
      }
    }
  }
  
  public void execute(DataPoint[] dp, Class<DataPointProcessingModule> module, SpectraPlot plot) {
    MZmineProcessingModule inst = MZmineCore.getModuleInstance(module);
    
    if (inst instanceof DataPointProcessingModule) {
      Task task = ((DataPointProcessingModule) inst).createTask(dp, plot, new DataPointProcessingListener() {
        
        @Override
        public void handle(DataPointProcessingEvent event) {
          if(event.getTask().isFinished()) {
            // start next task here
          }
        }
      });
      
      MZmineCore.getTaskController().addTask(task);
    }
  }
}
