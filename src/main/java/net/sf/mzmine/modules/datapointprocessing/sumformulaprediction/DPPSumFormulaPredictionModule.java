package net.sf.mzmine.modules.datapointprocessing.sumformulaprediction;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DPPSumFormulaPredictionModule implements DataPointProcessingModule {

  @Override
  public String getName() {
    return "Sum formula prediction";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return DPPSumFormulaPredictionParameters.class;
  }

  @Override
  public ModuleSubCategory getModuleSubCategory() {
    return ModuleSubCategory.IDENTIFICATION;
  }

  @Override
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, ParameterSet parameterSet,
      SpectraPlot plot, DataPointProcessingController controller, TaskStatusListener listener) {
    return new DPPSumFormulaPredictionTask(dataPoints, plot, parameterSet, controller, listener);
  }

}
