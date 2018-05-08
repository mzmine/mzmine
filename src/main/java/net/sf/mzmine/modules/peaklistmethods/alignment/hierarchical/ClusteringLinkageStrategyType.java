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

package net.sf.mzmine.modules.peaklistmethods.alignment.hierarchical;

public enum ClusteringLinkageStrategyType {

    AVERAGE("Average"), // Distance of a new cluster is the Average distance of
                        // children
    COMPLETE("Complete"), // Distance of a new cluster is the Max distance over
                          // all children
    SINGLE("Single"); // Distance of a new cluster is the Min distance over all
                      // children
    // WEIGHTED("Weighted"); // Distance of a new cluster is the Weighted
    // distance over all children

    private final String name;

    ClusteringLinkageStrategyType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

}
