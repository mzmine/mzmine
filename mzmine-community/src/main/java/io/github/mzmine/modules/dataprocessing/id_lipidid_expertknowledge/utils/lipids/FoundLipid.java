package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids;

public class FoundLipid {

    private Lipid lipid;
    //To store the score and description assigned by the rules
    private Integer score;
    private String descrCorrect;
    private String descrIncorrect;

    public FoundLipid() {
        this.lipid = null;
        this.score = 0; // Default score
        this.descrCorrect = "Correct: " + lipid.getName();
        this.descrIncorrect = "INCORRECT, please verify: " + lipid.getName();
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

    public void setDescrIncorrect(String descrIncorrect) {
        this.descrIncorrect = this.descrIncorrect + descrIncorrect;
    }
}
