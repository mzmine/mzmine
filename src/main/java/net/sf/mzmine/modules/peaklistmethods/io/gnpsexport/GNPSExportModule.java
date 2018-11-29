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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.awt.Desktop;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.export.ExportCorrAnnotationTask;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.CSVExportTask;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.ExportRowCommonElement;
import net.sf.mzmine.modules.peaklistmethods.io.csvexport.ExportRowDataFileElement;
import net.sf.mzmine.modules.peaklistmethods.io.gnpsexport.GNPSExportParameters.RowFilter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.AllTasksFinishedListener;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.DialogLoggerUtil;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Exports all files needed for GNPS feature based molecular networking (quant table (csv export)),
 * MS2 mgf, additional edges (ion identity networks)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSExportModule implements MZmineProcessingModule {
  private final Logger LOG = Logger.getLogger(getClass().getName());
  /**
   * This website is still the beta. TODO change later to the final
   */
  public static final String GNPS_WEBSITE =
      "http://mingwangbeta.ucsd.edu:5050/featurebasednetworking";

  private static final String MODULE_NAME = "Export for/Submit to GNPS";
  private static final String MODULE_DESCRIPTION =
      "Exports the MGF file for GNPS (only for MS/MS), the quant table (CSV export) and additional edges (ion identity networks and correlation)";

  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(MZmineProject project, ParameterSet parameters,
      Collection<Task> tasks) {
    // add gnps export task

    boolean openFolder = parameters.getParameter(GNPSExportParameters.OPEN_FOLDER).getValue();
    boolean submit = parameters.getParameter(GNPSExportParameters.SUBMIT).getValue();
    File file = parameters.getParameter(GNPSExportParameters.FILENAME).getValue();
    file = FileAndPathUtil.eraseFormat(file);
    parameters.getParameter(GNPSExportParameters.FILENAME).setValue(file);


    List<AbstractTask> list = new ArrayList<>(3);
    GNPSExportTask task = new GNPSExportTask(parameters);
    tasks.add(task);
    list.add(task);

    // add csv quant table
    list.add(addQuantTableTask(parameters, tasks));

    // add csv extra edges
    list.add(addExtraEdgesTask(parameters, tasks));

    // finish listener to submit
    if (submit || openFolder) {
      final File fileName = file;
      final File folder = file.getParentFile();
      new AllTasksFinishedListener(list, true, l -> {
        // succeed
        if (submit) {
          GNPSSubmitParameters param =
              parameters.getParameter(GNPSExportParameters.SUBMIT).getEmbeddedParameters();
          submit(fileName, param);
        }

        // open?
        try {
          if (Desktop.isDesktopSupported()) {
            if (openFolder)
              Desktop.getDesktop().open(folder);
          }
        } catch (Exception ex) {
        }
      }, lerror -> {
        // TODO show error
        DialogLoggerUtil.showErrorDialog(null, "GNPS submit was not started",
            "File export was not complete");
        LOG.log(Level.WARNING, "GNPS submit was not started due too errors while file export");
      });
    }

    return ExitCode.OK;
  }

  private void submit(File fileName, GNPSSubmitParameters param) {
    try {
      String url = GNPSUtils.submitJob(fileName, param);
      if (url == null || url.isEmpty())
        LOG.log(Level.WARNING, "GNPS submit failed (url empty)");
    } catch (Exception e) {
      LOG.log(Level.WARNING, "GNPS submit failed", e);
    }
  }

  /**
   * Export quant table
   * 
   * @param parameters
   * @param tasks
   */
  private AbstractTask addQuantTableTask(ParameterSet parameters, Collection<Task> tasks) {
    File full = parameters.getParameter(GNPSExportParameters.FILENAME).getValue();
    String name = FileAndPathUtil.eraseFormat(full.getName());
    full = FileAndPathUtil.getRealFilePath(full.getParentFile(), name + "_quant", "csv");

    ExportRowCommonElement[] common = new ExportRowCommonElement[] {ExportRowCommonElement.ROW_ID,
        ExportRowCommonElement.ROW_MZ, ExportRowCommonElement.ROW_RT,
        ExportRowCommonElement.ROW_CORR_GROUP_ID, ExportRowCommonElement.ROW_MOL_NETWORK_ID,
        ExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT,
        ExportRowCommonElement.ROW_NEUTRAL_MASS};

    ExportRowDataFileElement[] rawdata =
        new ExportRowDataFileElement[] {ExportRowDataFileElement.PEAK_AREA};

    RowFilter filter = parameters.getParameter(GNPSExportParameters.FILTER).getValue();

    CSVExportTask quanExport = new CSVExportTask(
        parameters.getParameter(GNPSExportParameters.PEAK_LISTS).getValue().getMatchingPeakLists(), //
        full, ",", common, rawdata, false, ";", filter);
    tasks.add(quanExport);
    return quanExport;
  }

  /**
   * Export extra edges (wont create files if empty)
   * 
   * @param parameters
   * @param tasks
   */
  private AbstractTask addExtraEdgesTask(ParameterSet parameters, Collection<Task> tasks) {
    File full = parameters.getParameter(GNPSExportParameters.FILENAME).getValue();
    RowFilter filter = parameters.getParameter(GNPSExportParameters.FILTER).getValue();

    boolean exAnn = true;
    if (parameters.getParameter(GNPSExportParameters.SUBMIT).getValue()) {
      exAnn = parameters.getParameter(GNPSExportParameters.SUBMIT).getEmbeddedParameters()
          .getParameter(GNPSSubmitParameters.ANN_EDGES).getValue();
    }

    AbstractTask extraEdgeExport = new ExportCorrAnnotationTask(
        parameters.getParameter(GNPSExportParameters.PEAK_LISTS).getValue()
            .getMatchingPeakLists()[0], //
        full, 0, filter, exAnn, false);
    tasks.add(extraEdgeExport);
    return extraEdgeExport;
  }

  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.PEAKLISTEXPORT;
  }

  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return GNPSExportParameters.class;
  }

}

/*
 * GNPS: "If you use the GNPS export module (http://gnps.ucsd.edu), cite MZmine2 and the following
 * article: Wang et al., Nature Biotechnology 34.8 (2016): 828-837. [LINK]
 * (https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.htm)
 * 
 * See documentation about MZmine2 data pre-processing
 * [https://bix-lab.ucsd.edu/display/Public/Mass+spectrometry+data+pre-processing+for+GNPS] for GNPS
 * (http://gnps.ucsd.edu)
 */
