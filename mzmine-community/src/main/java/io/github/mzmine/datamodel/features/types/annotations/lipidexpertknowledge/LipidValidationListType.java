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

/**
 * This class represents the columns for the feature in Lipid Validation module.
 * It includes the columns for "Found adducts", "Correct comments", "Incorrect comments" and "Score".
 */
public class LipidValidationListType extends ListWithSubsType<FoundLipid> implements AnnotationType {

    /**
     * List of all the classes this class contains.
     * Each class will be represented as a column in the feature list table.
     */
    private static final List<DataType> subTypes = List.of(
            new LipidValidationListType(),
            new LipidValidationScoreType(),
            new LipidValidationCorrectDescriptionType(),
            new LipidValidationIncorrectDescriptionType(),
            new LipidValidationAdductsType()
            );

    /**
     * Identifier for the group of columns.
     * @return The group ID.
     */
    @Override
    public @NotNull String getUniqueID() {
        return "lipid_validations";
    }

    /**
     * Header (name) for the group of columns.
     * It is what appears at the top of the column in the feature list table.
     * @return The column header.
     */
    @Override
    public @NotNull String getHeaderString() {
        return "Lipid Validation";
    }

    /**
     * Gets the group of classes.
     * @return List of all the classes.
     */
    @Override
    public @NotNull List<DataType> getSubDataTypes() {
        return subTypes;
    }

    /**
     * Maps each column with the data they contain.
     * @param subType    the data type of the sub column. Defines what value should be extracted from
     *                   the parentItem
     * @param lipid the parent item, which is usually an element of the list set to this
     *                   {@link ListWithSubsType}
     * @return The specific information each column contains.
     * @param <K> The type of keys maintained by the map.
     */
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
        }else if (subType instanceof LipidValidationAdductsType)  {
            return (K) lipid.getAdducts();
        } else {
            throw new UnsupportedOperationException(
                    "DataType %s is not covered in map".formatted(subType.toString())
            );
        }
    }


    @Override
    public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
                          @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
                          @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
        if (value == null) {
            return;
        }

        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException(
                    "Wrong value type for data type: " + this.getClass().getName() +
                            " value class: " + value.getClass());
        }

        for (Object o : list) {
            if (!(o instanceof FoundLipid foundLipid)) {
                continue;
            }

            foundLipid.saveToXML(writer, flist, row);
        }
    }


    @Override
    public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
                              @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
                              @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

        // Ensure the current element matches the expected data type
        if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
                && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
            throw new IllegalStateException("Wrong element or invalid data type ID");
        }

        // List to hold FoundLipid objects
        List<FoundLipid> foundLipids = new ArrayList<>();

        // Process XML and add FoundLipid elements to the list
        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
                .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
            reader.next();
            if (!reader.isStartElement()) {
                continue;
            }

            if (reader.getLocalName().equals(FoundLipid.XML_ELEMENT)) {
                // Load a FoundLipid object from XML (without needing `getPossibleFiles` from flist)
                FoundLipid foundLipid = FoundLipid.loadFromXML(reader, project.getCurrentRawDataFiles());
                foundLipids.add(foundLipid);
            }
        }

        // Ensure to never return null, even if no items were found
        return foundLipids;
    }

    /**
     * Sets the visibility of this column.
     * True = appears by default, False = does not appear by default.
     * @return True value.
     */
    @Override
    public boolean getDefaultVisibility() {
        return true;
    }
}
