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

package net.sf.mzmine.modules.peaklist.duplicatefilter;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class DuplicateFilterParameters extends SimpleParameterSet {

    public static final Parameter suffix = new SimpleParameter(
            ParameterType.STRING, "Name suffix",
            "Suffix to be added to peak list name", null, "Duplicate peaks filtered", null);

    public static final Parameter mzDifferenceMax = new SimpleParameter(
            ParameterType.FLOAT, "M/Z tolerance",
            "Maximum m/z difference between duplicate peaks",
            "m/z", new Float(0.05), new Float(0.0), null,
            MZmineCore.getDesktop().getMZFormat());

    public static final Parameter rtDifferenceMax = new SimpleParameter(
            ParameterType.FLOAT,
            "RT difference maximum",
            "Maximum retention time difference between duplicate peaks",
            "seconds", new Float(5.0), new Float(0.0), null,
            MZmineCore.getDesktop().getRTFormat());

    public static final Parameter requireSameIdentification = new SimpleParameter(
    		ParameterType.BOOLEAN, "Require same identification",
    		"If checked, duplicate peaks must have same identification(s)",
    		new Boolean(true));   
    
    public static final Parameter autoRemove = new SimpleParameter(
            ParameterType.BOOLEAN,
            "Remove original peaklist",
            "If checked, original peaklist will be removed and only deisotoped version remains",
            new Boolean(true));

    public DuplicateFilterParameters() {
        super(new Parameter[] { suffix, mzDifferenceMax,
                rtDifferenceMax, requireSameIdentification, autoRemove, });
    }

}
