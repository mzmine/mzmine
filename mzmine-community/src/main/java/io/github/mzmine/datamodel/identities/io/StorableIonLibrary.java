/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities.io;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPartSorting;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonTypeUtils;
import io.github.mzmine.datamodel.identities.iontype.LibraryOrigin;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used for storage in both JSON and XML. Jackson-XML annotations make the XML shape explicit
 * (attributes for scalars, wrapped element lists for collections); plain Jackson JSON ignores the
 * XML annotations and renders with default field names.
 *
 * @param id       stable identifier for this library
 * @param origin   where the library came from ({@link LibraryOrigin})
 * @param name     the library name
 * @param parts    all part definitions without count — name, formula, charge, mass + a unique id
 *                 referenced from {@link IonTypeDTO#parts}.
 * @param ionTypes ion types that reference parts by id.
 */
@JacksonXmlRootElement(localName = "ionLibrary")
@JsonInclude(JsonInclude.Include.NON_NULL)
record StorableIonLibrary(@JacksonXmlProperty(isAttribute = true)//
                          @NotNull UUID id, //
                          @JacksonXmlProperty(isAttribute = true) //
                          @NotNull LibraryOrigin origin, //
                          @JacksonXmlProperty(isAttribute = true) //
                          @NotNull String name, //
                          @JacksonXmlProperty(isAttribute = true) //
                          @JsonDeserialize(using = LocalDateTimeDeserializer.class) //
                          @JsonSerialize(using = LocalDateTimeSerializer.class) //
                          @NotNull LocalDateTime savedDate, //
                          @JacksonXmlElementWrapper(localName = "parts") //
                          @JacksonXmlProperty(localName = "part") //
                          @NotNull List<IonPartDTO> parts, //
                          @JacksonXmlElementWrapper(localName = "ionTypes") //
                          @JacksonXmlProperty(localName = "ionType") //
                          @NotNull List<IonTypeDTO> ionTypes) {

  StorableIonLibrary {
    if (parts == null) {
      parts = List.of();
    }
    if (ionTypes == null) {
      ionTypes = List.of();
    }
  }

  public static @NotNull StorableIonLibrary of(@NotNull IonLibrary library) {
    final List<IonPart> allParts = new ArrayList<>(IonTypeUtils.extractUniqueParts(library.ions()));
    allParts.sort(IonPartSorting.DEFAULT_NEUTRAL_THEN_LOSSES_THEN_ADDED.getComparator());

    // assign a stable id to each unique no-count part (H+ and 2H+ share an id)
    final Map<IonPartDTO, Integer> idByNoCount = HashMap.newHashMap(allParts.size());
    final List<IonPartDTO> parts = new ArrayList<>(allParts.size());
    int nextId = 0;
    for (IonPart part : allParts) {
      final IonPartDTO noCount = noCountKey(part);
      if (idByNoCount.putIfAbsent(noCount, nextId) == null) {
        parts.add(new IonPartDTO(nextId, noCount.name(), noCount.formula(), noCount.mass(),
            noCount.charge()));
        nextId++;
      }
    }

    final List<IonTypeDTO> types = library.ions().stream().map(ion -> new IonTypeDTO(
        ion.parts().stream().map(p -> new IonPartID(idByNoCount.get(noCountKey(p)), p.count()))
            .toList(), ion.molecules())).toList();

    return new StorableIonLibrary(library.id(), library.origin(), library.name(),
        LocalDateTime.now(), parts, types);
  }

  private static IonPartDTO noCountKey(IonPart part) {
    // id is irrelevant for equality (see IonPartDTO.equals)
    return new IonPartDTO(-1, part.name(), part.singleFormula(), part.absSingleMass(),
        part.singleCharge());
  }

  /**
   * Part definition without count, carrying its stable id. Equality ignores the id so callers can
   * detect duplicate definitions regardless of assigned id.
   */
  record IonPartDTO(@JacksonXmlProperty(isAttribute = true) int id,
                    @JacksonXmlProperty(isAttribute = true) @NotNull String name,
                    @JacksonXmlProperty(isAttribute = true) @Nullable String formula,
                    @JacksonXmlProperty(isAttribute = true) double mass,
                    @JacksonXmlProperty(isAttribute = true) int charge) {

    @NotNull
    @JsonIgnore
    public IonPart withCount(int count) {
      return IonParts.create(name, formula, mass, charge, count);
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof IonPartDTO other)) {
        return false;
      }
      return Double.compare(mass, other.mass) == 0 && charge == other.charge && name.equals(
          other.name) && (formula == null ? other.formula == null : formula.equals(other.formula));
    }

    @Override
    public int hashCode() {
      int h = name.hashCode();
      h = 31 * h + (formula == null ? 0 : formula.hashCode());
      h = 31 * h + Double.hashCode(mass);
      h = 31 * h + charge;
      return h;
    }
  }

  /**
   * @param parts     ids that reference entries in {@link StorableIonLibrary#parts}
   * @param molecules num molecules
   */
  record IonTypeDTO(@JacksonXmlElementWrapper(useWrapping = false) //
                    @JacksonXmlProperty(localName = "part") //
                    @NotNull List<@NotNull IonPartID> parts,
                    @JacksonXmlProperty(isAttribute = true) int molecules) {

  }

  /**
   * References an {@link IonPartDTO} by id with the count.
   */
  record IonPartID(@JacksonXmlProperty(isAttribute = true) int id,
                   @JacksonXmlProperty(isAttribute = true) int count) {

  }
}
