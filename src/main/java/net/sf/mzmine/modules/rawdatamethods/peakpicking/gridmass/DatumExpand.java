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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.peakpicking.gridmass;

class DatumExpand implements Comparable<DatumExpand> {
    Datum dato;
    boolean left;
    boolean right;
    boolean up;
    boolean down;
    double expanded = Double.MAX_VALUE; // expanded to a certain threshold
    int index;
    double minIntensity = 0;

    DatumExpand(Datum dato, boolean l, boolean r, boolean up, boolean dw,
	    int pos) {
	this.dato = dato;
	this.left = l;
	this.right = r;
	this.up = up;
	this.down = dw;
	this.index = pos;
	minIntensity = dato.intensity;
    }

    public int compareTo(DatumExpand other) {
	if (dato.scan < other.dato.scan)
	    return -1;
	if (dato.scan > other.dato.scan)
	    return 1;

	// equal scan, then sort by lower mz
	if (dato.mz < other.dato.mz)
	    return -1;
	if (dato.mz > other.dato.mz)
	    return 1;

	return 0;
    }

}
