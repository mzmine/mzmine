package util.lipidvalidationtest;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.LipidIDExpertKnowledgeSearch;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.CommonAdductPositive;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.FoundAdduct;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.Lipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.params.MobilePhases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestFindLipids {

    DummyModularFeatureList dummyList;
    CapturingFeatureListRow row;

    @BeforeEach
    void setUp(){
        // CODE THAT IS COMMON TO ALL LIPID TESTS
        dummyList = new DummyModularFeatureList(null, null, List.of(new DummyRawDataFile()));
        row = new CapturingFeatureListRow(dummyList, 0);
    }

    /**
     * This test assigns the correct lipid with correct evidence.
     * It has all the mobile phases so [M+C2H7N2]+ should appear, as well as a normal adduct like [M+Na]+.
     */
    @Test
    void testFindLipidsPositive_DG_allMobilePhases_C2H7N2_Na() {

        LipidClasses lipid = LipidClasses.DIACYLGLYCEROLS;
        MolecularSpeciesLevelAnnotation msla = new MolecularSpeciesLevelAnnotation(lipid, null, null, null);
        MatchedLipid matchedLipid = new MatchedLipid(msla, null, null, null, null);

        List<MobilePhases> mobilePhases = MobilePhases.getListOfMobilePhases();
        List<FoundAdduct> foundAdducts = new ArrayList<>();
        foundAdducts.add(new FoundAdduct((CommonAdductPositive.M_PLUS_C2H7N2).getCompleteName(), 0.00, 0.00, 0.00, +1));
        foundAdducts.add(new FoundAdduct((CommonAdductPositive.M_PLUS_NA.getCompleteName()), 0.00, 0.00, 0.00, +1));

        LipidIDExpertKnowledgeSearch.findLipidsPositive(row, matchedLipid, foundAdducts, mobilePhases);

        FoundLipid found = row.getCapturedLipid(); // get what was added

        assertEquals(Lipid.DG.getName(), found.getLipid().getName());
        assertTrue(found.getScore() > 0);
        String description = found.getDescrCorrect();
        assertTrue(description.contains("C2H7N2"), "Description should contain 'C2H7N2'");
        assertTrue(description.contains("Na"), "Description should contain 'Na'");
    }

    /**
     * This test assigns the correct lipid with correct evidence.
     * It does not have all the mobile phases so [M+C2H7N2]+ should not appear, [M+Na]+ should appear.
     */
    @Test
    void testFindLipidsPositive_DG_someMobilePhases_NoC2H7N2_Na() {

        LipidClasses lipid = LipidClasses.DIACYLGLYCEROLS;
        MolecularSpeciesLevelAnnotation msla = new MolecularSpeciesLevelAnnotation(lipid, null, null, null);
        MatchedLipid matchedLipid = new MatchedLipid(msla, null, null, null, null);

        List<MobilePhases> mobilePhases = new ArrayList<>();
        mobilePhases.add(MobilePhases.NH4);
        mobilePhases.add(MobilePhases.CH3CN);
        List<FoundAdduct> foundAdducts = new ArrayList<>();
        foundAdducts.add(new FoundAdduct((CommonAdductPositive.M_PLUS_C2H7N2).getCompleteName(), 0.00, 0.00, 0.00, +1));
        foundAdducts.add(new FoundAdduct((CommonAdductPositive.M_PLUS_NA.getCompleteName()), 0.00, 0.00, 0.00, +1));

        LipidIDExpertKnowledgeSearch.findLipidsPositive(row, matchedLipid, foundAdducts, mobilePhases);

        FoundLipid found = row.getCapturedLipid();

        assertEquals(Lipid.DG.getName(), found.getLipid().getName());
        assertTrue(found.getScore() > 0);
        String description = found.getDescrCorrect();
        assertFalse(description.contains("C2H7N2"), "Description should not contain 'C2H7N2'");
        assertTrue(description.contains("Na"), "Description should contain 'Na'");
    }

    /**
     * This test does not have a path for a drl file so the added lipid is null.
     */
    @Test
    void testFindLipidsPositive_pathIsNull_noValidationAdded() {

        LipidClasses unknownLipid = LipidClasses.FATTYACIDESTOLIDES;
        MolecularSpeciesLevelAnnotation annotation = new MolecularSpeciesLevelAnnotation(unknownLipid, null, null, null);
        MatchedLipid matchedLipid = new MatchedLipid(annotation, null, null, null, null);

        List<FoundAdduct> adducts = List.of(
                new FoundAdduct(CommonAdductPositive.M_PLUS_NA.getName(), 0.0, 0.0, 0.0, +1)
        );
        List<MobilePhases> mobilePhases = MobilePhases.getListOfMobilePhases();

        LipidIDExpertKnowledgeSearch.findLipidsPositive(row, matchedLipid, adducts, mobilePhases);

        // Assert that no actual lipid was added
        FoundLipid captured = row.getCapturedLipid();
        assertTrue(captured.getScore() == 0.0, "No meaningful lipid should be added when path is null");
    }


}
