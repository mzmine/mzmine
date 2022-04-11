/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.group_metacorrelate.export;

import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

import java.util.List;

import javafx.stage.FileChooser.ExtensionFilter;

public class ExportCorrAnnotationParameters extends SimpleParameterSet {

    private static final List<ExtensionFilter> extensions = List.of( //
            new ExtensionFilter("comma-separated values", "*.csv"), //
            new ExtensionFilter("All files", "*.*") //
    );

    // NOT INCLUDED in sub
    // General parameters
    public static final FeatureListsParameter featureLists = new FeatureListsParameter();

    public static final FileNameParameter filename = new FileNameParameter("Filename", "Base file name of all edge files (Use {} to fill in the feature list name when exporting multiple feature lists at once)", extensions, FileSelectionType.SAVE);
    public static final BooleanParameter exportIIN = new BooleanParameter("Export IIN edges", "Export all edges of Ion Identity Networks (IIN)", true);
    public static final BooleanParameter exportIINRelationship = new BooleanParameter("Export IIN relationship edges", "Export relationships between Ion Identity Networks (IIN)", false);

    public static final MultiChoiceParameter<RowsRelationship.Type> exportTypes = new MultiChoiceParameter<>("Export row relationships", "Export all relationships of different rows to files", Type.values(), Type.values(), 1);

    public static final BooleanParameter allInOneFile = new BooleanParameter("Combine to one file", "Either combine to one file or export one file per relationship type", false);

    public static final ComboParameter<FeatureListRowsFilter> filter = new ComboParameter<>("Filter rows", "Limit the exported rows to those with MS/MS data or annotated rows", FeatureListRowsFilter.values(), FeatureListRowsFilter.MS2_OR_ION_IDENTITY);

    // Constructor
    public ExportCorrAnnotationParameters() {
        super(new Parameter[] {featureLists, filename, exportTypes, allInOneFile, exportIIN, exportIINRelationship, filter});
    }
}