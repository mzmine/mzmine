/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.deisotoping.simplegrouper;

import java.util.Comparator;

import net.sf.mzmine.data.Peak;

/**
 * This is a helper class required for TreeSet to sorting peaks in order of
 * decreasing intensity.
 */
class PeakSorterByDescendingHeight implements Comparator<Peak> {

    public int compare(Peak p1, Peak p2) {
        if (p1 == p2)
            return 0;
        if (p1.getNormalizedHeight() <= p2.getNormalizedHeight()) {
            return 1;
        } else {
            return -1;
        }
    }

    public boolean equals(Object obj) {
        return (obj == this);
    }
}