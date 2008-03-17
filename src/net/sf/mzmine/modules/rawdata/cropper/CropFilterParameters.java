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

package net.sf.mzmine.modules.rawdata.cropper;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class CropFilterParameters extends SimpleParameterSet {

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Filename suffix",
            "Suffix to be added to filename", null, "cropped", null);

    public static final Parameter minMZ = new SimpleParameter(
            ParameterType.FLOAT, "Minimum M/Z",
            "Lower m/z boundary of the cropped region", "m/z",
            new Float(100.0), new Float(0.0), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter maxMZ = new SimpleParameter(
            ParameterType.FLOAT, "Maximum M/Z",
            "Upper m/z boundary of the cropped region", "m/z",
            new Float(1000.0), new Float(0.0), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter minRT = new SimpleParameter(
            ParameterType.FLOAT, "Minimum retention time",
            "Lower retention time boundary of the cropped region", null,
            new Float(0.0), new Float(0.0), null,
            MZmineCore.getDesktop().getRTFormat());

    public static final Parameter maxRT = new SimpleParameter(
            ParameterType.FLOAT, "Maximum retention time",
            "Upper retention time boundary of the cropped region", null,
            new Float(600.0), new Float(0.0), null,
            MZmineCore.getDesktop().getRTFormat());

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove source file after filtering",
            "If checked, original file will be removed and only filtered version remains",
            new Boolean(true));

    public CropFilterParameters() {
        super(new Parameter[] { suffix, minMZ, maxMZ, minRT, maxRT,
                        autoRemove });
    }

}
