package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params;

public final class SampleType {

    private final String name;
    private final SampleTypes predefined;

    private SampleType(SampleTypes predefined, String name) {
        this.predefined = predefined;
        this.name = name;
    }

    public static SampleType of(SampleTypes type) {
        return new SampleType(type, type.name());
    }

    public static SampleType of(String customName) {
        return new SampleType(null, customName);
    }

    public boolean isPredefined() {
        return predefined != null;
    }

    public SampleTypes getPredefined() {
        return predefined;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

