/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.util.spectraldb.entry;

import java.text.MessageFormat;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;

public class PrecursorDBPeakIdentity extends SimplePeakIdentity {

    private final SpectralDBEntry entry;

    public PrecursorDBPeakIdentity(SpectralDBEntry entry, String method) {
        super(MessageFormat.format("Precursor? {0} as {3} ({1}) {2}",
                entry.getField(DBEntryField.NAME).orElse("NONAME"), // Name
                entry.getField(DBEntryField.MZ).orElse(""), // precursor m/z
                entry.getField(DBEntryField.FORMULA).orElse(""), // molecular
                                                                 // formula
                entry.getField(DBEntryField.ION_TYPE).orElse("")), // Ion type
                entry.getField(DBEntryField.FORMULA).orElse("").toString(),
                method, "", "");
        this.entry = entry;
    }

    public SpectralDBEntry getEntry() {
        return entry;
    }

    public DataPoint[] getLibraryDataPoints() {
        return entry.getDataPoints();
    }

}
