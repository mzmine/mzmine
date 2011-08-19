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

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.nist;

import net.sf.mzmine.data.IonizationType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PeakListsParameter;

/**
 * Holds NIST MS Search parameters.
 *
 * @author $Author: cpudney $
 * @version $Revision: 2369 $
 */
public class NistMsSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    /**
     * Ionization method.
     */
    public static final ComboParameter<IonizationType> IONIZATION_METHOD = new ComboParameter<IonizationType>(
            "Ionization method",
            "Type of ion used to calculate the neutral mass",
            IonizationType.values());

    /**
     * Spectrum RT width.
     */
    public static final DoubleParameter SPECTRUM_RT_WIDTH = new DoubleParameter(
            "Spectrum RT tolerance",
            "The RT tolerance (>= 0) to use when forming search spectra; include all other detected peaks whose RT is within the specified tolerance of a given peak.",
            3.0,
            0.0,
            null);

    /**
     * Match factor cut-off.
     */
    public static final IntegerParameter MIN_MATCH_FACTOR = new IntegerParameter(
            "Min. match factor",
            "The minimum match factor (0 .. 1000) that search hits must have.",
            800, 0, 1000);

    /**
     * Match factor cut-off.
     */
    public static final IntegerParameter MIN_REVERSE_MATCH_FACTOR = new IntegerParameter(
            "Min. reverse match factor",
            "The minimum reverse match factor (0 .. 1000) that search hits must have.",
            800, 0, 1000);

    /**
     * Construct the parameter set.
     */
    public NistMsSearchParameters() {
        super(new Parameter[]{peakLists, IONIZATION_METHOD,
                              SPECTRUM_RT_WIDTH, MIN_MATCH_FACTOR, MIN_REVERSE_MATCH_FACTOR});
    }
}
