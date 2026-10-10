package io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data;

import io.github.mzmine.datamodel.AcquisitionMetadata;
import io.github.mzmine.datamodel.AcquisitionMetadata.Field;
import io.github.mzmine.datamodel.AcquisitionMetadata.Term;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Collects controlled acquisition declarations during the existing streaming import. */
public final class MzMLAcquisitionMetadata {
  private final Deque<String> path = new ArrayDeque<>();
  private final Map<String, List<String>> groups = new HashMap<>();
  private final Map<String, List<Term>> instruments = new HashMap<>();
  private final Set<String> usedInstruments = new HashSet<>();
  private final List<Term> methods = new ArrayList<>();
  private @Nullable String group;
  private @Nullable String instrument;

  public void open(final @NotNull XMLStreamReader xml, final @NotNull String tag) {
    path.addLast(tag);
    switch (tag) {
      case "referenceableParamGroup" -> {
        group = xml.getAttributeValue(null, "id");
        if (group != null) groups.put(group, new ArrayList<>());
      }
      case "instrumentConfiguration" -> {
        instrument = xml.getAttributeValue(null, "id");
        if (instrument != null) instruments.put(instrument, new ArrayList<>());
      }
      case "run", "scan" -> {
        final String reference = xml.getAttributeValue(null, tag.equals("run")
            ? "defaultInstrumentConfigurationRef" : "instrumentConfigurationRef");
        if (reference != null) usedInstruments.add(reference);
      }
      case "cvParam" -> {
        final String accession = xml.getAttributeValue(null, "accession");
        if (accession != null) {
          if (group != null) groups.get(group).add(accession);
          else accept(accession);
        }
      }
      case "referenceableParamGroupRef" -> {
        final String ref = xml.getAttributeValue(null, "ref");
        groups.getOrDefault(ref, List.of()).forEach(this::accept);
      }
      default -> {}
    }
  }

  private void accept(final @NotNull String accession) {
    for (final Term term : AcquisitionMetadata.resolve(accession)) {
      if (instrument != null && term.field() != Field.ACQUISITION_METHOD) {
        final boolean relevant = switch (term.field()) {
          case INSTRUMENT_MODEL -> !path.contains("componentList");
          case ANALYZER -> path.contains("analyzer");
          case IONIZATION -> path.contains("source");
          case DETECTOR -> path.contains("detector");
          case ACQUISITION_METHOD -> false;
        };
        if (relevant && !instruments.get(instrument).contains(term)) instruments.get(instrument).add(term);
      } else if (term.field() == Field.ACQUISITION_METHOD && (path.contains("fileContent")
          || path.contains("run")) && !path.contains("sample") && !methods.contains(term)) {
        methods.add(term);
      }
    }
  }

  public void close(final @NotNull String tag) {
    if (tag.equals("instrumentConfiguration")) instrument = null;
    if (tag.equals("referenceableParamGroup")) group = null;
    path.removeLast();
  }

  public @NotNull AcquisitionMetadata result() {
    final List<Term> result = new ArrayList<>(methods);
    usedInstruments.stream().sorted().forEach(id -> result.addAll(instruments.getOrDefault(id, List.of())));
    return new AcquisitionMetadata(result);
  }
}
