/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction;

import java.awt.Component;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import org.jfree.data.gantt.Task;
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

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class PeakListBlankSubtractionMasterTask extends AbstractTask {

  private static final String ALIGNED_BLANK_NAME = "Aligned blank";

  private NumberFormat mzFormat, rtFormat;

  private static Logger logger = Logger.getLogger("PeakListBlankSubstractionTask");

  private MZmineProject project;
  private PeakListBlankSubtractionParameters parameters;

  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;

  private int minBlankDetections;

  private PeakListsSelection blankSelection;
  private PeakList[] blanks, target;
  private PeakList alignedBlank;

  private List<AbstractTask> subTasks;

  private enum BlankListType {
    ALIGNED, SELECTION
  };

  public enum SubtractionType {
    COMBINED, ALIGNED
  };

  public enum MatchingParameter {
    MZ, RT, RTRANGE, FOLDCHANGE
  };

  public SubtractionType subtractionType;

  public PeakListBlankSubtractionMasterTask(MZmineProject project,
      PeakListBlankSubtractionParameters parameters) {

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();

    this.project = project;
    this.parameters = parameters;

    this.mzTolerance =
        parameters.getParameter(PeakListBlankSubtractionParameters.mzTolerance).getValue();
    this.rtTolerance =
        parameters.getParameter(PeakListBlankSubtractionParameters.rtTolerance).getValue();

    this.blankSelection =
        parameters.getParameter(PeakListBlankSubtractionParameters.blankPeakLists).getValue();
    this.blanks = blankSelection.getMatchingPeakLists();
    this.target = parameters.getParameter(PeakListBlankSubtractionParameters.peakLists).getValue()
        .getMatchingPeakLists();

    this.subtractionType =
        parameters.getParameter(PeakListBlankSubtractionParameters.subtractionType).getValue();

    this.minBlankDetections =
        parameters.getParameter(PeakListBlankSubtractionParameters.minBlanks).getValue();

    subTasks = new ArrayList<>(target.length);
    
    setStatus(TaskStatus.WAITING);

    logger.setLevel(Level.FINEST);
  }

  @Override
  public String getTaskDescription() {
    return "Blank subtraction master task";
  }

  @Override
  public double getFinishedPercentage() {
    double perc = 0;
    for (AbstractTask t : subTasks) {
      perc += t.getFinishedPercentage();
    }
    return perc / subTasks.size();
  }

  @Override
  public void run() {

    alignedBlank = alignBlankPeakLists(blanks);

    if (alignedBlank == null) {
      setErrorMessage("Error while setting up the aligned blank peak list.");
      setStatus(TaskStatus.ERROR);
    }
    setStatus(TaskStatus.PROCESSING);

    logger.finest("Aligned blank peak list: " + alignedBlank.getName() + ". Contains "
        + alignedBlank.getNumberOfRawDataFiles() + " raw data files.");

    PeakListRow[] allowedBlankRows =
        getAllowedBlankRows(alignedBlank, subtractionType, minBlankDetections);


    // Create one task for every peak list
    for (PeakList peakList : target) {
      AbstractTask task =
          new PeakListBlankSubtractionSingleTask(project, parameters, peakList, allowedBlankRows);

      MZmineCore.getTaskController().addTask(task);
      subTasks.add(task);
    }

    setStatus(TaskStatus.FINISHED);

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
  private PeakListRow[] getAllowedBlankRows(@Nonnull PeakList aligned, SubtractionType type,
      int minDetectionsInBlank) {
    PeakListRow[] blankRows = PeakUtils.sortRowsMzAsc(alignedBlank.getRows());

    List<PeakListRow> results = new ArrayList<PeakListRow>();

    if (type == SubtractionType.COMBINED) {
      return blankRows;
    } else {
      for (PeakListRow row : blankRows) {
        if (row.getRawDataFiles().length > minDetectionsInBlank) {
          results.add(row);
        }
      }
    }
    logger.finest(
        "Feature list 'Aligned blank' contains " + aligned.getNumberOfRows() + " rows of which "
            + results.size() + " appear in more than " + minDetectionsInBlank + " raw data files.");
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
    // check what kind of peak lists the user passed to this module
    BlankListType blankType = getBlankType(blank);
    if (blankType == null)
      return null;

    JoinAlignerParameters param;
    String alignedPeakListName;
    PeakList aligned = null;

    if (blankType == BlankListType.SELECTION) {
      String options[] = {"Manual setup", "Standard parameters", "Cancel"};

      int answer = JOptionPane.showOptionDialog((Component) MZmineCore.getDesktop(),
          "Multiple peak lists were selected as blank. Would you like to set up the join aligner "
              + "manually or use standard parameters?",
          "Join aligner setup", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
          options, 1);

      if (answer == JOptionPane.YES_OPTION) {
        param = new JoinAlignerParameters();
        ExitCode code = param.showSetupDialog(null, true);
        if (code != ExitCode.OK) {
          logger.info("Join aligner setup was not exited via OK. Canceling.");
          setStatus(TaskStatus.CANCELED);
          return null;
        }
      } else if (answer == JOptionPane.NO_OPTION) {
        param = createDefaultSJoinAlignerParameters();
      } else {
        logger.warning("Multiple peak lists were selected, but join aligner was not set up.");
        setStatus(TaskStatus.CANCELED);
        return null;
      }

      alignedPeakListName = param.getParameter(JoinAlignerParameters.peakListName).getValue();

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
   * Determines what kind of peak lists the user passed as a blank. If it contains one aligned peak
   * list, ALIGNED that will be returned. If it is a selection of single-raw-data-file-peak-lists,
   * this will return SELECTION. If more than one peak list is selected and one of them is an
   * aligned one, this will return null.
   * 
   * @param blank The array of peak lists
   * @return SELECTION, ALIGNED or null
   */
  private @Nullable BlankListType getBlankType(@Nonnull PeakList[] blank) {
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
    return blankType;
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
