/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.significance;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class SignificanceParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter(1, 1);

    public static final StringParameter controlGroupName = new StringParameter("Control Group ID",
            "All lists containing this name will be assigned to the control group");

    public static final StringParameter experimentalGroupName = new StringParameter("Experimental Group ID",
            "All lists containing this name will be assigned to the experimental group");

    public SignificanceParameters() {
        super(new Parameter[] {peakLists, experimentalGroupName, controlGroupName});
    }
}
