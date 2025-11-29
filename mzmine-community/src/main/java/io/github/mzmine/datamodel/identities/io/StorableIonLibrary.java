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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.github.mzmine.datamodel.identities.IonLibrary;
import io.github.mzmine.datamodel.identities.IonPart;
import io.github.mzmine.datamodel.identities.IonPartSorting;
import io.github.mzmine.datamodel.identities.IonType;
import io.github.mzmine.datamodel.identities.IonTypeUtils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Only used for storage.
 *
 * @param name     the library name
 * @param parts    all part definitions without count just name, formula, charge, mass + a unique ID
 *                 that is used in types.
 * @param ionTypes ion types that use unique ids to refer to the ion parts
 */

record StorableIonLibrary(@NotNull String name, //
                          @JsonDeserialize(using = LocalDateTimeDeserializer.class) //
                          @JsonSerialize(using = LocalDateTimeSerializer.class) //
                          @NotNull LocalDateTime savedDate, //
                          @JsonDeserialize(as = LinkedHashMap.class) @NotNull Map<Integer, IonPartNoCountDTO> parts,
                          @NotNull List<IonTypeDTO> ionTypes) {

  public StorableIonLibrary(@NotNull IonLibrary library) {
    // reduce parts to a list of unique parts without counts
    final List<IonPart> allParts = new ArrayList<>(IonTypeUtils.extractUniqueParts(library.ions()));
    allParts.sort(IonPartSorting.DEFAULT_NEUTRAL_THEN_LOSSES_THEN_ADDED.getComparator());

    int nextID = 0;
    final Map<IonPart, IonPartNoCountDTO> partMapping = HashMap.newHashMap(allParts.size());
    final Map<IonPartNoCountDTO, Integer> uniquePartIds = HashMap.newHashMap(allParts.size());
    // the final map to use
    final Map<Integer, IonPartNoCountDTO> parts = LinkedHashMap.newHashMap(allParts.size());

    for (IonPart part : allParts) {
      // use same id for now to check if this part is already added (may be multiple parts with different counts)
      final IonPartNoCountDTO noCount = new IonPartNoCountDTO(part);
      // H+ and 2H+ will point to the same value
      partMapping.put(part, noCount);
      if (!uniquePartIds.containsKey(noCount)) {
        uniquePartIds.put(noCount, nextID);
        parts.put(nextID, noCount);
        nextID++;
      }
    }

    // map types to just the part ids
    final List<IonTypeDTO> types = library.ions().stream()
        .map(ion -> IonTypeDTO.createIonTypeDTO(ion, uniquePartIds, partMapping)).toList();

    this(library.name(), LocalDateTime.now(), parts, types);
  }

  /**
   * Used to reduce the parts that have different count to single instances
   *
   */
  record IonPartNoCountDTO(@NotNull String name, @Nullable String formula, double mass,
                           int charge) {

    public IonPartNoCountDTO(@NotNull IonPart p) {
      this(p.name(), p.singleFormula(), p.absSingleMass(), p.singleCharge());
    }

    @NotNull
    @JsonIgnore
    public IonPart withCount(int count) {
      return new IonPart(name, formula, mass, charge, count);
    }
  }

  /**
   *
   * @param parts     just the IDs that need to be mapped to the real ion parts
   * @param molecules num molecules
   */
  record IonTypeDTO(@NotNull List<@NotNull IonPartID> parts, int molecules) {

    static @NotNull IonTypeDTO createIonTypeDTO(IonType ion,
        Map<IonPartNoCountDTO, Integer> uniquePartIds,
        Map<IonPart, IonPartNoCountDTO> partMapping) {
      final List<IonPartID> partIds = ion.parts().stream()
          .map(part -> new IonPartID(uniquePartIds.get(partMapping.get(part)), part.count()))
          .toList();
      return new IonTypeDTO(partIds, ion.molecules());
    }
  }

  /**
   * Identifies the ion part by ID
   */
  record IonPartID(int id, int count) {

  }
}
