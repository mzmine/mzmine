/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.id_fragmentsearch;

import java.util.Arrays;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.ScanUtils;

public class FragmentSearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int finishedRows, totalRows;
  private FeatureList peakList;

  private RTTolerance rtTolerance;
  private MZTolerance ms2mzTolerance;
  private double maxFragmentHeight, minMS2peakHeight;

  private ParameterSet parameters;

  /**
   * @param parameters
   * @param peakList
   */
  public FragmentSearchTask(ParameterSet parameters, FeatureList peakList) {
    super(null); // no new data stored -> null

    this.peakList = peakList;
    this.parameters = parameters;

    rtTolerance = parameters.getParameter(FragmentSearchParameters.rtTolerance).getValue();
    ms2mzTolerance = parameters.getParameter(FragmentSearchParameters.ms2mzTolerance).getValue();
    maxFragmentHeight =
        parameters.getParameter(FragmentSearchParameters.maxFragmentHeight).getValue();
    minMS2peakHeight =
        parameters.getParameter(FragmentSearchParameters.minMS2peakHeight).getValue();

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return ((double) finishedRows) / totalRows;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Identification of fragments in " + peakList;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting fragments search in " + peakList);

    FeatureListRow rows[] = peakList.getRows().toArray(FeatureListRow[]::new);
    totalRows = rows.length;

    // Start with the highest peaks
    Arrays.sort(rows,
        new FeatureListRowSorter(SortingProperty.Height, SortingDirection.Descending));

    // Compare each two rows against each other
    for (int i = 0; i < totalRows; i++) {

      for (int j = i + 1; j < rows.length; j++) {

        // Task canceled?
        if (isCanceled())
          return;

        // Treat the higher m/z peak as main peak and check if the
        // smaller one may be a fragment
        if (rows[i].getAverageMZ() > rows[j].getAverageMZ()) {
          if (checkFragment(rows[i], rows[j]))
            addFragmentInfo(rows[i], rows[j]);
        } else {
          if (checkFragment(rows[j], rows[i]))
            addFragmentInfo(rows[j], rows[i]);
        }

      }

      finishedRows++;

    }

    // Add task description to peakList
    ((ModularFeatureList) peakList).addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Identification of fragments",
            FragmentSearchModule.class, parameters));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished fragments search in " + peakList);

  }

  /**
   * Check if candidate peak may be a possible fragment of a given main peak
   *
   * @param mainPeak
   * @param possibleFragment
   */
  private boolean checkFragment(FeatureListRow mainPeak, FeatureListRow possibleFragment) {

    // Check retention time condition
    boolean rtCheck =
        rtTolerance.checkWithinTolerance(mainPeak.getAverageRT(), possibleFragment.getAverageRT());
    if (!rtCheck)
      return false;

    // Check height condition
    if (possibleFragment.getAverageHeight() > mainPeak.getAverageHeight() * maxFragmentHeight)
      return false;

    // Get MS/MS scan, if exists
    Scan fragmentScan = mainPeak.getBestFeature().getMostIntenseFragmentScan();
    if (fragmentScan == null)
      return false;

    if (fragmentScan == null)
      return false;

    // Get MS/MS data points in the tolerance range
    Range<Double> ms2mzRange = ms2mzTolerance.getToleranceRange(possibleFragment.getAverageMZ());

    DataPoint fragmentDataPoints[] =
        ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(fragmentScan), ms2mzRange);

    // If there is a MS/MS peak of required height, we have a hit
    for (DataPoint dp : fragmentDataPoints) {
      if (dp.getIntensity() > minMS2peakHeight)
        return true;
    }

    return false;

  }

  /**
   * Add new identity to the fragment row
   *
   * @param mainRow
   * @param fragmentRow
   */
  private void addFragmentInfo(FeatureListRow mainRow, FeatureListRow fragmentRow) {
    FragmentIdentity newIdentity = new FragmentIdentity(mainRow);
    fragmentRow.addFeatureIdentity(newIdentity, false);
  }

}
