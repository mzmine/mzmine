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
 */

package datamodel;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundFeature;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.modules.io.projectload.version_3_0.FeatureListLoadTask;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javafx.scene.paint.Color;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Round-trip persistence tests for {@link CompoundList}: build a CompoundList in memory, write the
 * {@code <compoundlist>} XML block via {@link FeatureListSaveTask#saveCompoundList}, then parse it
 * back via {@link FeatureListLoadTask#parseCompoundList} into a fresh {@link CompoundList}. Asserts
 * compound id, member resolution (source rows + nested compound rows), preferred row identity,
 * confidence/neutral mass, and compound feature values.
 * <p>
 * Bindings are intentionally disabled (empty list passed to the {@link CompoundList} constructor)
 * so the saved schema values are exactly what the test wrote — no listener-driven recomputation
 * mixed in.
 */
public class CompoundListSaveLoadTest {

  private RawDataFileImpl fileA;
  private RawDataFileImpl fileB;
  private ModularFeatureList flist;
  private MZmineProject project;
  private ModularFeatureListRow source1;
  private ModularFeatureListRow source2;
  private ModularFeatureListRow source3;
  private ModularFeatureListRow source4;

  void setUp() {
    fileA = new RawDataFileImpl("file_a", null, null, Color.BLACK);
    fileB = new RawDataFileImpl("file_b", null, null, Color.BLACK);
    flist = new ModularFeatureList("test_flist", null, fileA, fileB);
    project = new MZmineProjectImpl();
    project.addFile(fileA);
    project.addFile(fileB);
    project.addFeatureList(flist);

    source1 = addSourceRow(1, 100.1);
    source2 = addSourceRow(2, 200.2);
    source3 = addSourceRow(3, 300.3);
    source4 = addSourceRow(4, 400.4);
  }

  private ModularFeatureListRow addSourceRow(int id, double mz) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    row.set(MZType.class, mz);
    flist.addRow(row);
    return row;
  }

  @Test
  void testSimpleRoundTrip() {
    setUp();

    // bindings disabled → the saved values are exactly what we set
    final CompoundList cl = new CompoundList(flist, null, 4, List.of());

    final ModularCompoundRow cr = new ModularCompoundRow(cl, 42, source1,
        List.of(new CompoundFeatureMember(source1, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(source2, CompoundMemberRole.ADDUCT, 0.91f)),
        0.83f, 317.0532);

    // attach a compound feature with its own area/height (compound row schema)
    final ModularCompoundFeature cfA = new ModularCompoundFeature(cl, cr, fileA);
    cfA.setOwnValue(DataTypes.get(AreaType.class), 1.20e5f);
    cfA.setOwnValue(DataTypes.get(HeightType.class), 3.40e4f);
    cr.addFeature(fileA, cfA, false);

    cl.setRows(List.of(cr));
    flist.setCompoundList(cl);

    // round trip
    final CompoundList loaded = roundTrip(cl);

    Assertions.assertEquals(1, loaded.size(), "loaded compound count");
    final ModularCompoundRow lcr = loaded.findRowByCompoundId(42);
    Assertions.assertNotNull(lcr, "compound 42 must be loaded");
    Assertions.assertEquals(42, lcr.getCompoundId());
    Assertions.assertEquals(0.83f, lcr.getCompoundConfidenceScore(), 1e-6f);
    Assertions.assertEquals(317.0532, lcr.getCompoundNeutralMass(), 1e-9);

    // preferred row should resolve back to source1 (same identity by id, but the row instance
    // must come from the same flist)
    Assertions.assertSame(source1, lcr.getPreferredRow(),
        "preferred row must resolve to the same source row instance");

    final List<CompoundFeatureMember> members = lcr.getCompoundMembers();
    Assertions.assertEquals(2, members.size(), "member count");
    Assertions.assertSame(source1, members.get(0).row());
    Assertions.assertEquals(CompoundMemberRole.REPRESENTATIVE, members.get(0).role());
    Assertions.assertEquals(1.0f, members.get(0).score(), 1e-6f);
    Assertions.assertSame(source2, members.get(1).row());
    Assertions.assertEquals(CompoundMemberRole.ADDUCT, members.get(1).role());
    Assertions.assertEquals(0.91f, members.get(1).score(), 1e-6f);

    // compound feature for fileA was saved → should reload via getOwnFeature
    final ModularCompoundFeature loadedCfA = lcr.getOwnFeature(fileA);
    Assertions.assertNotNull(loadedCfA, "compound feature for fileA should round-trip");
    Assertions.assertEquals(1.20e5f, loadedCfA.getOwnValue(DataTypes.get(AreaType.class)), 1e-3f);
    Assertions.assertEquals(3.40e4f, loadedCfA.getOwnValue(DataTypes.get(HeightType.class)),
        1e-3f);

    // no compound feature for fileB
    Assertions.assertNull(lcr.getOwnFeature(fileB),
        "compound feature for fileB was never written; should remain null after load");

    fileA.close();
    fileB.close();
  }

  @Test
  void testNestedCompoundRoundTrip() {
    setUp();

    final CompoundList cl = new CompoundList(flist, null, 4, List.of());

    // inner compound (id=10): preferred = source1, members = source1+source2
    final ModularCompoundRow inner = new ModularCompoundRow(cl, 10, source1,
        List.of(new CompoundFeatureMember(source1, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(source2, CompoundMemberRole.ADDUCT, 0.9f)),
        0.7f, null);

    // outer compound (id=20): preferred = inner compound, members = inner compound + source3
    final ModularCompoundRow outer = new ModularCompoundRow(cl, 20, inner,
        List.of(new CompoundFeatureMember(inner, CompoundMemberRole.REPRESENTATIVE, 1.0f),
            new CompoundFeatureMember(source3, CompoundMemberRole.ADDUCT, 0.6f)),
        0.5f, 500.0);

    // intentionally save the outer first to exercise forward-reference handling: outer's <member
    // compound="true" id="10"> appears before inner's <compoundrow id="10">
    cl.setRows(List.of(outer, inner));
    flist.setCompoundList(cl);

    final CompoundList loaded = roundTrip(cl);

    Assertions.assertEquals(2, loaded.size(), "loaded compound count");
    final ModularCompoundRow loadedInner = loaded.findRowByCompoundId(10);
    final ModularCompoundRow loadedOuter = loaded.findRowByCompoundId(20);
    Assertions.assertNotNull(loadedInner, "inner compound 10 must be loaded");
    Assertions.assertNotNull(loadedOuter, "outer compound 20 must be loaded");

    // outer's preferred row is the inner compound (same identity as the loaded inner)
    Assertions.assertSame(loadedInner, loadedOuter.getPreferredRow(),
        "outer's preferred row must be the loaded inner compound");

    // outer's members: 0 = loaded inner, 1 = source3
    final List<CompoundFeatureMember> outerMembers = loadedOuter.getCompoundMembers();
    Assertions.assertEquals(2, outerMembers.size());
    Assertions.assertSame(loadedInner, outerMembers.get(0).row(),
        "outer's first member must resolve to the loaded inner compound");
    Assertions.assertEquals(CompoundMemberRole.REPRESENTATIVE, outerMembers.get(0).role());
    Assertions.assertSame(source3, outerMembers.get(1).row(),
        "outer's second member must resolve to source row 3");

    // inner's preferred + members are source rows
    Assertions.assertSame(source1, loadedInner.getPreferredRow());
    Assertions.assertSame(source1, loadedInner.getCompoundMembers().get(0).row());
    Assertions.assertSame(source2, loadedInner.getCompoundMembers().get(1).row());

    Assertions.assertEquals(500.0, loadedOuter.getCompoundNeutralMass(), 1e-9);
    Assertions.assertNull(loadedInner.getCompoundNeutralMass(),
        "inner compound never had neutral mass set; should remain null after load");

    fileA.close();
    fileB.close();
  }

  /**
   * Save the compound list block to a byte stream, then parse it back into a fresh CompoundList on
   * the same {@link #flist}. Returns the freshly loaded CompoundList.
   */
  private CompoundList roundTrip(CompoundList original) {
    final byte[] bytes = saveToBytes(original);
    return loadFromBytes(bytes);
  }

  private byte[] saveToBytes(CompoundList cl) {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      final XMLOutputFactory xof = XMLOutputFactory.newInstance();
      final XMLStreamWriter writer = new IndentingXMLStreamWriter(
          xof.createXMLStreamWriter(os, "UTF-8"));
      writer.writeStartDocument("UTF-8", "1.0");
      writer.writeStartElement(CONST.XML_FEATURE_LIST_ELEMENT);
      // saveCompoundList only writes the <compoundlist> child; the <featurelist> wrapper is here
      // to mirror the production XML layout the loader expects.
      FeatureListSaveTask.saveCompoundList(writer, flist, cl);
      writer.writeEndElement(); // featurelist
      writer.writeEndDocument();
      writer.flush();
      writer.close();
    } catch (Exception e) {
      Assertions.fail("Failed to save compound list: " + e.getMessage());
    }
    return os.toByteArray();
  }

  private CompoundList loadFromBytes(byte[] bytes) {
    // wipe the existing compound list — load must rebuild it from XML alone
    flist.setCompoundList(null);

    try (final ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
      final XMLInputFactory xif = XMLInputFactory.newInstance();
      final XMLStreamReader reader = xif.createXMLStreamReader(is);

      // walk to the <compoundlist> START_ELEMENT
      while (reader.hasNext()) {
        final int event = reader.next();
        if (event == XMLEvent.START_ELEMENT
            && CONST.XML_COMPOUND_LIST_ELEMENT.equals(reader.getLocalName())) {
          break;
        }
      }
      Assertions.assertTrue(
          reader.getEventType() == XMLEvent.START_ELEMENT
              && CONST.XML_COMPOUND_LIST_ELEMENT.equals(reader.getLocalName()),
          "expected to find <compoundlist> in saved XML");

      FeatureListLoadTask.parseCompoundList(reader, project, flist);
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.fail("Failed to load compound list: " + e.getMessage());
    }
    final CompoundList loaded = flist.getCompoundList();
    Assertions.assertNotNull(loaded, "flist.getCompoundList() must be non-null after load");
    return loaded;
  }
}
