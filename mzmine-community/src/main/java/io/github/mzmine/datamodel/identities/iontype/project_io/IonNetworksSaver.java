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

package io.github.mzmine.datamodel.identities.iontype.project_io;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkNode;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.UnmodifiableIonLibrary;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * Writes {@link IonNetwork}s as XML without closing the caller owned output stream.
 * <p>
 * The distinct {@link IonType}s are written once as a library at the start of the file, and every
 * ion only references one of them by index instead of repeating all of its parts. The rest is row
 * major: every row lists its {@link IonIdentity}s in the order it holds them, and every ion names
 * its network. That way the order of a row's ion identities - which decides its best ion - is
 * simply the document order and needs no extra index. The ion identities are stored here because
 * they are not persisted anywhere else; a row gets them back from this file.
 */
public final class IonNetworksSaver {

  /**
   * Only used to sort and hold the ion types while writing, never written to the file itself.
   */
  private static final String LIBRARY_NAME = "ion identity networks of this feature list";

  private IonNetworksSaver() {
  }

  public static void save(@NotNull final Collection<IonNetwork> networks,
      @NotNull final OutputStream out) throws XMLStreamException {
    final XMLStreamWriter writer = new IndentingXMLStreamWriter(
        XMLOutputFactory.newInstance().createXMLStreamWriter(out, "UTF-8"));

    // stable order so that saving the same networks twice produces the same file, apart from the
    // id and save date that the ion library carries
    final List<IonNetwork> sortedNetworks = networks.stream()
        .sorted(Comparator.comparingInt(IonNetwork::getID)).toList();
    final Set<Integer> networkIds = new HashSet<>();
    for (final IonNetwork network : sortedNetworks) {
      networkIds.add(network.getID());
    }

    final Map<FeatureListRow, List<IonIdentity>> ionsByRow = collectIonsByRow(sortedNetworks,
        networkIds);
    final IonLibrary library = collectLibrary(ionsByRow);
    final String libraryJson = IonLibraryIO.toJson(library);

    // toJson writes the ion types in the order of ions(), so that position is the index an ion
    // references its ion type by and the loader gets back from LoadedIonLibrary.ionTypesByIndex
    final Map<IonType, Integer> libraryIndices = HashMap.newHashMap(library.getNumIons());
    for (int i = 0; i < library.getNumIons(); i++) {
      libraryIndices.put(library.ions().get(i), i);
    }

    writer.writeStartDocument("UTF-8", "1.0");
    writer.writeStartElement(IonNetworkXml.NETWORKS_ELEMENT);

    // the library has to come first, the ions below reference it by index
    writeLibrary(writer, libraryJson);
    for (final IonNetwork network : sortedNetworks) {
      writeNetwork(writer, network);
    }
    for (final Entry<FeatureListRow, List<IonIdentity>> entry : ionsByRow.entrySet()) {
      writeRow(writer, entry.getKey(), entry.getValue(), libraryIndices);
    }

    writer.writeEndElement();
    writer.writeEndDocument();
    // flush but never close, the stream belongs to the caller (the project zip)
    writer.flush();
  }

  /**
   * The ions to write per row, keyed by row and ordered by row ID for a reproducible file. Only
   * ions that belong to one of the saved networks can be restored, so only those are written.
   */
  private static Map<FeatureListRow, List<IonIdentity>> collectIonsByRow(
      @NotNull final List<IonNetwork> networks, @NotNull final Set<Integer> networkIds) {
    final List<FeatureListRow> rows = new ArrayList<>();
    final Set<Integer> seen = new HashSet<>();
    for (final IonNetwork network : networks) {
      for (final IonNetworkNode node : network.getNodes()) {
        if (seen.add(node.row().getID())) {
          rows.add(node.row());
        }
      }
    }
    rows.sort(Comparator.comparingInt(FeatureListRow::getID));

    final Map<FeatureListRow, List<IonIdentity>> ionsByRow = LinkedHashMap.newLinkedHashMap(
        rows.size());
    for (final FeatureListRow row : rows) {
      final List<IonIdentity> ions = new ArrayList<>();
      // the order of this list decides the best ion of the row, so keep it as it is
      for (final IonIdentity ion : row.getIonIdentities()) {
        final IonNetwork network = ion.getNetwork();
        if (network != null && networkIds.contains(network.getID())) {
          ions.add(ion);
        }
      }
      if (!ions.isEmpty()) {
        ionsByRow.put(row, ions);
      }
    }
    return ionsByRow;
  }

  /**
   * An {@link IonLibrary} of the distinct ion types of all written ions. Sorted within ion library
   * but order does not matter for loading - the index an ion uses is read back from the file - it
   * only keeps the output stable and the library readable.
   */
  private static IonLibrary collectLibrary(
      @NotNull final Map<FeatureListRow, List<IonIdentity>> ionsByRow) {
    final Set<IonType> distinct = new LinkedHashSet<>();
    for (final List<IonIdentity> ions : ionsByRow.values()) {
      for (final IonIdentity ion : ions) {
        distinct.add(ion.getIonType());
      }
    }
    return new UnmodifiableIonLibrary(LIBRARY_NAME, new ArrayList<>(distinct));
  }

  /**
   * The library goes in as the JSON of a StorableIonLibrary, which stores every ion part definition
   * once and lets the ion types reference it by id and count.
   */
  private static void writeLibrary(@NotNull final XMLStreamWriter writer,
      @NotNull final String libraryJson) throws XMLStreamException {
    writer.writeStartElement(IonNetworkXml.ION_LIBRARY_ELEMENT);
    writer.writeCharacters(libraryJson);
    writer.writeEndElement();
  }

  /**
   * Only the network itself, its members are written with the rows.
   */
  private static void writeNetwork(@NotNull final XMLStreamWriter writer,
      @NotNull final IonNetwork network) throws XMLStreamException {
    writer.writeStartElement(IonNetworkXml.NETWORK_ELEMENT);
    writer.writeAttribute(IonNetworkXml.NETWORK_ID_ATTR, String.valueOf(network.getID()));
    writeFormulas(writer, IonNetworkXml.CONSENSUS_FORMULAS_ELEMENT, network.getMolFormulas());
    writer.writeEndElement();
  }

  private static void writeRow(@NotNull final XMLStreamWriter writer,
      @NotNull final FeatureListRow row, @NotNull final List<IonIdentity> ions,
      @NotNull final Map<IonType, Integer> libraryIndices) throws XMLStreamException {
    writer.writeStartElement(IonNetworkXml.ROW_ELEMENT);
    writer.writeAttribute(IonNetworkXml.ROW_ID_ATTR, String.valueOf(row.getID()));

    for (final IonIdentity ion : ions) {
      writer.writeStartElement(IonNetworkXml.ION_ELEMENT);
      writer.writeAttribute(IonNetworkXml.ION_NETWORK_ATTR,
          String.valueOf(ion.getNetwork().getID()));
      writer.writeAttribute(IonNetworkXml.ION_TYPE_REF_ATTR,
          String.valueOf(libraryIndices.get(ion.getIonType())));
      writeFormulas(writer, IonNetworkXml.ION_FORMULAS_ELEMENT, ion.getMolFormulas());
      writer.writeEndElement();
    }

    writer.writeEndElement();
  }

  private static void writeFormulas(@NotNull final XMLStreamWriter writer,
      @NotNull final String element, @NotNull final List<ResultFormula> formulas)
      throws XMLStreamException {
    if (formulas.isEmpty()) {
      return;
    }
    writer.writeStartElement(element);
    for (final ResultFormula formula : formulas) {
      formula.saveToXML(writer);
    }
    writer.writeEndElement();
  }
}
