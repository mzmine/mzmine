package util.lipidvalidationtest;

import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;

class CapturingFeatureListRow extends DummyFeatureListRow {
    private FoundLipid capturedLipid;

    public CapturingFeatureListRow(DummyModularFeatureList list, int rowNum) {
        super(list, rowNum);
    }

    @Override
    public void addLipidValidation(FoundLipid lipid) {
        this.capturedLipid = lipid;
    }

    public FoundLipid getCapturedLipid() {
        return capturedLipid;
    }
}
