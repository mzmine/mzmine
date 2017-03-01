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
package net.sf.mzmine.modules.peaklistmethods.alignment.path.functions;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.ParameterSet;

public interface ScoreCalculator {

    /**
     *
     * @param path
     * @param peak
     * @param params
     * @return
     */
    double calculateScore(AlignmentPath path, PeakListRow peak,
	    ParameterSet params);

    double getWorstScore();

    /**
     * Is score calculated by calculate Score in any way meaningful? If path and
     * peak don't match in ScoreCalculator's mind, value returned from
     * calculateScore may still be finite, but matches returns false.
     * 
     * @param path
     * @param peak
     * @param params
     * @return
     */
    boolean matches(AlignmentPath path, PeakListRow peak, ParameterSet params);

    boolean isValid(PeakListRow peak);

    String name();
}
