/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed abstract class AbstractSpectralLibraryMatchesType extends
    ListWithSubsType<SpectralDBAnnotation> permits SpectralLibraryMatchesType {

  @Override
  protected <K> @Nullable K map(@NotNull final DataType<K> subType,
      final SpectralDBAnnotation match) {
    // for types that are only visible in table map them here
    // all other types are mapped in the match directly
    Object value = switch (subType) {
      case AnnotationSummaryType _ -> null; // created on demand in cell factory
      default -> null;
    };
    if (value != null) {
      return (K) value;
    }

    // just delegate to match.get now that is a ModularDataModel similar to CompoundDatabaseMatchesType
    return match.get(subType);
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
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }

    for (Object o : list) {
      if (!(o instanceof SpectralDBAnnotation id)) {
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

    List<FeatureAnnotation> ids = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      // todo remove first branch in a few versions so we can delete SpectralDBFeatureIdentity
      if (reader.getLocalName().equals(FeatureIdentity.XML_GENERAL_IDENTITY_ELEMENT)
          && reader.getAttributeValue(null, FeatureIdentity.XML_IDENTITY_TYPE_ATTR)
          .equals(SpectralDBFeatureIdentity.XML_IDENTITY_TYPE)) {
        FeatureIdentity id = FeatureIdentity.loadFromXML(reader, project,
            project.getCurrentRawDataFiles());
        ids.add(new SpectralDBAnnotation((SpectralDBFeatureIdentity) id));
      } else if (reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)
          && reader.getAttributeValue(null, FeatureAnnotation.XML_TYPE_ATTR)
          .equals(SpectralDBAnnotation.XML_ATTR)) {
        ids.add(SpectralDBAnnotation.loadFromXML(reader, project, flist, row,
            project.getCurrentRawDataFiles()));
      }
    }

    // never return null, if this type was saved we even need empty lists.
    return ids;
  }

  @Override
  public boolean getDefaultVisibility() {
    return false;
  }

  @Override
  public double getPrefColumnWidth() {
    return 150;
  }
}
