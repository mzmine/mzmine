package util.lipidvalidationtest;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.LipidIDExpertKnowledgeSearch;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.RowInfo;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.Collectors;

public class TestFindAdducts {

    /**
     * This test should find an adduct in the list when it has a matched lipid.
     * It finds the [M+H]+ adduct.
     * This is the case where the row is annotated and has an adduct which takes as reference, so it should be found.
     */
    @Test
    public void testFindAdducts_shouldFindExpectedAdducts() {
        ExpertKnowledge adductH = CommonAdductPositive.M_PLUS_H;
        List<ExpertKnowledge> adductsISF = List.of(adductH);

        List<Double> mzList = List.of(501.0, 200.4);
        List<Float> intensityList = List.of(1000f, 1000f);
        List<Float> rtList = List.of(5.0f, 3.8f);
        RowInfo rowInfo = new RowInfo(mzList, intensityList, rtList);

        MatchedLipid match = new MatchedLipid(null, null, IonizationType.POSITIVE_HYDROGEN, null, null);
        List<MatchedLipid> matches = new ArrayList<>();
        matches.add(match);
        DummyModularFeatureList flist = new DummyModularFeatureList(null, null, List.of(new DummyRawDataFile()));
        DummyFeatureListRow row = new DummyFeatureListRow(flist, 0);
        row.setMatchedLipid(matches);
        row.setAverageMZ(500.0);
        row.setMaxHeight(1000.0f);
        row.setAverageRT(5.0f);

        DummyRawDataFile file = new DummyRawDataFile();
        file.setPolarity(PolarityType.POSITIVE);

        double mzTolerance = 0.5;

        List<FoundAdduct> foundAdducts = LipidIDExpertKnowledgeSearch.findAdducts(adductsISF, rowInfo, mzTolerance, row, match);

        assertFalse(foundAdducts.isEmpty(), "Should find at least one adduct");
        assertEquals("[M+H]+", foundAdducts.get(0).getAdductName());
        assertEquals(1, foundAdducts.get(0).getCharge());
    }

    /**
     * This test should find adducts in case there are no matched lipids, so it does not have a reference adduct.
     * It should find [M+H]+, [M+Na]+ and [M+K]+.
     * This is the case where the row is not annotated but it finds adducts.
     */
    @Test
    public void testFindAdducts_shouldFindExpectedAdducts_noMatch() {
        ExpertKnowledge adductH = CommonAdductPositive.M_PLUS_H;
        ExpertKnowledge adductNa = CommonAdductPositive.M_PLUS_NA;
        ExpertKnowledge adductK = CommonAdductPositive.M_PLUS_K;
        List<ExpertKnowledge> adductsISF = List.of(adductH, adductNa, adductK);

        List<Double> mzList = List.of(501.007, 522.990, 538.963);
        List<Float> intensityList = List.of(1000f, 800f, 600f);
        List<Float> rtList = List.of(5.0f, 5.1f, 5.2f);
        RowInfo rowInfo = new RowInfo(mzList, intensityList, rtList);

        //MatchedLipid match = new MatchedLipid(null, null, IonizationType.POSITIVE_HYDROGEN, null, null);
        List<MatchedLipid> matches = new ArrayList<>();
        //matches.add(match);
        DummyModularFeatureList flist = new DummyModularFeatureList(null, null, List.of(new DummyRawDataFile()));
        DummyFeatureListRow row = new DummyFeatureListRow(flist, 0);
        row.setMatchedLipid(matches);
        row.setAverageMZ(500.0);
        row.setMaxHeight(1000.0f);
        row.setAverageRT(5.0f);

        DummyRawDataFile file = new DummyRawDataFile();
        file.setPolarity(PolarityType.POSITIVE);

        double mzTolerance = 0.01;

        List<FoundAdduct> foundAdducts = LipidIDExpertKnowledgeSearch.findAdducts(adductsISF, rowInfo, mzTolerance, row, null);

        List<String> foundNames = foundAdducts.stream().map(FoundAdduct::getAdductName).collect(Collectors.toList());
        assertTrue(foundNames.contains(adductH.getCompleteName()));
        assertTrue(foundNames.contains(adductNa.getCompleteName()));
        assertTrue(foundNames.contains(adductK.getCompleteName()));
    }

    /**
     * This test should not find any adducts, it has matched lipid.
     * The list should be empty.
     * This is the case where the row is annotated and it does not find adducts.
     */
    @Test
    public void testFindAdducts_shouldNotFindExpectedAdducts() {
        ExpertKnowledge adductH = CommonAdductPositive.M_PLUS_NH4;
        List<ExpertKnowledge> adductsISF = List.of(adductH);

        List<Double> mzList = List.of(500.0);
        List<Float> intensityList = List.of(1000f);
        List<Float> rtList = List.of(5.0f);
        RowInfo rowInfo = new RowInfo(mzList, intensityList, rtList);

        MatchedLipid match = new MatchedLipid(null, null, IonizationType.POSITIVE_HYDROGEN, null, null);
        List<MatchedLipid> matches = new ArrayList<>();
        matches.add(match);
        DummyModularFeatureList flist = new DummyModularFeatureList(null, null, List.of(new DummyRawDataFile()));
        DummyFeatureListRow row = new DummyFeatureListRow(flist, 0);
        row.setMatchedLipid(matches);
        row.setAverageMZ(600.0);
        row.setMaxHeight(1000.0f);
        row.setAverageRT(5.0f);

        DummyRawDataFile file = new DummyRawDataFile();
        file.setPolarity(PolarityType.POSITIVE);

        double mzTolerance = 0.02;

        List<FoundAdduct> foundAdducts = LipidIDExpertKnowledgeSearch.findAdducts(adductsISF, rowInfo, mzTolerance, row, match);

        assertTrue(foundAdducts.isEmpty(), "Should not find an adduct");
    }

    /**
     * This test should not find any adducts, it has matched lipid.
     * The list should be empty.
     * This is the case where the row is not annotated and it does not find adducts.
     */
    @Test
    public void testFindAdducts_shouldNotFindExpectedAdducts_noMatch() {
        ExpertKnowledge adductH = CommonAdductPositive.M_PLUS_H;
        ExpertKnowledge adductNa = CommonAdductPositive.M_PLUS_NA;
        ExpertKnowledge adductK = CommonAdductPositive.M_PLUS_K;
        List<ExpertKnowledge> adductsISF = List.of(adductH, adductNa, adductK);

        List<Double> mzList = List.of(100.0, 200.0, 300.0);
        List<Float> intensityList = List.of(1000f, 800f, 600f);
        List<Float> rtList = List.of(1.0f, 1.1f, 1.2f);
        RowInfo rowInfo = new RowInfo(mzList, intensityList, rtList);

        DummyModularFeatureList flist = new DummyModularFeatureList(null, null, List.of(new DummyRawDataFile()));
        DummyFeatureListRow row = new DummyFeatureListRow(flist, 0);
        row.setMatchedLipid(new ArrayList<>());

        row.setAverageMZ(100.0);
        row.setMaxHeight(1000.0f);
        row.setAverageRT(1.0f);

        DummyRawDataFile file = new DummyRawDataFile();
        file.setPolarity(PolarityType.POSITIVE);

        double mzTolerance = 0.01;

        List<FoundAdduct> foundAdducts = LipidIDExpertKnowledgeSearch.findAdducts(adductsISF, rowInfo, mzTolerance, row, null);


        assertTrue(foundAdducts.isEmpty(), "Should not find any adducts since no combinations match");
    }

}
