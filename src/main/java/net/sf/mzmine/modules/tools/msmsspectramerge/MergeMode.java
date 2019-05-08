package net.sf.mzmine.modules.tools.msmsspectramerge;

public enum MergeMode {

    NO_MERGE("no merge"),
    CONSECUTIVE_SCANS("consecutive scans"),
    SAME_SAMPLE("same sample"),
    ACROSS_SAMPLES("across samples");

    private final String simpleName;

    MergeMode(String simpleName) {
        this.simpleName = simpleName;
    }

    @Override
    public String toString() {
        return simpleName;
    }
}
