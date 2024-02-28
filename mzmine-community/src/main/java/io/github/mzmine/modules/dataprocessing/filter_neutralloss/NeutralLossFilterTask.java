/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_neutralloss;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.Candidate;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.Candidates;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.PeakListHandler;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.ResultBuffer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * This module will scan for neutral losses in a very similar way to IsotopePeakScanner.
 */
public class NeutralLossFilterTask extends AbstractTask {

  IIsotope[] el;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ParameterSet parameters;
  private double minRating;
  private String suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private String message;
  private double minHeight;
  private int totalRows, finishedRows;
  private String molecule;
  private ModularFeatureList resultPeakList;
  private MZmineProject project;
  private FeatureList peakList;
  private boolean checkRT;
  private double dMassLoss;
  private IMolecularFormula formula;

  NeutralLossFilterTask(MZmineProject project, FeatureList peakList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.parameters = parameters;
    this.project = project;
    this.peakList = peakList;

    mzTolerance = parameters.getParameter(NeutralLossFilterParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(NeutralLossFilterParameters.rtTolerance).getValue();
    minHeight = parameters.getParameter(NeutralLossFilterParameters.minHeight).getValue();
    molecule = parameters.getParameter(NeutralLossFilterParameters.molecule).getValue();
    dMassLoss = parameters.getParameter(NeutralLossFilterParameters.neutralLoss).getValue();
    suffix = parameters.getParameter(NeutralLossFilterParameters.suffix).getValue();
    checkRT = parameters.getParameter(NeutralLossFilterParameters.checkRT).getValue();

    // calc mass for molecule
    if (!molecule.isEmpty()) {
      formula = FormulaUtils.createMajorIsotopeMolFormula(molecule);
      if (formula != null) {
        dMassLoss = 0;
        for (IIsotope i : formula.isotopes()) {
          dMassLoss += i.getExactMass() * formula.getIsotopeCount(i);
        }
        logger.info("Mass of molecule: " + molecule + " = " + dMassLoss);
      }
    }

    message = "Got paramenters...";
  }

  public static double round(double value,
      int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
    if (places < 0) {
      throw new IllegalArgumentException();
    }

    BigDecimal bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.doubleValue();
  }

  /**
   * adds a comment to a PeakListRow without deleting the current comment
   *
   * @param row PeakListRow to add the comment to
   * @param str comment to be added
   */
  public static void addComment(FeatureListRow row, String str) { // maybe add
    // this to
    // PeakListRow
    // class?
    String current = row.getComment();
    if (current == null) {
      row.setComment(str);
    } else if (current.contains(str)) {
      return;
    } else {
      row.setComment(current + " " + str);
    }
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0.0;
    }
    return (double) finishedRows / (double) totalRows;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "NeutralLossFilter: " + message;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalRows = peakList.getNumberOfRows();

    ArrayList<Double> diff = setUpDiff();
    if (diff == null || Double.compare(dMassLoss, 0.0d) == 0) {
      setErrorMessage(
          "Could not set up neutral loss. Mass loss could not be calculated from the formula or is 0.0");
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (suffix.equals("auto")) {
      if (molecule.equals("")) {
        suffix = " NL: " + dMassLoss + " RTtol: " + rtTolerance.getTolerance() + "_results";
      } else {
        suffix = " NL (" + molecule + "): " + dMassLoss + " RTtol: " + rtTolerance.getTolerance()
                 + "_results";
      }
    }

    // get all rows and sort by m/z
    FeatureListRow[] rows = peakList.getRows().toArray(FeatureListRow[]::new);
    Arrays.sort(rows, new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    PeakListHandler plh = new PeakListHandler();
    plh.setUp(peakList);

    resultPeakList = new ModularFeatureList(peakList.getName() + " " + suffix, storage,
        peakList.getRawDataFiles());
    PeakListHandler resultMap = new PeakListHandler();

    for (int i = 0; i < totalRows; i++) {
      // i will represent the index of the row in peakList
      if (rows[i].getPeakIdentities().size() > 0) {
        finishedRows++;
        continue;
      }

      message = "Row " + i + "/" + totalRows;

      // now get all peaks that lie within RT and maxIsotopeMassRange:
      // pL[index].mz ->
      // pL[index].mz+maxMass
      ArrayList<FeatureListRow> groupedPeaks =
          groupPeaks(rows, i, diff.get(diff.size() - 1).doubleValue());

      if (groupedPeaks.size() < 2) {
        finishedRows++;
        continue;
      }

      ResultBuffer[] resultBuffer = new ResultBuffer[diff.size()]; // this
      // will
      // store
      // row
      // indexes
      // of
      // all
      // features
      // with
      // fitting
      // rt
      // and
      // mz
      for (int a = 0; a < diff.size(); a++) // resultBuffer[i] index will
      // represent Isotope[i] (if
      // numAtoms = 0)
      {
        resultBuffer[a] = new ResultBuffer(); // [0] will be the isotope
      }
      // with lowest mass#

      for (int j = 0; j < groupedPeaks.size(); j++) // go through all
      // possible peaks
      {
        for (int k = 0; k < diff.size(); k++) // check for each peak if
        // it is a possible
        // feature for
        // every diff[](isotope)
        { // this is necessary bc there might be more than one possible
          // feature
          // j represents the row index in groupedPeaks
          // k represents the isotope number the peak will be a
          // candidate for
          if (mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff.get(k),
              groupedPeaks.get(j).getAverageMZ())) {
            // this will automatically add groupedPeaks[0] to the
            // list -> isotope with
            // lowest mass
            resultBuffer[k].addFound(); // +1 result for isotope k
            resultBuffer[k].addRow(j); // row in groupedPeaks[]
            resultBuffer[k].addID(groupedPeaks.get(j).getID());
          }
        }
      }

      if (!checkIfAllTrue(resultBuffer)) // this means that for every
      // isotope we expected to find,
      // we found one or more possible
      // features
      {
        finishedRows++;
        continue;
      }

      Candidates candidates = new Candidates(diff.size(), minHeight, mzTolerance, plh);

      for (int k = 0; k < resultBuffer.length; k++) // reminder:
      // resultBuffer.length
      // = diff.size()
      {
        for (int l = 0; l < resultBuffer[k].getFoundCount(); l++) {
          // k represents index resultBuffer[k] and thereby the
          // isotope number
          // l represents the number of results in resultBuffer[k]

          candidates.get(k).checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l),
              diff.get(k), minRating);
        }
      }

      if (!checkIfAllTrue(candidates.getCandidates())) {
        finishedRows++;
        // logger.info("Not enough valid candidates for parent feature "
        // +
        // groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks
        // were found.") ;
        continue; // jump to next i
      }

      String comParent = "", comChild = "";

      ModularFeatureListRow originalChild = getRowFromCandidate(candidates, 0, plh);
      if (originalChild == null) {
        finishedRows++;
        continue;
      }
      FeatureListRow child = new ModularFeatureListRow(resultPeakList, originalChild.getID(),
          originalChild, true);

      if (resultMap.containsID(child.getID())) {
        comChild += resultMap.getRowByID(child.getID()).getComment();
      }

      comChild += "Parent ID: " + candidates.get(1).getCandID();
      addComment(child, comChild);

      List<FeatureListRow> rowBuffer = new ArrayList<FeatureListRow>();
      boolean allPeaksAddable = true;

      rowBuffer.add(child);

      for (int k = 1; k < candidates.size(); k++) // we skip k=0 because
      // == groupedPeaks[0]
      // which we
      // added before
      {
        ModularFeatureListRow originalParent = getRowFromCandidate(candidates, 1, plh);

        if (originalParent == null) {
          allPeaksAddable = false;
          continue;
        }

        FeatureListRow parent = new ModularFeatureListRow(resultPeakList, originalParent.getID(),
            originalParent, true);

        if (resultMap.containsID(parent.getID())) {
          comParent += resultMap.getRowByID(parent.getID()).getComment();
        }

        comParent += ("[--IS PARENT-- child ID: " + child.getID() + " ] | ");
        addComment(parent, comParent);

        addComment(child,
            " m/z shift(ppm): "
            + round(((parent.getAverageMZ() - child.getAverageMZ()) - diff.get(1))
                    / parent.getAverageMZ() * 1E6, 2)
            + " ");

        rowBuffer.add(parent);
      }

      if (allPeaksAddable) {
        for (FeatureListRow row : rowBuffer) {
          resultMap.addRow(row);
        }
      }

      if (isCanceled()) {
        return;
      }

      finishedRows++;
    }

    ArrayList<Integer> keys = resultMap.getAllKeys();
    for (int j = 0; j < keys.size(); j++) {
      resultPeakList.addRow(resultMap.getRowByID(keys.get(j)));
    }

    if (resultPeakList.getNumberOfRows() > 1) {
      addResultToProject(/* resultPeakList */);
    } else {
      message = "Element not found.";
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * @param b
   * @return true if every
   */
  private boolean checkIfAllTrue(ResultBuffer[] b) {
    for (int i = 0; i < b.length; i++) {
      if (b[i].getFoundCount() == 0) {
        return false;
      }
    }
    return true;
  }

  private boolean checkIfAllTrue(Candidate[] cs) {
    for (Candidate c : cs) {
      if (c.getRating() == 0) {
        return false;
      }
    }
    return true;
  }

  private ArrayList<Double> setUpDiff() {
    ArrayList<Double> diff = new ArrayList<Double>(2);

    diff.add(0.0);
    diff.add(dMassLoss);
    return diff;
  }

  /**
   * @param pL
   * @param parentIndex index of possible parent peak
   * @return will return ArrayList<PeakListRow> of all peaks within the range of pL[parentIndex].mz
   * -> pL[parentIndex].mz+maxMass
   */
  private ArrayList<FeatureListRow> groupPeaks(FeatureListRow[] pL, int parentIndex,
      double maxDiff) {
    ArrayList<FeatureListRow> buf = new ArrayList<FeatureListRow>();

    buf.add(pL[parentIndex]); // this means the result will contain
    // row(parentIndex) itself

    double mz = pL[parentIndex].getAverageMZ();
    float rt = pL[parentIndex].getAverageRT();

    for (int i = parentIndex + 1; i < pL.length; i++) // will not add the
    // parent peak itself
    {
      FeatureListRow r = pL[i];
      // check for rt

      if (r.getAverageHeight() < minHeight) {
        continue;
      }

      if (!rtTolerance.checkWithinTolerance(rt, r.getAverageRT()) && checkRT) {
        continue;
      }

      if (pL[i].getAverageMZ() > mz
          && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance())) {
        buf.add(pL[i]);
      }

      if (pL[i].getAverageMZ() > (mz + maxDiff)) // since pL is sorted by
      // ascending mass, we can
      // stop now
      {
        return buf;
      }
    }
    return buf;
  }

  /**
   * Add feature list to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addFeatureList(resultPeakList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("NeutralLossFilter",
            NeutralLossFilterModule.class, parameters, getModuleCallDate()));
  }

  /**
   * Extracts a feature list row from a Candidates array.
   *
   * @param candidates
   * @param peakIndex  the index of the candidate peak, the feature list row should be extracted
   *                   for.
   * @param plh
   * @return null if no peak with the given parameters exists, the specified feature list row
   * otherwise.
   */
  private @Nullable
  ModularFeatureListRow getRowFromCandidate(@NotNull Candidates candidates, int peakIndex,
      @NotNull PeakListHandler plh) {

    if (peakIndex >= candidates.size()) {
      return null;
    }

    Candidate cand = candidates.get(peakIndex);

    if (cand != null) {
      int id = cand.getCandID();
      FeatureListRow original = plh.getRowByID(id);
      return (ModularFeatureListRow) original;
    }
    return null;
  }
}
