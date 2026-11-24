/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 */

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainFactory;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Lipid annotation with chain composition and at least one fully localized unsaturated chain.
 * <p>
 * Positions are stored on the {@link ILipidChain} instances. The position-level shorthand and its
 * molecular-species fallback are both retained so callers can render a requested lower annotation
 * level without reparsing the name. Chain separators are always underscores because double-bond
 * localization does not establish sn-position.
 * <p>
 * Equality is based on the complete position-level shorthand. That string includes lipid class,
 * chain composition, localized positions, geometry, and oxygen suffixes, making it the stable
 * identity used by annotation deduplication.
 */
public final class DoubleBondPositionLevelAnnotation implements IChainedLipidAnnotation {

  private static final String XML_ELEMENT = "lipidannotation";
  private static final String XML_LIPID_CLASS = "lipidclass";
  private static final String XML_NAME = "name";
  private static final String XML_LEVEL = "lipidannotationlevel";
  private static final String XML_MOLECULAR_SPECIES_NAME = "molecularspecieslevelname";
  private static final String XML_FORMULA = "molecularformula";
  private static final String XML_CHAINS = "lipidchains";

  private final @NotNull ILipidClass lipidClass;
  private final @NotNull String annotation;
  private final @NotNull String molecularSpeciesLevelAnnotation;
  private final @NotNull IMolecularFormula molecularFormula;
  private final @NotNull List<ILipidChain> lipidChains;

  public DoubleBondPositionLevelAnnotation(final @NotNull ILipidClass lipidClass,
      final @NotNull String annotation, final @NotNull String molecularSpeciesLevelAnnotation,
      final @NotNull IMolecularFormula molecularFormula,
      final @NotNull List<ILipidChain> lipidChains) {
    this.lipidClass = lipidClass;
    this.annotation = annotation;
    this.molecularSpeciesLevelAnnotation = molecularSpeciesLevelAnnotation;
    this.molecularFormula = molecularFormula;
    this.lipidChains = List.copyOf(lipidChains);
  }

  @Override
  public @NotNull ILipidClass getLipidClass() {
    return lipidClass;
  }

  @Override
  public @NotNull String getAnnotation() {
    return annotation;
  }

  @Override
  public @NotNull String getMolecularSpeciesLevelAnnotation() {
    return molecularSpeciesLevelAnnotation;
  }

  @Override
  public @NotNull LipidAnnotationLevel getLipidAnnotationLevel() {
    return LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL;
  }

  @Override
  public @NotNull IMolecularFormula getMolecularFormula() {
    return molecularFormula;
  }

  @Override
  public @NotNull List<ILipidChain> getLipidChains() {
    return lipidChains;
  }

  @Override
  public int getChainsCarbonCount() {
    return lipidChains.stream().mapToInt(ILipidChain::getNumberOfCarbons).sum();
  }

  @Override
  public int getChainsDoubleBondCount() {
    return lipidChains.stream().mapToInt(ILipidChain::getNumberOfDBEs).sum();
  }

  @Override
  public int getSpeciesLevelOxygens() {
    return lipidChains.stream().mapToInt(ILipidChain::getNumberOfOxygens).sum();
  }

  @Override
  public void saveToXML(final @NotNull XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(XML_ELEMENT);
    writer.writeAttribute(XML_ELEMENT, getLipidAnnotationLevel().name());
    lipidClass.saveToXML(writer);
    writeTextElement(writer, XML_NAME, annotation);
    writeTextElement(writer, XML_LEVEL, getLipidAnnotationLevel().name());
    writeTextElement(writer, XML_MOLECULAR_SPECIES_NAME, molecularSpeciesLevelAnnotation);
    writeTextElement(writer, XML_FORMULA, FormulaUtils.getFormulaString(molecularFormula));
    writer.writeStartElement(XML_CHAINS);
    for (final ILipidChain lipidChain : lipidChains) {
      lipidChain.saveToXML(writer);
    }
    writer.writeEndElement();
    writer.writeEndElement();
  }

  private static void writeTextElement(final @NotNull XMLStreamWriter writer,
      final @NotNull String element, final @NotNull String value) throws XMLStreamException {
    writer.writeStartElement(element);
    writer.writeCharacters(value);
    writer.writeEndElement();
  }

  public static @Nullable DoubleBondPositionLevelAnnotation loadFromXML(
      final @NotNull XMLStreamReader reader) throws XMLStreamException {
    if (!(reader.isStartElement() && XML_ELEMENT.equals(reader.getLocalName()))) {
      throw new XMLStreamException("Expected a lipid annotation element.");
    }
    ILipidClass lipidClass = null;
    String annotation = null;
    String molecularSpeciesName = null;
    IMolecularFormula formula = null;
    final List<ILipidChain> chains = new ArrayList<>();
    LipidAnnotationLevel level = null;
    while (reader.hasNext() && !(reader.isEndElement() && XML_ELEMENT.equals(
        reader.getLocalName()))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case XML_LIPID_CLASS -> {
          lipidClass = LipidClasses.loadFromXML(reader);
          if (lipidClass == null) {
            lipidClass = CustomLipidClass.loadFromXML(reader);
          }
        }
        case XML_NAME -> annotation = reader.getElementText();
        case XML_LEVEL -> level = LipidAnnotationLevel.valueOf(reader.getElementText());
        case XML_MOLECULAR_SPECIES_NAME -> molecularSpeciesName = reader.getElementText();
        case XML_FORMULA -> formula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
            reader.getElementText());
        case XML_CHAINS -> {
        }
        case "lipidchain" -> chains.add(LipidChainFactory.loadLipidChainFromXML(reader));
        default -> {
        }
      }
    }
    if (level != LipidAnnotationLevel.DOUBLE_BOND_POSITION_LEVEL || lipidClass == null
        || annotation == null || molecularSpeciesName == null || formula == null) {
      return null;
    }
    return new DoubleBondPositionLevelAnnotation(lipidClass, annotation, molecularSpeciesName,
        formula, chains);
  }

  @Override
  public boolean equals(final Object object) {
    return this == object || object instanceof DoubleBondPositionLevelAnnotation other
        && annotation.equals(other.annotation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotation);
  }
}
