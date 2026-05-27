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

package io.github.mzmine.datamodel.features.types.numbers.scores;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Similarity score produced by an ML model (MS2Deepscore or DREAMS). Carries both the numeric score
 * and the model identifier so downstream code can retrieve the score without knowing which
 * algorithm produced it.
 */
public record MLScore(float score, @NotNull MLModelId model) {

  public static final String XML_ELEMENT = "ml_score";
  private static final String XML_SCORE_ATTR = "score";
  private static final String XML_MODEL_ATTR = "model";

  public MLScore {
    if (model == null) {
      throw new IllegalArgumentException("MLScore.model must not be null");
    }
  }

  public void saveToXML(@NotNull XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_SCORE_ATTR, Float.toString(score));
    writer.writeAttribute(XML_MODEL_ATTR, model.getUniqueID());
    writer.writeEndElement();
  }

  @Nullable
  public static MLScore loadFromXML(@NotNull XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(XML_ELEMENT))) {
      throw new IllegalStateException("Expected <" + XML_ELEMENT + "> element");
    }
    final String scoreStr = reader.getAttributeValue(null, XML_SCORE_ATTR);
    final String modelStr = reader.getAttributeValue(null, XML_MODEL_ATTR);
    if (scoreStr == null || modelStr == null) {
      return null;
    }
    final MLModelId model = UniqueIdSupplier.parseOrElse(modelStr, MLModelId.values(), null);
    if (model == null) {
      return null;
    }
    return new MLScore(Float.parseFloat(scoreStr), model);
  }

  @Override
  public String toString() {
    return String.format("%.3f (%s)", score, model.labelVersion());
  }
}
