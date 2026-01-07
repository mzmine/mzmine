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

package io.github.mzmine.modules.dataprocessing.id_online_reactivity;

import static java.util.stream.Collectors.groupingBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OnlineReactionJsonWriter {

  private static final Logger logger = Logger.getLogger(OnlineReactionJsonWriter.class.getName());
  private final ObjectWriter writer;

  public OnlineReactionJsonWriter(final boolean prettyPrint) {
    ObjectMapper mapper = new ObjectMapper();
    if (prettyPrint) {
      writer = mapper.writerWithDefaultPrettyPrinter();
    } else {
      writer = mapper.writer();
    }
  }

  private static void addAll(
      final Map<Type, Map<OnlineReaction, List<OnlineReactionMatch>>> groupedByTypeAndReaction,
      final Type type, final List<OnlineReactionMatchExportDto> reactionsSorted) {

    Map<OnlineReaction, List<OnlineReactionMatch>> groupedByType = groupedByTypeAndReaction.get(
        type);
    if (groupedByType == null) {
      return;
    }

    groupedByType.entrySet().stream()
        .sorted(Comparator.comparing(entry -> entry.getKey().deltaMz())).forEach(entry -> {
          int[] partnerIds = entry.getValue().stream().mapToInt(OnlineReactionMatch::getPartnerRowId)
              .sorted().toArray();
          reactionsSorted.add(new OnlineReactionMatchExportDto(type, entry.getKey(), partnerIds));
        });
  }

  @Nullable
  public String createReactivityString(final @NotNull FeatureListRow row,
      final List<OnlineReactionMatch> matches) {
    if (matches.isEmpty()) {
      return null;
    }
    List<OnlineReactionMatchExportDto> reactionsSorted = new ArrayList<>();

    // group by Educt or Product type and then by reaction
    Map<Type, Map<OnlineReaction, List<OnlineReactionMatch>>> groupedByTypeAndReaction = matches.stream()
        .collect(groupingBy(match -> match.getTypeOfRow(row),
            groupingBy(OnlineReactionMatch::getReaction)));

    addAll(groupedByTypeAndReaction, Type.Educt, reactionsSorted);
    addAll(groupedByTypeAndReaction, Type.Product, reactionsSorted);

    try {
      return writer.writeValueAsString(reactionsSorted);
    } catch (JsonProcessingException e) {
      logger.log(Level.WARNING, "Could not write reaction to json ", e);
      return null;
    }
  }

}
