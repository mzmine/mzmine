package net.sf.mzmine.util.R;

public enum REngineType {

    RSERVE("RServe"),
    RCALLER("RCaller");

    private final String name;

    REngineType(String name) {
        this.name = name;
    }

    public String toString() {
        return this.name;
    }

}
