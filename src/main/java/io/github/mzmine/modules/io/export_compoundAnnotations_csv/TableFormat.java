package io.github.mzmine.modules.io.export_compoundAnnotations_csv;

public enum TableFormat {
    /**
     * The standard table format that mzmine uses for display with columns for each type
     */
    WIDE,
    /**
     * AN export format where each type is a new row in the table: Columns are:
     * feature_id, TODO : Describe here
     */
    LONG;

}