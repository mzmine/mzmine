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

package net.sf.mzmine.util;

import java.util.Comparator;

import net.sf.mzmine.data.AlignmentResultRow;

/**
 */
public class AlignmentResultRowSorter implements Comparator<AlignmentResultRow> {

    public int compare(AlignmentResultRow row1, AlignmentResultRow row2) {
        Double mz1 = row1.getAverageMZ();
        Double mz2 = row2.getAverageMZ();
        return mz1.compareTo(mz2); 
    }

}