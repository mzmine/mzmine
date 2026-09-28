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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.identities.io.IonLibraryIO;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkNode;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.SimpleIonNetwork;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;

/**
 * Loads {@link IonNetwork}s from XML written by {@link IonNetworksSaver} and attaches them to the
 * rows of the given feature list. Rows are resolved by their {@code getID()}, ions of rows that are
 * no longer in the feature list are dropped.
 * <p>
 * The ion types come from the library at the start of the file and are shared between all ions that
 * reference them. The rest is row major, so the ion identities of a row are simply restored in
 * document order and the first one is its best ion again. Networks are collected while the rows are
 * read and built once everything is known.
 */
public final class IonNetworksLoader {

  private static final Logger logger = Logger.getLogger(IonNetworksLoader.class.getName());

  private IonNetworksLoader() {
  }

  /**
   * @param in    the ion network file, not closed by this method
   * @param flist the feature list whose rows the networks refer to
   * @return the loaded networks, already attached to their rows
   */
  @NotNull
  public static List<IonNetwork> load(@NotNull final InputStream in,
      @NotNull final ModularFeatureList flist) throws XMLStreamException {
    final XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);

    // the ion types the ions reference by index, written before the rows
    final Map<Integer, IonType> library = new LinkedHashMap<>();
    // network id -> consensus formulas, in the order the networks were declared
    final Map<Integer, NetworkPlaceholder> networks = new LinkedHashMap<>();
    // network id -> members, filled while reading the rows
    final Map<Integer, List<IonNetworkNode>> nodesByNetwork = new LinkedHashMap<>();

    try {
      while (reader.hasNext()) {
        reader.next();
        if (!reader.isStartElement()) {
          continue;
        }
        switch (reader.getLocalName()) {
          case IonNetworkXml.ION_LIBRARY_ELEMENT -> library.putAll(readLibrary(reader));
          case IonNetworkXml.NETWORK_ELEMENT -> readNetwork(reader, networks);
          case IonNetworkXml.ROW_ELEMENT -> readRow(reader, flist, library, nodesByNetwork);
          default -> {
            // unknown element of a newer format, skip
          }
        }
      }
    } finally {
      reader.close();
    }

    return buildNetworks(flist, networks, nodesByNetwork);
  }

  /**
   * The ion types that the ions reference by index, keyed by that index. Written before the rows, so
   * it is complete by the time the first ion is read. The element holds the JSON of a
   * StorableIonLibrary, which {@link IonLibraryIO} turns back into ion types with deduplicated
   * parts.
   */
  private static Map<Integer, IonType> readLibrary(@NotNull final XMLStreamReader reader)
      throws XMLStreamException {
    final String json = reader.getElementText().trim();
    if (json.isEmpty()) {
      return Map.of();
    }
    // by index of the file, not by the order of library().ions(): that may be an already known
    // library instance whose ions are ordered differently
    return IonLibraryIO.loadFromJson(json).ionTypesByIndex();
  }

  /**
   * Read the ion network ID and other properties like the consensus formulas on the networks.
   */
  private static void readNetwork(@NotNull final XMLStreamReader reader,
      @NotNull final Map<Integer, NetworkPlaceholder> networks) throws XMLStreamException {
    final int id = Integer.parseInt(reader.getAttributeValue(null, IonNetworkXml.NETWORK_ID_ATTR));
    final List<ResultFormula> formulas = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(IonNetworkXml.NETWORK_ELEMENT))) {
      reader.next();
      if (reader.isStartElement() && reader.getLocalName()
          .equals(IonNetworkXml.CONSENSUS_FORMULAS_ELEMENT)) {
        formulas.addAll(readFormulas(reader, IonNetworkXml.CONSENSUS_FORMULAS_ELEMENT));
      }
    }

    networks.put(id, new NetworkPlaceholder(id, formulas));
  }

  /**
   * Read one row and hand its ion identities to their networks. The row keeps them in document
   * order, which restores its best ion.
   */
  private static void readRow(@NotNull final XMLStreamReader reader,
      @NotNull final ModularFeatureList flist, @NotNull final Map<Integer, IonType> library,
      @NotNull final Map<Integer, List<IonNetworkNode>> nodesByNetwork) throws XMLStreamException {
    final int rowId = Integer.parseInt(reader.getAttributeValue(null, IonNetworkXml.ROW_ID_ATTR));
    final FeatureListRow row = flist.findRowByID(rowId);

    final List<IonIdentity> ions = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(IonNetworkXml.ROW_ELEMENT))) {
      reader.next();
      if (!(reader.isStartElement() && reader.getLocalName().equals(IonNetworkXml.ION_ELEMENT))) {
        continue;
      }

      final int networkId = Integer.parseInt(
          reader.getAttributeValue(null, IonNetworkXml.ION_NETWORK_ATTR));
      final IonIdentity ion = readIon(reader, library);
      // still parse the ion of a missing row so that the reader stays in sync
      if (ion == null || row == null) {
        continue;
      }
      ions.add(ion);
      nodesByNetwork.computeIfAbsent(networkId, _ -> new ArrayList<>())
          .add(new IonNetworkNode(row, ion));
    }

    if (row == null) {
      logger.fine(
          () -> "Skipping ion identities of row %d, it is not in feature list %s".formatted(rowId,
              flist.getName()));
      return;
    }
    if (!ions.isEmpty()) {
      row.setIonIdentities(ions);
    }
  }

  /**
   * The ion type is not stored with the ion, it is referenced by its index in the library.
   */
  private static IonIdentity readIon(@NotNull final XMLStreamReader reader,
      @NotNull final Map<Integer, IonType> library) throws XMLStreamException {
    final int typeIndex = Integer.parseInt(
        reader.getAttributeValue(null, IonNetworkXml.ION_TYPE_REF_ATTR));
    final List<ResultFormula> formulas = new ArrayList<>();

    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(IonNetworkXml.ION_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }
      switch (reader.getLocalName()) {
        case IonNetworkXml.ION_FORMULAS_ELEMENT ->
            formulas.addAll(readFormulas(reader, IonNetworkXml.ION_FORMULAS_ELEMENT));
        default -> {
          // unknown element of a newer format, skip
        }
      }
    }

    final IonType ionType = library.get(typeIndex);
    if (ionType == null) {
      logger.fine(
          () -> "Skipping ion identity, ion type %d is not in the library of %d types".formatted(
              typeIndex, library.size()));
      return null;
    }
    final IonIdentity ion = new IonIdentity(ionType);
    ion.addMolFormulas(formulas);
    return ion;
  }

  private static List<ResultFormula> readFormulas(@NotNull final XMLStreamReader reader,
      @NotNull final String endElement) throws XMLStreamException {
    final List<ResultFormula> formulas = new ArrayList<>();
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(endElement))) {
      reader.next();
      if (reader.isStartElement() && reader.getLocalName().equals(ResultFormula.XML_ELEMENT)) {
        formulas.add(ResultFormula.loadFromXML(reader));
      }
    }
    return formulas;
  }

  @NotNull
  private static List<IonNetwork> buildNetworks(@NotNull final ModularFeatureList flist,
      @NotNull final Map<Integer, NetworkPlaceholder> placeholderNetworks,
      @NotNull final Map<Integer, List<IonNetworkNode>> nodesByNetwork) {
    final List<IonNetwork> networks = new ArrayList<>(nodesByNetwork.size());

    for (final Entry<Integer, List<IonNetworkNode>> entry : nodesByNetwork.entrySet()) {
      final int id = entry.getKey();
      final NetworkPlaceholder placeholder = placeholderNetworks.get(id);
      final List<ResultFormula> consensusFormulas =
          placeholder == null ? List.of() : placeholder.consensusFormulas;

      final SimpleIonNetwork network = new SimpleIonNetwork(id, entry.getValue(),
          consensusFormulas);
      network.setNetworkToAllRows();
      networks.add(network);
    }

    for (final int id : placeholderNetworks.keySet()) {
      if (!nodesByNetwork.containsKey(id)) {
        logger.fine(
            () -> "Skipping ion network %d, none of its rows are in feature list %s".formatted(id,
                flist.getName()));
      }
    }
    return networks;
  }

  private record NetworkPlaceholder(int id, List<ResultFormula> consensusFormulas) {

  }
}
