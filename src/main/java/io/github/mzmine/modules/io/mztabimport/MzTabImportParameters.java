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

package io.github.mzmine.modules.io.mztabimport;

import java.util.List;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import javafx.stage.FileChooser.ExtensionFilter;

public class MzTabImportParameters extends SimpleParameterSet {

    private static final List<ExtensionFilter> filters = List
            .of(new ExtensionFilter("mztab files", "*.mztab"));

    public static final FileNamesParameter file = new FileNamesParameter(
            "mzTab files", "mzTab files to import.", filters);

    public static final BooleanParameter importrawfiles = new BooleanParameter(
            "Import raw data files?",
            "If selected, raw data files will also be imported if they are available. If some raw data files cannot be found, empty files will be generated instead.");

    public MzTabImportParameters() {
        super(new Parameter[] { file, importrawfiles });
    }
}
