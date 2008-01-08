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

package net.sf.mzmine.modules.alignment.join;

import java.util.Comparator;

/**
 * This is a helper class required for TreeSet to sorting scores in order of
 * descending goodness of fit.
 */
class ScoreSorter implements Comparator<PeakVsRowScore> {

    public int compare(PeakVsRowScore score1, PeakVsRowScore score2) {

        // Smaller score value means smaller M/Z and RT difference
        // and therefore smaller score is better and should come first
        if (score1.getScore() < score2.getScore())
            return -1;
        return 1;

    }

}