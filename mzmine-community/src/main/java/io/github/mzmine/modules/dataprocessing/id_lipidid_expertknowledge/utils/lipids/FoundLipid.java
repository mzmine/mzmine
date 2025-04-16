package io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.adducts.FoundAdduct;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class that represents the lipids found in my features
 */
public class FoundLipid {

    private Lipid lipid;
    //To store the score and description assigned by the rules
    private MatchedLipid annotatedLipid;
    private Double score;
    private String descrCorrect;
    private String descrIncorrect;
    private List<FoundAdduct> adducts;
    public static final String XML_ELEMENT = "lipidvalidation";
    private static final Logger logger = Logger.getLogger(FoundLipid.class.getName());
    private static final String XML_LIPID_VALIDATION_ELEMENT = "lipidvalidation";
    private static final String XML_MATCHED_LIPID = "matchedlipid";
    private static final String XML_SCORE = "score";
    private static final String XML_CORRECT = "correctdescription";
    private static final String XML_INCORRECT = "incorrectdescription";
    private static final String XML_ADDUCTS = "adducts";

    public FoundLipid(MatchedLipid lipid) {
        this.lipid = null;
        this.score = 0.00; // Default score
        this.annotatedLipid = lipid;
        this.descrCorrect = lipid.getLipidAnnotation().getLipidClass().getAbbr() + "-Correct: ";
        this.descrIncorrect = "Verify: ";
        this.adducts = new ArrayList<>();
    }

    public FoundLipid(List<FoundAdduct> foundAdducts) {
        this.lipid = null;
        this.score = 0.00;
        this.annotatedLipid = null;
        this.descrCorrect = "No matched lipids found";
        this.descrIncorrect = "Found adducts are assumptions!";
        this.adducts = foundAdducts;
    }

    public String getAdducts() {
        String stringAdducts = this.adducts.toString();
        return stringAdducts;
    }

    public void setAdducts(List<FoundAdduct> adducts) {
        this.adducts = adducts;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(double score) {
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

    public double normalizeScore(double rawScore, double maxScore) {
        return Math.max(0.0, Math.min(2.0, ((double)(rawScore + maxScore) / (2.0 * maxScore)) * 2.0));
    }

    public void setFinalScore(double score) { this.score = score; }

    private static List<FoundAdduct> loadAdductsFromXML(XMLStreamReader reader)
            throws XMLStreamException {

        List<FoundAdduct> adducts = new ArrayList<>();

        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement() && reader.getLocalName().equals("adduct")) {
                adducts.add(loadSingleAdductFromXML(reader));
            } else if (reader.isEndElement() && reader.getLocalName().equals("adducts")) {
                break;
            }
        }

        return adducts;
    }

    private static FoundAdduct loadSingleAdductFromXML(XMLStreamReader reader)
            throws XMLStreamException {

        String adductName = null;
        double mzFeature = 0;
        double intensity = 0;
        double rt = 0;
        int charge = 0;

        while (reader.hasNext()) {
            reader.next();

            if (reader.isStartElement()) {
                switch (reader.getLocalName()) {
                    case "adductName" -> adductName = reader.getElementText();
                    case "mzFeature" -> mzFeature = Double.parseDouble(reader.getElementText());
                    case "intensity" -> intensity = Double.parseDouble(reader.getElementText());
                    case "rt" -> rt = Double.parseDouble(reader.getElementText());
                    case "charge" -> charge = Integer.parseInt(reader.getElementText());
                }
            } else if (reader.isEndElement() && reader.getLocalName().equals("adduct")) {
                break;
            }
        }

        FoundAdduct adduct = new FoundAdduct(adductName, mzFeature, intensity, rt, charge);

        return adduct;
    }

    public static FoundLipid loadFromXML(XMLStreamReader reader, Collection<RawDataFile> possibleFiles)
            throws XMLStreamException {

        if (reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT)) {
            logger.finest("Loading FoundLipid entry.");
        } else {
            throw new IllegalStateException("Cannot load FoundLipid from the current element. Wrong name: " + reader.getLocalName());
        }

        MatchedLipid annotatedLipid = null;
        Double score = null;
        String descrCorrect = null;
        String descrIncorrect = null;
        List<FoundAdduct> adducts = new ArrayList<>();

        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName().equals(XML_ELEMENT))) {
            reader.next();
            if (!reader.isStartElement()) {
                continue;
            }

            switch (reader.getLocalName()) {
                case XML_MATCHED_LIPID ->
                        annotatedLipid = MatchedLipid.loadFromXML(reader, possibleFiles);

                case XML_SCORE ->
                        score = Double.parseDouble(reader.getElementText());

                case XML_CORRECT ->
                        descrCorrect = reader.getElementText();

                case XML_INCORRECT ->
                        descrIncorrect = reader.getElementText();

                case XML_ADDUCTS ->
                        adducts = loadAdductsFromXML(reader);

                default -> {
                    // Unknown element; skip it or log it
                    logger.fine("Skipping unknown element: " + reader.getLocalName());
                }
            }
        }

        FoundLipid foundLipid = new FoundLipid(annotatedLipid);
        foundLipid.setScore(score);
        foundLipid.setDescrCorrect(descrCorrect);
        foundLipid.setDescrIncorrect(descrIncorrect);
        foundLipid.setAdducts(adducts);

        return foundLipid;
    }

    public void saveToXML(@NotNull XMLStreamWriter writer,
                          @NotNull ModularFeatureList flist,
                          @NotNull ModularFeatureListRow row) throws XMLStreamException {
        writer.writeStartElement(XML_ELEMENT);

        // Save matched lipid
        if (annotatedLipid != null) {
            writer.writeStartElement(XML_MATCHED_LIPID);
            annotatedLipid.saveToXML(writer, flist, row);
            writer.writeEndElement();
        }

        // Save score
        writer.writeStartElement(XML_SCORE);
        writer.writeCharacters(score != null ? score.toString() : CONST.XML_NULL_VALUE);
        writer.writeEndElement();

        // Save correct description
        writer.writeStartElement(XML_CORRECT);
        writer.writeCharacters(descrCorrect != null ? descrCorrect : CONST.XML_NULL_VALUE);
        writer.writeEndElement();

        // Save incorrect description
        writer.writeStartElement(XML_INCORRECT);
        writer.writeCharacters(descrIncorrect != null ? descrIncorrect : CONST.XML_NULL_VALUE);
        writer.writeEndElement();

        // Save adducts
        writer.writeStartElement(XML_ADDUCTS);
        if (adducts != null && !adducts.isEmpty()) {
            for (FoundAdduct adduct : adducts) {
                writer.writeStartElement("adduct");

                writer.writeStartElement("adductName");
                writer.writeCharacters(adduct.getAdductName());
                writer.writeEndElement();

                writer.writeStartElement("mzFeature");
                writer.writeCharacters(Double.toString(adduct.getMzFeature()));
                writer.writeEndElement();

                writer.writeStartElement("intensity");
                writer.writeCharacters(Double.toString(adduct.getIntensity()));
                writer.writeEndElement();

                writer.writeStartElement("rt");
                writer.writeCharacters(Double.toString(adduct.getRt()));
                writer.writeEndElement();

                writer.writeStartElement("charge");
                writer.writeCharacters(Integer.toString(adduct.getCharge()));
                writer.writeEndElement();

                writer.writeEndElement(); // </adduct>
            }
        } else {
            writer.writeCharacters(CONST.XML_NULL_VALUE);
        }
        writer.writeEndElement(); // </adducts>

        writer.writeEndElement(); // </lipidvalidation>
    }



}
