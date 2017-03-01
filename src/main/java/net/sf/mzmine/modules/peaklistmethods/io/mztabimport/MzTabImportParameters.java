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

package net.sf.mzmine.modules.peaklistmethods.io.mztabimport;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNamesParameter;

public class MzTabImportParameters extends SimpleParameterSet {

    private static final FileFilter filters[] = new FileFilter[] {
            new FileNameExtensionFilter("mztab files", "mztab") };

    public static final FileNamesParameter file = new FileNamesParameter(
            "mzTab files", "mzTab files to import.", filters);

    public static final BooleanParameter importrawfiles = new BooleanParameter(
            "Import raw data files?",
            "If selected, raw data files will also be imported if they are available. If some raw data files cannot be found, empty files will be generated instead.");

    public MzTabImportParameters() {
        super(new Parameter[] { file, importrawfiles });
    }
}
