package net.sf.mzmine.modules.datapointprocessing.massdetection;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DPPMassDetectionModule implements DataPointProcessingModule {

  @Override
  public String getName() {
    return "DataPoint mass detection";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {

    return DPPMassDetectionParameters.class;
  }

  @Override
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, ParameterSet parameterSet,
      SpectraPlot plot, DataPointProcessingController controller, TaskStatusListener listener) {

    return new DPPMassDetectionTask(dataPoints, plot,  parameterSet, controller, listener);
  }

  @Override
  public ModuleSubCategory getModuleSubCategory() {
    return ModuleSubCategory.MASSDETECTION;
  }

}
