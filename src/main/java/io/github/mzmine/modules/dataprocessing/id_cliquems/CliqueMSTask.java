package io.github.mzmine.modules.dataprocessing.id_cliquems;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.modules.dataprocessing.id_camera.CameraSearchTask;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.ComputeCliqueModule;
import io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation.NetworkCliqueMS;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.logging.Logger;

public class CliqueMSTask extends AbstractTask {
  // Logger.
  private static final Logger logger = Logger.getLogger(CameraSearchTask.class.getName());

  // Feature list to process.
  private final PeakList peakList;

  // Task progress
  private double progress;

  // Project
  MZmineProject project;

  // Parameters.
  private final ParameterSet parameters;

  public CliqueMSTask(final MZmineProject project, final ParameterSet parameters,
      final PeakList list){
    this.project = project;
    this.parameters = parameters;
    peakList = list;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of pseudo-spectra in " + peakList;
  }

  @Override
  public double getFinishedPercentage() {

    return progress;
  }

  @Override
  public void cancel(){
    super.cancel();
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);

      ComputeCliqueModule cm = new ComputeCliqueModule(peakList,peakList.getRawDataFile(0));
      cm.getClique();

      // Finished.
      setStatus(TaskStatus.FINISHED);
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }
}
