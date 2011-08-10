/*
 * Copyright 2006-2011 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.modules.peaklistmethods.dataanalysis.heatmaps;

import java.text.NumberFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.NumberParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;

public class HeatMapParameters extends SimpleParameterSet {

        public static final String[] fileTypes = {"pdf", "svg", "png", "fig"};
        public static final FileNameParameter fileName = new FileNameParameter(
                "Output name",
                "Select the path and name of the output file.");
        public static final ComboParameter<String> fileTypeSelection = new ComboParameter<String>(
                "Output file type", "Output file type",
                fileTypes, fileTypes[0]);
        public static final ParameterSelection selectionData = new ParameterSelection();
        public static final StringParameter referenceGroup = new StringParameter(
                "Group of reference", "Name of the group that will be used as a reference from the sample parametes.",
                "");
        public static final BooleanParameter useIdenfiedRows = new BooleanParameter(
                "Only identified rows",
                "Plot only identified rows.", false);
        public static final BooleanParameter usePeakArea = new BooleanParameter(
                "Use peak area",
                "Use peak area.", true);
        public static final BooleanParameter scale = new BooleanParameter(
                "Scaling",
                "Scaling the data with the standard deviation of each colum.", true);
        public static final BooleanParameter log = new BooleanParameter(
                "Log",
                "Log scaling of the data", true);
        public static final BooleanParameter plegend = new BooleanParameter(
                "P-value legend",
                "Adds the p-value legend", true);
        public static final NumberParameter star = new NumberParameter(
                "Size p-value legend",
                "Size of the p-value legend", NumberFormat.getIntegerInstance(), 5);
        public static final BooleanParameter showControlSamples = new BooleanParameter(
                "Show control samples",
                "Shows control samples if this option is selected", true);
        public static final NumberParameter height = new NumberParameter(
                "Height",
                "Height", NumberFormat.getIntegerInstance(), 10);
        public static final NumberParameter width = new NumberParameter(
                "Width",
                "Width", NumberFormat.getIntegerInstance(), 10);
        public static final NumberParameter columnMargin = new NumberParameter(
                "Column margin",
                "Column margin", NumberFormat.getIntegerInstance(), 10);
        public static final NumberParameter rowMargin = new NumberParameter(
                "Row margin",
                "Row margin", NumberFormat.getIntegerInstance(), 10);

        public HeatMapParameters() {
                super(new Parameter[]{fileName, fileTypeSelection, selectionData, referenceGroup, useIdenfiedRows, usePeakArea, scale, log, showControlSamples, plegend, star, height, width, columnMargin, rowMargin});
        }
}
