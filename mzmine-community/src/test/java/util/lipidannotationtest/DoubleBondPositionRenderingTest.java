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

package util.lipidannotationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.DoubleBondPositionLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.DoubleBondGeometry;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.DoubleBondPosition;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.DoubleBondPositionLipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;

class DoubleBondPositionRenderingTest {

  private static final LipidChainFactory CHAIN_FACTORY = new LipidChainFactory();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  @Test
  void rendersUnknownAndKnownGeometry() {
    assertChain("18:1(9)", LipidChainType.ACYL_CHAIN, 18, 1,
        List.of(DoubleBondPosition.unknownGeometry(9)));
    assertChain("18:1(9Z)", LipidChainType.ACYL_CHAIN, 18, 1,
        List.of(new DoubleBondPosition(9, DoubleBondGeometry.Z)));
  }

  @Test
  void sortsPositionsAndPlacesThemBeforeOxygenSuffix() {
    assertChain("18:2(9Z,12Z);O2", LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN,
        18, 2, List.of(new DoubleBondPosition(12, DoubleBondGeometry.Z),
            new DoubleBondPosition(9, DoubleBondGeometry.Z)));
    assertChain("O-18:1(9)", LipidChainType.ALKYL_CHAIN, 18, 1,
        List.of(DoubleBondPosition.unknownGeometry(9)));
    assertChain("18:1(9);O", LipidChainType.ACYL_MONO_HYDROXY_CHAIN, 18, 1,
        List.of(DoubleBondPosition.unknownGeometry(9)));
  }

  @Test
  void partialLocalizationFallsBackToPlainChain() {
    final ILipidChain chain = CHAIN_FACTORY.buildLipidChain(LipidChainType.ACYL_CHAIN, 18, 2,
        List.of(DoubleBondPosition.unknownGeometry(9)));
    assertEquals("18:2", chain.getChainAnnotation());
  }

  @Test
  void positionLevelAlwaysUsesUnderscore() {
    final ILipidChain first = localizedAcyl(18, 1, 9);
    final ILipidChain second = localizedAcyl(18, 1, 9);
    final DoubleBondPositionLevelAnnotation annotation =
        LIPID_FACTORY.buildDoubleBondPositionLevelLipidFromChains(
            LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, new ArrayList<>(List.of(first, second)));
    assertEquals("PC 18:1(9)_18:1(9)", annotation.getAnnotation());
    assertEquals("PC 18:1/18:1", annotation.getMolecularSpeciesLevelAnnotation());
  }

  @Test
  void saturatedChainsCannotCreatePositionLevelAnnotation() {
    final List<ILipidChain> chains = new ArrayList<>(List.of(
        CHAIN_FACTORY.buildLipidChain(LipidChainType.ACYL_CHAIN, 16, 0),
        CHAIN_FACTORY.buildLipidChain(LipidChainType.ACYL_CHAIN, 18, 0)));
    assertNull(LIPID_FACTORY.buildDoubleBondPositionLevelLipidFromChains(
        LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, chains));
  }

  @Test
  void positionChainXmlRoundTripKeepsFixedOxygensAndGeometry() throws Exception {
    final ILipidChain original = CHAIN_FACTORY.buildLipidChain(
        LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, 18, 1,
        List.of(new DoubleBondPosition(4, DoubleBondGeometry.E)));
    final StringWriter xml = new StringWriter();
    final XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(xml);
    original.saveToXML(writer);
    writer.close();
    final XMLStreamReader reader = XMLInputFactory.newFactory()
        .createXMLStreamReader(new StringReader(xml.toString()));
    reader.nextTag();

    final ILipidChain loaded = LipidChainFactory.loadLipidChainFromXML(reader);

    assertEquals(2, loaded.getNumberOfOxygens());
    assertEquals("18:1(4E);O2", loaded.getChainAnnotation());
    assertEquals(original.getDoubleBondPositions(), loaded.getDoubleBondPositions());
  }

  private static void assertChain(final String expected, final LipidChainType type,
      final int carbons, final int doubleBonds, final List<DoubleBondPosition> positions) {
    final ILipidChain chain = CHAIN_FACTORY.buildLipidChain(type, carbons, doubleBonds, positions);
    assertInstanceOf(DoubleBondPositionLipidChain.class, chain);
    assertEquals(expected, chain.getChainAnnotation());
  }

  private static ILipidChain localizedAcyl(final int carbons, final int doubleBonds,
      final int position) {
    return CHAIN_FACTORY.buildLipidChain(LipidChainType.ACYL_CHAIN, carbons, doubleBonds,
        List.of(DoubleBondPosition.unknownGeometry(position)));
  }
}
