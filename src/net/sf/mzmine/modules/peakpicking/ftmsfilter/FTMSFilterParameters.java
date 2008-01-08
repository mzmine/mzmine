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

package net.sf.mzmine.modules.peakpicking.ftmsfilter;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

class FTMSFilterParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "FTMS filtered", null);

    public static final Parameter mzDifferenceMin = new SimpleParameter(
            ParameterType.FLOAT, "M/Z difference minimum",
            "Minimum m/z difference between real peak and shoulder peak",
            "m/z", new Float(0.001), new Float(0.0), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter mzDifferenceMax = new SimpleParameter(
            ParameterType.FLOAT, "M/Z difference maximum",
            "Maximum m/z difference between real peak and shoulder peak",
            "m/z", new Float(0.005), new Float(0.0), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter rtDifferenceMax = new SimpleParameter(
            ParameterType.FLOAT,
            "RT difference maximum",
            "Maximum retention time difference between real peak and shoulder peak",
            "seconds", new Float(5.0), new Float(0.0), null,
            MZmineCore.getDesktop().getRTFormat());

    public static final Parameter heightMax = new SimpleParameter(
            ParameterType.FLOAT, "Maximum height",
            "Maximum height of shoulder peak", "%", new Float(0.05), new Float(
                    0.0), null, percentFormat);

    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove source peaklist after deisotoping",
            "If checked, original peaklist will be removed and only deisotoped version remains",
            new Boolean(true));

    FTMSFilterParameters() {
        super(new Parameter[] { suffix, mzDifferenceMin, mzDifferenceMax,
                rtDifferenceMax, heightMax, autoRemove, });
    }

}
