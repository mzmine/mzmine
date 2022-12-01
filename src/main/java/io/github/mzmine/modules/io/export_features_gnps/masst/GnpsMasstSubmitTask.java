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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_features_gnps.masst;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.abstr.UrlShortName;
import io.github.mzmine.datamodel.features.types.annotations.MasstUrlType;
import io.github.mzmine.modules.io.export_features_gnps.GNPSUtils;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.web.RequestResponse;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Submit MASST job to GNPS
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class GnpsMasstSubmitTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(GnpsMasstSubmitTask.class.getName());

  private final MasstDatabase database;
  private final String description;
  private final FeatureListRow row;
  private final double precursorMZ;
  private final DataPoint[] dataPoints;
  private final Double minCosine;
  private final double parentMzTol;
  private final double fragmentMzTol;
  private final int minMatchedSignals;
  private final boolean openWebsite;
  private final boolean searchAnalogs;
  private final String email;
  private final String password;
  private final String username;
  private final double progress = 0d;


  GnpsMasstSubmitTask(@Nullable FeatureListRow row, double precursorMZ, DataPoint[] dataPoints,
      ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.row = row;
    this.precursorMZ = precursorMZ;
    this.dataPoints = dataPoints;
    minCosine = parameters.getValue(GnpsMasstSubmitParameters.cosineScore);
    database = parameters.getValue(GnpsMasstSubmitParameters.database);
    description = parameters.getValue(GnpsMasstSubmitParameters.description);
    parentMzTol = parameters.getValue(GnpsMasstSubmitParameters.parentMassTolerance);
    fragmentMzTol = parameters.getValue(GnpsMasstSubmitParameters.fragmentMassTolerance);
    minMatchedSignals = parameters.getValue(GnpsMasstSubmitParameters.minimumMatchedSignals);
    searchAnalogs = parameters.getValue(GnpsMasstSubmitParameters.searchAnalogs);
    email = parameters.getValue(GnpsMasstSubmitParameters.email);
    username = parameters.getValue(GnpsMasstSubmitParameters.username);
    password = parameters.getValue(GnpsMasstSubmitParameters.password);
    openWebsite = parameters.getValue(GnpsMasstSubmitParameters.openWebsite);
  }

  @Override
  public String getTaskDescription() {
    return "Submitting single MASST job to GNPS";
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      RequestResponse response = GNPSUtils.submitMASSTJob(description, dataPoints, precursorMZ,
          database, minCosine, parentMzTol, fragmentMzTol, minMatchedSignals, searchAnalogs, email,
          username, password, openWebsite);

      String url = response.url();
      if (row != null && !url.isBlank()) {
        row.set(MasstUrlType.class, new UrlShortName(url, url));
      }
      if (!response.isSuccess()) {
        logger.log(Level.WARNING, "GNPS MASST submit failed");
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "GNPS MASST submit failed", e);
    }

    setStatus(TaskStatus.FINISHED);
  }

}
