package net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction.PeakListBlankSubtractionMasterTask.MatchingParameter;
import net.sf.mzmine.modules.peaklistmethods.filtering.blanksubtraction.PeakListBlankSubtractionMasterTask.SubtractionType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class PeakListBlankSubtractionSingleTask extends AbstractTask {

  private static final String ALIGNED_BLANK_NAME = "Aligned blank";

  private NumberFormat mzFormat, rtFormat, percFormat;

  private static Logger logger =
      Logger.getLogger(PeakListBlankSubtractionSingleTask.class.getName());

  private MZmineProject project;
  private PeakListBlankSubtractionParameters parameters;

  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private double foldChange;

  private PeakList peakList;
  private PeakListRow[] alignedBlank;

  private int finishedRows, totalRows;

  public SubtractionType subtractionType;

  public PeakListBlankSubtractionSingleTask(MZmineProject project,
      PeakListBlankSubtractionParameters parameters, PeakList peakList,
      PeakListRow[] alignedBlank) {

    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    percFormat = new DecimalFormat("0.0 %");

    this.project = project;
    this.parameters = parameters;

    this.mzTolerance =
        parameters.getParameter(PeakListBlankSubtractionParameters.mzTolerance).getValue();
    this.rtTolerance =
        parameters.getParameter(PeakListBlankSubtractionParameters.rtTolerance).getValue();
    this.subtractionType =
        parameters.getParameter(PeakListBlankSubtractionParameters.subtractionType).getValue();
    this.foldChange =
        parameters.getParameter(PeakListBlankSubtractionParameters.foldChange).getValue();

    this.alignedBlank = alignedBlank;
    this.peakList = peakList;

    finishedRows = 0;
    totalRows = peakList.getNumberOfRows();

  }

  @Override
  public String getTaskDescription() {
    return "Blank subtraction for peak list " + peakList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return finishedRows / totalRows;
  }

  @Override
  public void run() {

    MatchingParameter[] matchingParameters = new MatchingParameter[] {MatchingParameter.MZ,
        MatchingParameter.RT, MatchingParameter.RTRANGE, MatchingParameter.FOLDCHANGE};

    PeakListRow[] rows = PeakUtils.sortRowsMzAsc(peakList.getRows());

    PeakList results =
        new SimplePeakList(peakList.getName() + " sbtrctd", peakList.getRawDataFile(0));

    for (PeakListRow targetRow : rows) {
      finishedRows++;
      if (targetRow == null)
        continue;

      MatchResult match = getBestMachtingRow(targetRow, alignedBlank, matchingParameters);
      if (match == null || match.getScore() < 0.7) {
        results.addRow(PeakUtils.copyPeakRow(targetRow));
      }

      if (getStatus() == TaskStatus.CANCELED)
        break;
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
          return "Subtracts a blank measurements peak list from another peak list.";
        }
      });
      project.addPeakList(results);

      logger.info("Processing of feature list " + peakList.getName() + " finished. "
          + (peakList.getNumberOfRows() - results.getNumberOfRows()) + " features removed.");

    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Calculates a matching score for a peak list row, with a value between 0 and 1. TODO: more
   * matching parameters
   * 
   * @param target
   * @param list An array of peak list rows to match the target row with. Has to be sorted by
   *        ascending m/z.
   * @param mp An array of the matching parameters to use.
   * @return The best matching row or null.
   */
  private @Nullable MatchResult getBestMachtingRow(@Nonnull PeakListRow row,
      @Nonnull PeakListRow[] list, @Nonnull MatchingParameter[] mp) {
    double bestScore = 0.0;
    PeakListRow bestMatch = null;
    for (PeakListRow blankRow : list) {

      // the rows are sorted by ascending mz, if the current mz of aRow is higher than the
      // tolerance, we can stop here
      if (mzTolerance.checkWithinTolerance(row.getAverageMZ(),
          blankRow.getAverageMZ() - mzTolerance.getMzToleranceForMass(blankRow.getAverageMZ())))
        break;

      if (!mzTolerance.checkWithinTolerance(row.getAverageMZ(), blankRow.getAverageMZ())
          || !rtTolerance.checkWithinTolerance(row.getAverageRT(), blankRow.getAverageRT()))
        continue;

      double score = getSimpleRowVsAlignedRowScores(row, blankRow, mp);

      if (score > bestScore) {
        bestMatch = blankRow;
        bestScore = score;
      }
      
    }

    if (bestMatch == null)
      return null;

    // we find the best match regarding all the other parameters first and then look at additional
    // parameters like the fold change
    for (MatchingParameter p : mp) {
      if (p == MatchingParameter.FOLDCHANGE) {
        if (row.getAverageArea() > (bestMatch.getAverageArea() * foldChange)) {
          logger.info(peakList.getName() + " m/z " + row.getAverageMZ()
              + " matches a blank row, but the intensity exceeds the fold change" 
              + (row.getAverageArea()/bestMatch.getAverageArea()));
          return null;
        }
      }
    }

    // logger.info("Best match for row " + target.getID() + " at m/z "
    // + mzFormat.format(target.getAverageMZ()) + " - " + " row " + bestMatch.getID() + " m/z "
    // + mzFormat.format(bestMatch.getAverageMZ()) + " rt "
    // + rtFormat.format(bestMatch.getAverageRT()) + " score " + rtFormat.format(bestScore));

    return new MatchResult(bestMatch, bestScore);
  }

  /**
   * Correlates the features by mz, rt and rt range.
   * @param peak
   * @param aligned
   * @param mp Matching parameters to look at.
   * @return A score between 1 and 0. 1 = best, 0 = worst
   */
  private double getSimpleRowVsAlignedRowScores(PeakListRow peak, PeakListRow aligned,
      MatchingParameter[] mp) {
    
    double score = 1;
    for (MatchingParameter p : mp) {
      if (p == MatchingParameter.MZ) {
        score *= compare(peak.getAverageMZ(), aligned.getAverageMZ());
      } else if (p == MatchingParameter.RT) {
        score *= compare(peak.getAverageRT(), aligned.getAverageRT());
      } else if (p == MatchingParameter.RTRANGE) {
        score *= scoreRtRange(peak.getBestPeak().getRawDataPointsRTRange(),
            PeakUtils.getPeakListRowAvgRtRange(aligned));
      }
    }
    return score;
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
}
