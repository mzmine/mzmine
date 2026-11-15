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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain;

import java.util.List;
import java.util.Objects;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Immutable position-aware view of a conventional {@link LipidChain}.
 * <p>
 * Carbon count, DBE count, oxygen count, chain type, and molecular formula are delegated because
 * localization does not change composition. This wrapper only adds a sorted position list and its
 * position-level shorthand. Construction is package-private so {@link LipidChainFactory} enforces
 * the invariant that every DBE has one distinct position.
 */
public final class DoubleBondPositionLipidChain implements ILipidChain {

  static final String XML_POSITIONS = "doublebondpositions";

  private final @NotNull LipidChain delegate;
  private final @NotNull List<DoubleBondPosition> positions;
  private final @NotNull String chainAnnotation;

  DoubleBondPositionLipidChain(final @NotNull LipidChain delegate,
      final @NotNull List<DoubleBondPosition> positions, final @NotNull String chainAnnotation) {
    this.delegate = delegate;
    this.positions = positions.stream().sorted().toList();
    this.chainAnnotation = chainAnnotation;
    if (!hasFullyLocalizedDoubleBonds()) {
      throw new IllegalArgumentException("Every double bond must have one distinct position.");
    }
  }

  @Override
  public @NotNull String getChainAnnotation() {
    return chainAnnotation;
  }

  @Override
  public int getNumberOfCarbons() {
    return delegate.getNumberOfCarbons();
  }

  @Override
  public int getNumberOfDBEs() {
    return delegate.getNumberOfDBEs();
  }

  @Override
  public @NotNull IMolecularFormula getChainMolecularFormula() {
    return delegate.getChainMolecularFormula();
  }

  @Override
  public @NotNull LipidChainType getLipidChainType() {
    return delegate.getLipidChainType();
  }

  @Override
  public int getNumberOfOxygens() {
    return delegate.getNumberOfOxygens();
  }

  @Override
  public @NotNull List<DoubleBondPosition> getDoubleBondPositions() {
    return positions;
  }

  @Override
  public @NotNull ILipidChain withoutDoubleBondPositions() {
    return delegate;
  }

  @Override
  public void saveToXML(final @NotNull XMLStreamWriter writer) throws XMLStreamException {
    writer.writeStartElement(LipidChainFactory.XML_CHAIN_ELEMENT);
    LipidChainFactory.writeChainBody(writer, this);
    writer.writeStartElement(XML_POSITIONS);
    for (final DoubleBondPosition position : positions) {
      position.saveToXML(writer);
    }
    writer.writeEndElement();
    writer.writeEndElement();
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DoubleBondPositionLipidChain other)) {
      return false;
    }
    return getNumberOfCarbons() == other.getNumberOfCarbons()
        && getNumberOfDBEs() == other.getNumberOfDBEs()
        && getNumberOfOxygens() == other.getNumberOfOxygens()
        && getLipidChainType() == other.getLipidChainType()
        && chainAnnotation.equals(other.chainAnnotation) && positions.equals(other.positions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chainAnnotation, positions, getNumberOfCarbons(), getNumberOfDBEs(),
        getNumberOfOxygens(), getLipidChainType());
  }
}
