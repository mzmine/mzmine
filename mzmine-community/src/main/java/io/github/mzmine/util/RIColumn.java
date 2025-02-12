package io.github.mzmine.util;

public enum RIColumn {
    DEFAULT,
    SEMIPOLAR,
    NONPOLAR,
    POLAR;

    public String toString() {
        return this.name();
    }
}
