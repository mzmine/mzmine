package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.multithreaded;

import java.util.function.Consumer;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;

public class SubTaskFinishListener implements Consumer<PeakList> {

  private final MZmineProject project;
  private ParameterSet parameters;
  private PeakList peakList;
  private int tasks;
  private int finished = 0;
  private boolean removeOriginal;


  public SubTaskFinishListener(MZmineProject project, ParameterSet parameters, PeakList peakList,
      boolean removeOriginal, int tasks) {
    super();
    this.project = project;
    this.parameters = parameters;
    this.peakList = peakList;
    this.tasks = tasks;
    this.removeOriginal = removeOriginal;
  }

  @Override
  public synchronized void accept(PeakList processedPeakList) {
    finished++;
    if (finished == tasks) {
      // add pkl to project
      // Append processed peak list to the project
      project.addPeakList(processedPeakList);

      // Add quality parameters to peaks
      QualityParameters.calculateQualityParameters(processedPeakList);

      // Add task description to peakList
      processedPeakList
          .addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod("Gap filling ", parameters));

      // Remove the original peaklist if requested
      if (removeOriginal)
        project.removePeakList(peakList);
    }
  }

}
