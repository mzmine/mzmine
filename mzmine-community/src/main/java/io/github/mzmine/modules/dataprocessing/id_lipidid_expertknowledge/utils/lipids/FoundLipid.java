package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.FoundAdduct;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class that represents the lipids found in my features
 */
public class FoundLipid {

    private Lipid lipid;
    //To store the score and description assigned by the rules
    private MatchedLipid annotatedLipid;
    private Integer score;
    private String descrCorrect;
    private String descrIncorrect;
    private List<FoundAdduct> adducts;

    public FoundLipid(MatchedLipid lipid) {
        this.lipid = null;
        this.score = 0; // Default score
        this.annotatedLipid = lipid;
        this.descrCorrect = lipid.getLipidAnnotation().getLipidClass().getAbbr() + "-Correct: ";
        this.descrIncorrect = "INCORRECT, please verify: ";
        this.adducts = new ArrayList<>();
    }

    public String getAdducts() {
        String stringAdducts = this.adducts.toString();
        return stringAdducts;
    }

    public void setAdducts(List<FoundAdduct> adducts) {
        this.adducts = adducts;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = this.score + score;
    }

    public Lipid getLipid() {
        return lipid;
    }

    public void setLipid(Lipid lipid) {
        this.lipid = lipid;
    }
    public String getDescrCorrect() {
        return descrCorrect;
    }

    public void setDescrCorrect(String descrCorrect) {
        this.descrCorrect = this.descrCorrect + descrCorrect;
    }

    public String getDescrIncorrect() {
        return descrIncorrect;
    }

    public MatchedLipid getAnnotatedLipid() {
        return annotatedLipid;
    }

    public void setAnnotatedLipid(MatchedLipid annotatedLipid) {
        this.annotatedLipid = annotatedLipid;
    }

    public void setDescrIncorrect(String descrIncorrect) {
        this.descrIncorrect = this.descrIncorrect + descrIncorrect;
    }

    @Override
    public String toString() {
        return  "[" + annotatedLipid +
                ']';
    }
}
