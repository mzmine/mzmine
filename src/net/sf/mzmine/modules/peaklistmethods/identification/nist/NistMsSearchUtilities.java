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

import java.io.File;

/**
 * Useful NIST MS Search utilities.
 *
 * @author $Author$
 * @version $Revision$
 */
public class NistMsSearchUtilities {

    // System property holding the path to the executable.
    public static final String NIST_MS_SEARCH_PATH_PROPERTY = "nist.ms.search.path";

    // NIST MS Search home directory and executable.
    public static final String NIST_MS_SEARCH_DIR = System.getProperty(NIST_MS_SEARCH_PATH_PROPERTY);
    public static final File NIST_MS_SEARCH_EXE =
            NIST_MS_SEARCH_DIR == null ? null : new File(NIST_MS_SEARCH_DIR, "nistms$.exe");

    /**
     * Utility class - private constructor.
     */
    private NistMsSearchUtilities() {
        // no public access.
    }
}
