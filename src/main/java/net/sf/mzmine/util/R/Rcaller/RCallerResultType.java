package net.sf.mzmine.util.R.Rcaller;

public enum RCallerResultType {

    DOUBLE_ARRAY("DoubleArray"),
    DOUBLE_MATRIX("DoubleMatrix"),
    INT_ARRAY("IntArray"), 
    BOOL_ARRAY("BoolArray"), 
    STRING_ARRAY("StringArray"),
    UNKNOWN("Unknown");

    private final String name;

    RCallerResultType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

}
