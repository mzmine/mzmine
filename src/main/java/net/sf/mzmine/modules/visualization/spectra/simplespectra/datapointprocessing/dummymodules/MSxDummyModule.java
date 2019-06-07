package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.dummymodules;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager.MSLevel;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class MSxDummyModule implements DataPointProcessingModule {

  @Override
  public String getName() {
    return "MSANY dummy";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return DummyParameters.class;
  }

  @Override
  public ModuleSubCategory getModuleSubCategory() {
    return ModuleSubCategory.DUMMY;
  }

  @Override
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, ParameterSet parameterSet,
      SpectraPlot plot, DataPointProcessingController controller, TaskStatusListener listener) {
    return new DummyTask(dataPoints, plot, parameterSet, controller, listener);
  }

  @Override
  public MSLevel getApplicableMSLevel() {
    return MSLevel.MSANY;
  }
  
}
