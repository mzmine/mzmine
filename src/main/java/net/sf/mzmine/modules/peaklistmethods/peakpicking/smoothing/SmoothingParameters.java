/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.peakpicking.smoothing;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 * Defines smoothing task parameters.
 * 
 * @author $Author$
 * @version $Revision$
 */
public class SmoothingParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    /**
     * Raw data file suffix.
     */
    public static final StringParameter SUFFIX = new StringParameter(
	    "Filename suffix", "Suffix to be appended to peak-list file name",
	    "smoothed");

    /**
     * Filter width.
     */
    public static final ComboParameter<Integer> FILTER_WIDTH = new ComboParameter<Integer>(
	    "Filter width",
	    "Number of data point covered by the smoothing filter",
	    new Integer[] { 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25 }, 5);

    /**
     * Remove original data file.
     */
    public static final BooleanParameter REMOVE_ORIGINAL = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, the source peak list will be replaced by the smoothed version");

    /**
     * Create the parameter set.
     */
    public SmoothingParameters() {
	super(new Parameter[] { peakLists, SUFFIX, FILTER_WIDTH,
		REMOVE_ORIGINAL });
    }
}