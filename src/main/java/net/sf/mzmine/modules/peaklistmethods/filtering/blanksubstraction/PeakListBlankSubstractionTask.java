package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubstraction;

import java.awt.Component;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MZmineProjectListener;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.join.JoinAlignerParameters;
import net.sf.mzmine.modules.peaklistmethods.alignment.join.JoinAlignerTask;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class PeakListBlankSubstractionTask extends AbstractTask {

  private static final String ALIGNED_BLANK_NAME = "Aligned blank";

  private NumberFormat mzFormat, rtFormat;

  private static Logger logger = Logger.getLogger("PeakListBlankSubstractionTask");

  private MZmineProject project;
  private PeakListBlankSubstractionParameters parameters;

  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;

  private int minBlankDetections;

  private PeakListsSelection blankSelection;
  private PeakList[] blanks, target;
  private PeakList alignedBlank;

  private int finishedRows, totalRows;

  private String message;

  private enum BlankListType {
    ALIGNED, SELECTION
  };

  public enum SubstractionType {
    COMBINED, ALIGNED
  };

  public enum MatchingParameter {
    MZ, RT, RTRANGE
  };

  public SubstractionType substractionType;

  public PeakListBlankSubstractionTask(MZmineProject project,
      PeakListBlankSubstractionParameters parameters) {

    message = "Initializing...";

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();

    this.project = project;
    this.parameters = parameters;

    this.mzTolerance =
        parameters.getParameter(PeakListBlankSubstractionParameters.mzTolerance).getValue();
    this.rtTolerance =
        parameters.getParameter(PeakListBlankSubstractionParameters.rtTolerance).getValue();

    this.blankSelection =
        parameters.getParameter(PeakListBlankSubstractionParameters.blankPeakLists).getValue();
    this.blanks = blankSelection.getMatchingPeakLists();
    this.target = parameters.getParameter(PeakListBlankSubstractionParameters.peakLists).getValue()
        .getMatchingPeakLists();

    this.substractionType =
        parameters.getParameter(PeakListBlankSubstractionParameters.substractionType).getValue();

    this.minBlankDetections =
        parameters.getParameter(PeakListBlankSubstractionParameters.minBlanks).getValue();

    for (PeakList peakList : target) {
      totalRows += peakList.getNumberOfRows();
    }
    logger.info("totalRows: " + totalRows);
    finishedRows = 0;

  }

  @Override
  public String getTaskDescription() {
    return "Blank susbtraction task";
  }

  @Override
  public double getFinishedPercentage() {
    return finishedRows / totalRows;
  }

  @Override
  public void run() {

    alignedBlank = alignBlankPeakLists(blanks);

    if (alignedBlank == null) {
      setErrorMessage("Error while setting up the aligned blank peak list.");
      setStatus(TaskStatus.ERROR);
    }
    setStatus(TaskStatus.PROCESSING);

    logger.fine("Aligned blank peak list: " + alignedBlank.getName() + ". Contains "
        + alignedBlank.getNumberOfRawDataFiles() + " raw data files.");

    PeakListRow[] allowedBlankRows =
        getAllowedBlankRows(alignedBlank, substractionType, minBlankDetections);

    MatchingParameter[] matchingParameters = new MatchingParameter[] {MatchingParameter.MZ,
        MatchingParameter.RT, MatchingParameter.RTRANGE};

    for (PeakList peakList : target) {
      PeakListRow[] rows = PeakUtils.sortRowsMzAsc(peakList.getRows());

      PeakList results =
          new SimplePeakList(peakList.getName() + " substracted", peakList.getRawDataFile(0));

      for (PeakListRow target : rows) {
        finishedRows++;
        if (target == null)
          continue;

        MatchResult match = getBestMachtingRow(target, allowedBlankRows, matchingParameters);
        if (match == null || match.getScore() < 0.7) {
          results.addRow(PeakUtils.copyPeakRow(target));
        }
      }

      if (results.getNumberOfRows() > 0) {
        PeakListAppliedMethod[] methods = peakList.getAppliedMethods();
        for (PeakListAppliedMethod m : methods)
          results.addDescriptionOfAppliedTask(m);
        results.addDescriptionOfAppliedTask(new PeakListAppliedMethod() {

          @Override
          public String getParameters() {
            return parameters.toString();
          }

          @Override
          public String getDescription() {
            return "Substracts a blank measurements peak list from another peak list.";
          }
        });
        project.addPeakList(results);

        logger.fine("Processing of feature list " + peakList.getName() + " finished. "
            + (peakList.getNumberOfRows() - results.getNumberOfRows()) + " features removed.");

      }
    }

    setStatus(TaskStatus.FINISHED);

  }

  /**
   * Calculates a matching score for a peak list row, with a value between 0 and 1. 
   * TODO: more matching parameters
   * 
   * @param target
   * @param list An array of peak list rows to match the target row with. Has to be sorted by
   *        ascending m/z.
   * @param mp An array of the matching parameters to use.
   * @return The best matching row or null.
   */
  private @Nullable MatchResult getBestMachtingRow(@Nonnull PeakListRow target,
      @Nonnull PeakListRow[] list, @Nonnull MatchingParameter[] mp) {
    double bestScore = 0.0;
    PeakListRow bestMatch = null;
    for (PeakListRow aRow : list) {
      double score = 1;

      // the rows are sorted by ascending mz, if the current mz of aRow is higher than the
      // tolerance, we can stop here
      if (mzTolerance.checkWithinTolerance(target.getAverageMZ(),
          aRow.getAverageMZ() - mzTolerance.getMzToleranceForMass(aRow.getAverageMZ())))
        break;

      if (!mzTolerance.checkWithinTolerance(target.getAverageMZ(), aRow.getAverageMZ())
          || !rtTolerance.checkWithinTolerance(target.getAverageRT(), aRow.getAverageRT()))
        continue;

      for (MatchingParameter p : mp) {
        if (p == MatchingParameter.MZ) {
          score *= compare(target.getAverageMZ(), aRow.getAverageMZ());
        } else if (p == MatchingParameter.RT) {
          score *= compare(target.getAverageRT(), aRow.getAverageRT());
        } else if (p == MatchingParameter.RTRANGE) {
          score *= scoreRtRange(target.getBestPeak().getRawDataPointsRTRange(),
              PeakUtils.getPeakListRowAvgRtRange(aRow));
        }
      }

      if (score > bestScore) {
        bestMatch = aRow;
        bestScore = score;
      }
    }

    if (bestMatch == null) {
//      logger.info("No match for m/z " + mzFormat.format(target.getAverageMZ()));
      return null;
    }
//    logger.info("Best match for row " + target.getID() + " at m/z "
//        + mzFormat.format(target.getAverageMZ()) + " - " + " row " + bestMatch.getID() + " m/z "
//        + mzFormat.format(bestMatch.getAverageMZ()) + " rt "
//        + rtFormat.format(bestMatch.getAverageRT()) + " score " + rtFormat.format(bestScore));

    return new MatchResult(bestMatch, bestScore);
  }

  /**
   * Returns a the deviation of mz1 from mz2.
   * 
   * @param mz1 The actual number
   * @param mz2 The reference number
   * @return d1/d2
   */
  private double compare(double d1, double d2) {
    double d = (d1 / d2);
    if (d > 1)
      d = 1 / d;
    return d;
  }

  /**
   * Returns a deviation of the upper and lower boundary of two ranges.
   * 
   * @param r1
   * @param r2
   * @return
   */
  private double scoreRtRange(Range<Double> r1, Range<Double> r2) {

    double lower = compare(r1.lowerEndpoint(), r2.lowerEndpoint());
    double upper = compare(r1.upperEndpoint(), r2.upperEndpoint());

    return (lower + upper) / 2;
  }


  /**
   * Sorts the aligned peak list by ascending m/z and returns an array that only contains the
   * features that are contained the given number of feature lists.
   * 
   * @param aligned The aligned feature list.
   * @param type Subtraction type, specifies if every feature that appears in any of the raw data
   *        files or only ones with a given number of detections will be removed.
   * @param minDetectionsInBlank the minimum number of detections in.
   * @return Array of PeakListRows that match the criteria.
   */
  private PeakListRow[] getAllowedBlankRows(@Nonnull PeakList aligned, SubstractionType type,
      int minDetectionsInBlank) {
    PeakListRow[] blankRows = PeakUtils.sortRowsMzAsc(alignedBlank.getRows());

    List<PeakListRow> results = new ArrayList<PeakListRow>();

    if (type == SubstractionType.COMBINED) {
      return blankRows;
    } else {
      for (PeakListRow row : blankRows) {
        if (row.getRawDataFiles().length > minDetectionsInBlank) {
          results.add(row);
        }
      }
    }

    return results.toArray(new PeakListRow[0]);
  }

  /**
   * Checks, if the blank feature list(s) given to this task were a collection single feature lists
   * or an aligned feature list.
   * 
   * If it was a selection of feature lists: Sets up an aligned feature list of all blank files
   * given as a parameter to this task using the Join aligner. Lets the user choose, if he wants to
   * set the parameters himself manually or use standard parameters.
   * 
   * If it was a single aligned feature list, this will just return the aligned feature list.
   * 
   * @param blank
   * @return
   */
  private @Nullable PeakList alignBlankPeakLists(@Nonnull PeakList[] blank) {

    JoinAlignerParameters param;

    // check what kind of peak lists the user passed to this module
    BlankListType blankType;
    if (blanks.length == 1 && blanks[0].getNumberOfRawDataFiles() > 1) {
        blankType = BlankListType.ALIGNED;
    } else {
      blankType = BlankListType.SELECTION;
      
      // did the user maybe select an aligned feature list on top of normal feature lists?
      for (PeakList b : blanks) {
        if (b.getNumberOfRawDataFiles() > 1) {
          setErrorMessage("A combination of aligned feature lists and feature lists with just one "
              + "raw data file was selected. Select either one aligned or a combination of "
              + " unaligned feature lists.");
          return null;
        }
        
      }
    }

    String alignedPeakListName;
    PeakList aligned = null;

    if (blankType == BlankListType.SELECTION) {
      String options[] = {"Manual setup", "Standard parameters", "Cancel"};

      int answer = JOptionPane.showOptionDialog((Component) MZmineCore.getDesktop(),
          "Multiple peak lists were selected as blank. Would you like to set up the join aligner manually or use standard parameters?",
          "Join aligner setup", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
          options, 1);

      if (answer == JOptionPane.YES_OPTION) {
        param = new JoinAlignerParameters();
        ExitCode code = param.showSetupDialog(null, true);
        if (code != ExitCode.OK) {
          message = "Join aligner setup was not exited via OK. Canceling.";
          setStatus(TaskStatus.CANCELED);
          return null;
        }
      } else if (answer == JOptionPane.NO_OPTION) {
        param = createDefaultSJoinAlignerParameters();
      } else {
        message = "Multiple peak lists were selected, but join aligner was not set up.";
        logger.warning(message);
        setStatus(TaskStatus.CANCELED);
        return null;
      }

      alignedPeakListName = param.getParameter(JoinAlignerParameters.peakListName).getValue();

      message = "Waiting for Join Aligner.";
      setStatus(TaskStatus.WAITING);

      runJoinAligner(param);

      // wait while the join aligner is processing
      while (getStatus() == TaskStatus.WAITING) {
        try {
          TimeUnit.MILLISECONDS.sleep(50);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }

      for (PeakList pl : MZmineCore.getProjectManager().getCurrentProject().getPeakLists()) {
        if (pl.getName().equals(alignedPeakListName)) {
          aligned = pl;
        }
      }

    } else {
      aligned = blank[0];
    }

    return aligned;
  }

  /**
   * Creates a parameter set of default join aligner parameters.
   * 
   * @return Default parameter set.
   */
  private JoinAlignerParameters createDefaultSJoinAlignerParameters() {
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


  /**
   * Runs a join aligner task with the given sets of parameters.
   * 
   * @param jp Join aligner parameters.
   */
  private void runJoinAligner(JoinAlignerParameters jp) {

    project.addProjectListener(new MZmineProjectListener() {

      @Override
      public void peakListAdded(PeakList newPeakList) {
        if (newPeakList.getName()
            .equals(jp.getParameter(JoinAlignerParameters.peakListName).getValue()))
          setStatus(TaskStatus.PROCESSING);
      }

      @Override
      public void dataFileAdded(RawDataFile newFile) {}
    });

    JoinAlignerTask joinAlignerTask = new JoinAlignerTask(project, jp);

    MZmineCore.getTaskController().addTask(joinAlignerTask);
  }


}
