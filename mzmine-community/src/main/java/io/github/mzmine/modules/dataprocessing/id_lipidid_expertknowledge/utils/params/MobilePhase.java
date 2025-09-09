package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

/**
 * This class represents the user input mobile phases in the .txt files.
 */

public class MobilePhase {
    private final String symbol;
    private final String name;

    public MobilePhase(String symbol, String name) {
        this.symbol = symbol;
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return symbol + " (" + name + ")";
    }
}
