package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubstraction;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MZmineProjectListener;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.modules.peaklistmethods.alignment.join.JoinAlignerTask;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class PeakListBlankSubstractionTask extends AbstractTask {

  private static final String ALIGNED_BLANK_NAME = "Aligned blank";
  private static Logger logger =
      Logger.getLogger(PeakListBlankSubstractionParameters.class.getName());

  private MZmineProject project;
  private PeakListBlankSubstractionParameters parameters;

  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;

  private PeakListsSelection blankSelection;
  private PeakList[] blank, target;

  private int finishedRows, totalRows;
  
  private String message;
  
  public PeakListBlankSubstractionTask(MZmineProject project,
      PeakListBlankSubstractionParameters parameters) {
    
    message = "Initializing...";

    this.project = project;
    this.parameters = parameters;

    this.mzTolerance =
        parameters.getParameter(PeakListBlankSubstractionParameters.mzTolerance).getValue();
    this.rtTolerance =
        parameters.getParameter(PeakListBlankSubstractionParameters.rtTolerance).getValue();

    this.blankSelection =
        parameters.getParameter(PeakListBlankSubstractionParameters.blankPeakLists).getValue();
    this.blank = blankSelection.getMatchingPeakLists();
    this.target = parameters.getParameter(PeakListBlankSubstractionParameters.peakLists).getValue()
        .getMatchingPeakLists();

  }

  @Override
  public String getTaskDescription() {
    return "Blank susbtraction task";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    message = "Waiting for Join Aligner.";
    setStatus(TaskStatus.WAITING);

    // at first, create a join aligner task, to align the blank peak lists if needed.
    JoinAlignerParameters jp = createJoinAlignerParameters();

    JoinAlignerTask joinAlignerTask = new JoinAlignerTask(project, jp);

    MZmineCore.getTaskController().addTask(joinAlignerTask);
    project.addProjectListener(new MZmineProjectListener() {

      @Override
      public void peakListAdded(PeakList newPeakList) {
        if (newPeakList.getName().equals(ALIGNED_BLANK_NAME))
          setStatus(TaskStatus.PROCESSING);
      }

      @Override
      public void dataFileAdded(RawDataFile newFile) {}
    });

    // wait while the join aligner is processing
    while(getStatus() == TaskStatus.WAITING) {
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    
    
  }

  private JoinAlignerParameters createJoinAlignerParameters() {
    JoinAlignerParameters jp = new JoinAlignerParameters();
    jp.getParameter(JoinAlignerParameters.compareIsotopePattern).setValue(false);
    jp.getParameter(JoinAlignerParameters.compareSpectraSimilarity).setValue(false);
    jp.getParameter(JoinAlignerParameters.MZTolerance).setValue(mzTolerance);
    jp.getParameter(JoinAlignerParameters.RTTolerance).setValue(rtTolerance);
    jp.getParameter(JoinAlignerParameters.MZWeight).setValue(1.0);
    jp.getParameter(JoinAlignerParameters.RTWeight).setValue(1.0);
    jp.getParameter(JoinAlignerParameters.SameChargeRequired).setValue(false);
    jp.getParameter(JoinAlignerParameters.SameIDRequired).setValue(false);
    jp.getParameter(JoinAlignerParameters.peakLists).setValue(blankSelection);
    jp.getParameter(JoinAlignerParameters.peakListName).setValue(ALIGNED_BLANK_NAME);
    return jp;
  }

}
