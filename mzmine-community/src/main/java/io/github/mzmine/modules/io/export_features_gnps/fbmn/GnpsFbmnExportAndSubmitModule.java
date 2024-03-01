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
 * 2018-Nov: Changes by Robin Schmid - Direct submit
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.export_features_gnps.fbmn;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * Exports all files needed for GNPS feature based molecular networking (quant table (csv export)),
 * MS2 mgf, additional edges (ion identity networks)
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class GnpsFbmnExportAndSubmitModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Export molecular networking files (e.g., GNPS, FBMN, IIMN, MetGem)";
  private static final String MODULE_DESCRIPTION = "GNPS feature-based molecular networking export and submit module. Exports the MGF file for GNPS (only for MS/MS), the quant table (CSV export) and additional edges (ion identity networks and correlation)";

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    // add gnps export task
    GnpsFbmnExportAndSubmitTask task = new GnpsFbmnExportAndSubmitTask(parameters, moduleCallDate);
    tasks.add(task);

    return ExitCode.OK;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTEXPORT;
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return GnpsFbmnExportAndSubmitParameters.class;
  }

}