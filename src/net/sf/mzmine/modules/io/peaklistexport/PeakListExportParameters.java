/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.io.peaklistexport;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class PeakListExportParameters extends SimpleParameterSet {

    public static final Parameter filename = new SimpleParameter(
            ParameterType.STRING,
            "Filename",
            "Name of exported peak list file name. If the file exists, it will be overwritten.");

    public static final Parameter fieldSeparator = new SimpleParameter(
            ParameterType.STRING, "Field separator",
            "Character(s) used to separate fields in the exported file",
            (Object) ",");

    public static final Parameter exportRowID = new SimpleParameter(
            ParameterType.BOOLEAN, "Export row ID",
            "Toggles exporting of row ID", true);

    public static final Parameter exportRowMZ = new SimpleParameter(
            ParameterType.BOOLEAN, "Export row m/z",
            "Toggles exporting of row average m/z", true);

    public static final Parameter exportRowRT = new SimpleParameter(
            ParameterType.BOOLEAN, "Export row retention time",
            "Toggles exporting of row averate retention time", true);

    public static final Parameter exportRowComment = new SimpleParameter(
            ParameterType.BOOLEAN, "Export row comment",
            "Toggles exporting of row's comment", true);

    public static final Parameter exportRowIdentity = new SimpleParameter(
            ParameterType.BOOLEAN, "Export row compound identity",
            "Toggles exporting of row compound identity", true);

    public static final Parameter exportRowNumberOfDetected = new SimpleParameter(
            ParameterType.BOOLEAN, "Export row number of detected peaks",
            "Toggles exporting of number of detected peaks on row", true);    
    
    public static final Parameter exportPeakStatus = new SimpleParameter(
            ParameterType.BOOLEAN, "Export peak status",
            "Toggles exporting of peak status in each file", true);

    public static final Parameter exportPeakMZ = new SimpleParameter(
            ParameterType.BOOLEAN, "Export peak m/z",
            "Toggles exporting of peak m/z in each file", true);

    public static final Parameter exportPeakRT = new SimpleParameter(
            ParameterType.BOOLEAN, "Export peak retention time",
            "Toggles exporting of peak retention time in each file", true);

    public static final Parameter exportPeakHeight = new SimpleParameter(
            ParameterType.BOOLEAN, "Export peak height",
            "Toggles exporting of peak height in each file", true);

    public static final Parameter exportPeakArea = new SimpleParameter(
            ParameterType.BOOLEAN, "Export peak area",
            "Toggles exporting of peak area in each file", true);

    public PeakListExportParameters() {
        super(new Parameter[] { filename, fieldSeparator, exportRowID,
                exportRowMZ, exportRowRT, exportRowComment, exportRowIdentity, exportRowNumberOfDetected, 
                exportPeakStatus, exportPeakMZ, exportPeakRT, exportPeakHeight,
                exportPeakArea });
    }

}
