package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;

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

    public FoundLipid(MatchedLipid lipid) {
        this.lipid = null;
        this.score = 0; // Default score
        this.annotatedLipid = lipid;
        this.descrCorrect = lipid.getLipidAnnotation().getLipidClass().getAbbr() + "-Correct: ";
        this.descrIncorrect = "INCORRECT, please verify: ";
    }

    public Integer getScore() {
        return score;
    }

    // Setters (to modify score and description dynamically)
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
