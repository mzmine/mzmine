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
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.util.ExitCode;

import java.awt.*;


public class GNPSExportAndSubmitParameters extends SimpleParameterSet {

  /**
   * Define which rows to export
   * 
   * @author Robin Schmid (robinschmid@uni-muenster.de)
   *
   */
  public enum RowFilter {
  ALL, ONLY_WITH_MS2;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }

    /**
     * Filter a row
     * 
     * @param row
     * @return
     */
    public boolean filter(PeakListRow row) {
      switch (this) {
        case ALL:
          return true;
        case ONLY_WITH_MS2:
          return row.getBestFragmentation() != null;
      }
      return false;
    }
  }

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the output MGF file. "
          + "Use pattern \"{}\" in the file name to substitute with peak list name. "
          + "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). "
          + "If the file already exists, it will be overwritten.",
      "mgf");

  public static final MassListParameter MASS_LIST = new MassListParameter();

  public static final OptionalModuleParameter<GNPSSubmitParameters> SUBMIT =
      new OptionalModuleParameter<GNPSSubmitParameters>("Submit to GNPS",
          "Directly submits a GNPS job", new GNPSSubmitParameters());

  public static final ComboParameter<RowFilter> FILTER = new ComboParameter<RowFilter>(
      "Filter rows", "Limit the exported rows to those with MS/MS data or annotated rows",
      RowFilter.values(), RowFilter.ONLY_WITH_MS2);


  // public static final BooleanParameter OPEN_GNPS = new BooleanParameter("Open GNPS website",
  // "Opens the super quick start of GNPS feature based networking in the standard browser.",
  // false);

  public static final BooleanParameter OPEN_FOLDER =
      new BooleanParameter("Open folder", "Opens the export folder", false);


  public static final OptionalModuleParameter<MsMsSpectraMergeParameters> MERGE_PARAMETER =
      new OptionalModuleParameter<>("Merge MS/MS (experimental)",
          "Merge high-quality MS/MS instead of exporting just the most intense one.",
          new MsMsSpectraMergeParameters(), true);


  public GNPSExportAndSubmitParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, MASS_LIST, MERGE_PARAMETER, FILTER, SUBMIT,
        OPEN_FOLDER});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    String message = "<html>GNPS Module Disclaimer:<br>"
        + "<ul><li>If you use the GNPS export module for <a href=\"http://gnps.ucsd.edu/\">GNPS web-platform</a>, cite <a href=\"https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-11-395\">MZmine2 paper</a> and the following article:"
        + "<br><a href=\"https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.html\">Wang et al., Nature Biotechnology 34.8 (2016): 828-837</a>."
        + "<li><a href=\"https://bix-lab.ucsd.edu/display/Public/GNPS+data+analysis+workflow+2.0\">See the documentation</a> about MZmine 2 data pre-processing for <a href=\"http://gnps.ucsd.edu/\">GNPS</a> molecular "
        + "networking and MS/MS spectral library search." + "</ul>";

    ParameterSetupDialog dialog =
        new ParameterSetupDialog(parent, valueCheckRequired, this, message);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
