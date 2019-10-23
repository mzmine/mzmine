/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.alignment.icpspeciation;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakAlignmentUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class IcpSpeciationAlignerTask extends AbstractTask {

  protected MZmineProject project;
  protected ParameterSet parameters;

  protected PeakList icpPeakList, mainPeakList;
  protected double peakShapeScore;

  protected int maxResults;

  protected RTTolerance rtTol;

  double totalCalculations;

  double doneCalculations;
  
  protected NumberFormat format;


  public IcpSpeciationAlignerTask(MZmineProject project, ParameterSet parameters) {
    this.project = project;
    this.parameters = parameters;

    this.icpPeakList = parameters.getParameter(IcpSpeciationAlignerParameters.icpPeakList)
        .getValue().getSpecificPeakLists()[0];

    this.mainPeakList = parameters.getParameter(IcpSpeciationAlignerParameters.moleculePeakList)
        .getValue().getSpecificPeakLists()[0];

    this.peakShapeScore = parameters
        .getParameter(IcpSpeciationAlignerParameters.peakShapeCorrelationScore).getValue();

    this.rtTol = parameters.getParameter(IcpSpeciationAlignerParameters.rtTolerance).getValue();

    this.maxResults = 5;
    
    format = new DecimalFormat("0.00");

    totalCalculations = icpPeakList.getNumberOfRows() * mainPeakList.getNumberOfRows();
    doneCalculations = 0;
  }

  @Override
  public String getTaskDescription() {
    return "Alignment of ICP and molecule-MS files.";
  }

  @Override
  public double getFinishedPercentage() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void run() {

    this.setStatus(TaskStatus.PROCESSING);

    int nRows = mainPeakList.getNumberOfRows();

    PeakListRow[] mainRows = mainPeakList.getRows();

    PeakListRowSorter sorter =
        new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending);
    Arrays.sort(mainRows, sorter);



    for (int i = 0; i < icpPeakList.getNumberOfRows(); i++) {
      List<PeakListRow> rows = new ArrayList<PeakListRow>();
      PeakListRow icpRow = icpPeakList.getRow(i);
      for (int j = 0; j < nRows; j++) {
        if (!rtTol.checkWithinTolerance(icpRow.getAverageRT(), mainRows[j].getAverageRT()))
          continue;

        double score = 0.0d;
        score = PeakAlignmentUtils.getPeakShapeScore(icpPeakList.getRow(i), mainRows[j]);
        if (score > peakShapeScore) {
          PeakListRow result = copyPeakRow(mainRows[j]);
          result.setComment("Peak shape score: " + format.format(score));
          rows.add(result);
        }
        doneCalculations++;
      }

      RawDataFile[] mainRaws = mainPeakList.getRawDataFiles();
      RawDataFile[] icpRaws = icpPeakList.getRawDataFiles();
      RawDataFile[] fusedRaws = new RawDataFile[mainRaws.length + icpRaws.length];
      int counter = 0;
      for (RawDataFile f : mainRaws) {
        fusedRaws[counter] = f;
        counter++;
      }
      for (RawDataFile f : icpRaws) {
        fusedRaws[counter] = f;
        counter++;
      }

      PeakList result =
          new SimplePeakList("Alignment " + mainPeakList.getName() + " & " + icpPeakList.getName()
              + "(" + ((int) icpPeakList.getRow(i).getAverageMZ()) + ")", fusedRaws);

      PeakListRow newIcpRow = copyPeakRow(icpPeakList.getRow(i));
      icpRow.setComment("Icp peak");
      result.addRow(newIcpRow);
      for (PeakListRow row : rows) {
        result.addRow(row);
      }
      project.addPeakList(result);
    }
    setStatus(TaskStatus.FINISHED);
  }

  public PeakListRow[] getICPRows(PeakList peakList) {

    return peakList.getRows();
  }

  /**
   * Create a copy of a feature list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static PeakListRow copyPeakRow(final PeakListRow row) {
    // Copy the feature list row.
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
}
