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

import net.sf.mzmine.datamodel.RawDataFile;

import java.util.Set;

public class Group {

    private final Set<RawDataFile> files;

    public Group(Set<RawDataFile> files) throws IllegalArgumentException {

        if (files == null || files.isEmpty())
            throw new IllegalArgumentException("List of files is empty or does not exist.");

        this.files = files;
    }

    public Set<RawDataFile> getFiles() {
        return files;
    }
}
