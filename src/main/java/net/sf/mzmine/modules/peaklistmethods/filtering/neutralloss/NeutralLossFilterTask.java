/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.filtering.neutralloss;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.Candidate;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.Candidates;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.PeakListHandler;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepeakscanner.ResultBuffer;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class NeutralLossFilterTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ParameterSet parameters;
  private Range<Double> massRange;
  private Range<Double> rtRange;
  private double minRating;
  private String element, suffix;
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  private String message;
  private double minHeight;
  private int totalRows, finishedRows;
  private String molecule;
  private PeakList resultPeakList;
  private MZmineProject project;
  private PeakList peakList;
  private boolean checkRT;

  public enum RatingType {
    HIGHEST, TEMPAVG
  };

  RatingType ratingType;
  private double dMassLoss;
  IIsotope[] el;

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  NeutralLossFilterTask(MZmineProject project, PeakList peakList, ParameterSet parameters) {
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

    message = "Got paramenters..."; // TODO
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return (double) finishedRows / (double) totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return message;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    totalRows = peakList.getNumberOfRows();

    ArrayList<Double> diff = setUpDiff();
    if (diff == null) {
      message = "ERROR: could not set up diff.";
      return;
    }
    if (suffix.equals("auto")) {
      if(molecule.equals(""))
          suffix = " NL: " + dMassLoss + " RTtol: " + rtTolerance.getTolerance() + "_results";
      else
        suffix = " NL (" + molecule + "): " + dMassLoss + " RTtol: " + rtTolerance.getTolerance() + "_results";
    }

    // get all rows and sort by m/z
    PeakListRow[] rows = peakList.getRows();
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    PeakListHandler plh = new PeakListHandler();
    plh.setUp(peakList);
    // totalRows = rows.length;

    resultPeakList = new SimplePeakList(peakList.getName() + suffix, peakList.getRawDataFiles());
    PeakListHandler resultMap = new PeakListHandler();

    for (int i = 0; i < totalRows; i++) {
      // i will represent the index of the row in peakList
      if (peakList.getRow(i).getPeakIdentities().length > 0
      /* || peakList.getRow(i).getRowCharge() != this.charge */) {
        // logger.info("Charge of row " + i + " is not " + charge + ". Charge of row " +
        // i + " is "
        // + peakList.getRow(i).getRowCharge());
        finishedRows++;
        continue;
      }

      message = "Row " + i + "/" + totalRows;
      massRange = mzTolerance.getToleranceRange(peakList.getRow(i).getAverageMZ());
      rtRange = rtTolerance.getToleranceRange(peakList.getRow(i).getAverageRT());

      // now get all peaks that lie within RT and maxIsotopeMassRange: pL[index].mz ->
      // pL[index].mz+maxMass
      ArrayList<PeakListRow> groupedPeaks =
          groupPeaks(rows, i, diff.get(diff.size() - 1).doubleValue());

      if (groupedPeaks.size() < 2) {
        finishedRows++;
        continue;
      }

      ResultBuffer[] resultBuffer = new ResultBuffer[diff.size()]; // this will store row indexes of
                                                                   // all features with fitting rt
                                                                   // and mz
      for (int a = 0; a < diff.size(); a++) // resultBuffer[i] index will represent Isotope[i] (if
                                            // numAtoms = 0)
        resultBuffer[a] = new ResultBuffer(); // [0] will be the isotope with lowest mass#

      for (int j = 0; j < groupedPeaks.size(); j++) // go through all possible peaks
      {
        for (int k = 0; k < diff.size(); k++) // check for each peak if it is a possible feature for
                                              // every diff[](isotope)
        { // this is necessary bc there might be more than one possible feature
          // j represents the row index in groupedPeaks
          // k represents the isotope number the peak will be a candidate for
          if (mzTolerance.checkWithinTolerance(groupedPeaks.get(0).getAverageMZ() + diff.get(k),
              groupedPeaks.get(j).getAverageMZ())) {
            // this will automatically add groupedPeaks[0] to the list -> isotope with
            // lowest mass
            resultBuffer[k].addFound(); // +1 result for isotope k
            resultBuffer[k].addRow(j); // row in groupedPeaks[]
            resultBuffer[k].addID(groupedPeaks.get(j).getID());
          }
        }
      }

      if (!checkIfAllTrue(resultBuffer)) // this means that for every isotope we expected to find,
                                         // we found one or more possible features
      {
        finishedRows++;
        continue;
      }

      Candidates candidates = new Candidates(diff.size(), minHeight, mzTolerance, plh);

      for (int k = 0; k < resultBuffer.length; k++) // reminder: resultBuffer.length = diff.size()
      {
        for (int l = 0; l < resultBuffer[k].getFoundCount(); l++) {
          // k represents index resultBuffer[k] and thereby the isotope number
          // l represents the number of results in resultBuffer[k]

          candidates.get(k).checkForBetterRating(groupedPeaks, 0, resultBuffer[k].getRow(l),
              diff.get(k), minRating);
        }
      }

      if (!checkIfAllTrue(candidates.getCandidates())) {
        finishedRows++;
        // logger.info("Not enough valid candidates for parent feature " +
        // groupedPeaks.get(0).getAverageMZ() + "\talthough enough peaks were found.") ;
        continue; // jump to next i
      }

      String comParent = "", comChild = "";
      PeakListRow parent = copyPeakRow(peakList.getRow(i));

      if (resultMap.containsID(parent.getID()))
        comParent += resultMap.getRowByID(parent.getID()).getComment();

      comParent += candidates.get(1).getCandID() + "<-ParentID";
      addComment(parent, comParent);

      resultMap.addRow(parent); // add results to resultPeakList

      for (int k = 1; k < candidates.size(); k++) // we skip k=0 because == groupedPeaks[0] which we
                                                  // added before
      {
        PeakListRow child = copyPeakRow(plh.getRowByID(candidates.get(k).getCandID()));
        // For neutral loss child and parent are
        // inverted. since child=higher m/z we set the
        // comments differently

        if (resultMap.containsID(child.getID()))
          comChild += resultMap.getRowByID(child.getID()).getComment();

        comChild += (/* child.getID() + */ "[--IS PARENT-- child ID: " + parent.getID() + " ] | ");
        addComment(child, comChild);

        addComment(parent,
            " m/z shift(ppm): "
                + round(((child.getAverageMZ() - parent.getAverageMZ()) - diff.get(1))
                    / child.getAverageMZ() * 1E6, 2) + " ");

        resultMap.addRow(child);
      }

      if (isCanceled())
        return;

      finishedRows++;
    }

    ArrayList<Integer> keys = resultMap.getAllKeys();
    for (int j = 0; j < keys.size(); j++)
      resultPeakList.addRow(resultMap.getRowByID(keys.get(j)));

    if (resultPeakList.getNumberOfRows() > 1)
      addResultToProject(/* resultPeakList */);
    else
      message = "Element not found.";
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * 
   * @param b
   * @return true if every
   */
  private boolean checkIfAllTrue(ResultBuffer[] b) {
    for (int i = 0; i < b.length; i++)
      if (b[i].getFoundCount() == 0)
        return false;
    return true;
  }

  private boolean checkIfAllTrue(Candidate[] cs) {
    for (Candidate c : cs)
      if (c.getRating() == 0)
        return false;
    return true;
  }

  private ArrayList<Double> setUpDiff() {
    ArrayList<Double> diff = new ArrayList<Double>(2);

    diff.add(0.0);

    if (!molecule.equals("")) {
      double diffBuffer = 0.0;

      IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
      IMolecularFormula formula =
          MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(molecule, builder);

      for (IIsotope iso : formula.isotopes()) {
        diffBuffer += iso.getExactMass() * formula.getIsotopeCount(iso);
      }
      dMassLoss = diffBuffer;
      diff.add(dMassLoss);
      logger.info("Mass of molecule: " + molecule + " = " + diffBuffer);
    } else {
      diff.add(dMassLoss);
    }

    return diff;
  }

  /**
   * 
   * @param pL
   * @param parentIndex index of possible parent peak
   * @param maxMass
   * @return will return ArrayList<PeakListRow> of all peaks within the range of pL[parentIndex].mz
   *         -> pL[parentIndex].mz+maxMass
   */
  private ArrayList<PeakListRow> groupPeaks(PeakListRow[] pL, int parentIndex, double maxDiff) {
    ArrayList<PeakListRow> buf = new ArrayList<PeakListRow>();

    buf.add(pL[parentIndex]); // this means the result will contain row(parentIndex) itself

    double mz = pL[parentIndex].getAverageMZ();
    double rt = pL[parentIndex].getAverageRT();

    for (int i = parentIndex + 1; i < pL.length; i++) // will not add the parent peak itself
    {
      PeakListRow r = pL[i];
      // check for rt

      if (r.getAverageHeight() < minHeight)
        continue;

      if (!rtTolerance.checkWithinTolerance(rt, r.getAverageRT()) && checkRT)
        continue;

      if (pL[i].getAverageMZ() > mz
          && pL[i].getAverageMZ() <= (mz + maxDiff + mzTolerance.getMzTolerance())) {
        buf.add(pL[i]);
      }

      if (pL[i].getAverageMZ() > (mz + maxDiff)) // since pL is sorted by ascending mass, we can
                                                 // stop now
        return buf;
    }
    return buf;
  }

  /**
   * Create a copy of a peak list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static PeakListRow copyPeakRow(final PeakListRow row) {
    // Copy the peak list row.
    final PeakListRow newRow = new SimplePeakListRow(row.getID());
    PeakUtils.copyPeakListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getPeaks()) {
      final Feature newPeak = new SimpleFeature(peak);
      PeakUtils.copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getDataFile(), newPeak);
    }

    return newRow;
  }

  public static double round(double value, int places) { // https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
    if (places < 0)
      throw new IllegalArgumentException();

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
  public static void addComment(PeakListRow row, String str) {
    String current = row.getComment();
    if (current == null)
      row.setComment(str);
    else if (current.contains(str))
      return;
    else
      row.setComment(current + " " + str);
  }

  /**
   * Add peak list to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("IsotopePeakScanner", parameters));
  }
}
