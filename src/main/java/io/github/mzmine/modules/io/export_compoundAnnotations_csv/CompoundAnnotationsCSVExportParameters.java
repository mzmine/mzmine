package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import javafx.stage.FileChooser;

import java.util.List;

public class CompoundAnnotationsCSVExportParameters extends SimpleParameterSet {

    public static final FeatureListsParameter featureLists = new FeatureListsParameter(1);

    private static final List<FileChooser.ExtensionFilter> extensions = List.of( //
            new FileChooser.ExtensionFilter("comma-separated values", "*.csv"), //
            new FileChooser.ExtensionFilter("tab-separated values", "*.tsv"), //
            new FileChooser.ExtensionFilter("All files", "*.*") //
    );

    public static final FileNameParameter filename = new FileNameParameter("Filename",
            "Name of the output CSV file. "
                    + "Use pattern \"{}\" in the file name to substitute with feature list name. "
                    + "(i.e. \"blah{}blah.csv\" would become \"blahSourceFeatureListNameblah.csv\"). "
                    + "If the file already exists, it will be overwritten.", extensions,
            FileSelectionType.SAVE);
    public CompoundAnnotationsCSVExportParameters() {
        super(featureLists, filename);
    }

}
