package io.github.mzmine.datamodel.features.types.annotations.lipidexpertknowledge;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.*;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid_expertknowledge.utils.lipids.FoundLipid;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class LipidValidationListType extends ListWithSubsType<FoundLipid> implements AnnotationType {

    private static final List<DataType> subTypes = List.of(
            new LipidValidationListType(),
            new LipidValidationScoreType(),
            new LipidValidationCorrectDescriptionType(),
            new LipidValidationIncorrectDescriptionType()
            );

    @Override
    public @NotNull String getUniqueID() {
        return "lipid_validations";
    }

    @Override
    public @NotNull String getHeaderString() {
        return "Lipid Validation";
    }

    @Override
    public @NotNull List<DataType> getSubDataTypes() {
        return subTypes;
    }

    @Override
    public <K> @Nullable K map(@NotNull DataType<K> subType, FoundLipid lipid) {
        if (subType instanceof LipidValidationListType) {
            return (K) lipid;
        } else if (subType instanceof LipidValidationScoreType) {
            return (K) lipid.getScore();
        } else if (subType instanceof LipidValidationCorrectDescriptionType) {
            return (K) lipid.getDescrCorrect();
        } else if (subType instanceof LipidValidationIncorrectDescriptionType) {
            return (K) lipid.getDescrIncorrect();
        } else {
            throw new UnsupportedOperationException(
                    "DataType %s is not covered in map".formatted(subType.toString())
            );
        }
    }


    public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
                          @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
                          @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
        if (value == null) {
            return;
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(
                    "Wrong value type for data type: " + this.getClass().getName() + " value class: "
                            + value.getClass());
        }

        for (Object o : list) {
            if (!(o instanceof MatchedLipid id)) {
                continue;
            }

            id.saveToXML(writer, flist, row);
        }
    }

    @Override
    public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
                              @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
                              @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

        if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
                && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
            throw new IllegalStateException("Wrong element");
        }

        List<MatchedLipid> ids = new ArrayList<>();
        final List<RawDataFile> currentRawDataFiles = project.getCurrentRawDataFiles();

        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
                .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
            reader.next();
            if (!reader.isStartElement()) {
                continue;
            }

            if (reader.getLocalName().equals(MatchedLipid.XML_ELEMENT) || (
                    reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)
                            && MatchedLipid.XML_ELEMENT.equals(
                            reader.getAttributeValue(null, FeatureAnnotation.XML_TYPE_ATTR)))) {
                MatchedLipid id = MatchedLipid.loadFromXML(reader, currentRawDataFiles);
                ids.add(id);
            }
        }

        // never return null, if this type was saved we even need empty lists.
        return ids;
    }

    @Override
    public boolean getDefaultVisibility() {
        return true;
    }
}
