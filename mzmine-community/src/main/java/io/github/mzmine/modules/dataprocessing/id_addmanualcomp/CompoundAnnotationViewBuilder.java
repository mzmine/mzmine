/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_addmanualcomp;

import static io.github.mzmine.javafx.components.factories.FxTexts.boldText;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.abstr.StringType;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.DoubleType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.FloatType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.IntegerType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.LongType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.NumberType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.structures.StructureParser;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.MapChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;

public class CompoundAnnotationViewBuilder extends FxViewBuilder<CompoundAnnotationModel> {

  private static final Logger logger = Logger.getLogger(
      CompoundAnnotationViewBuilder.class.getName());
  private final Runnable onSave;
  private final Runnable onCancel;
  //  private final Set<DataType<?>> excludedTypes = new HashSet<>();
  private BooleanProperty valid = new SimpleBooleanProperty(false);
  private final List<DataType<?>> userTypes = Arrays.stream(
          new DataType<?>[]{new CompoundNameType(), new FormulaType(), new IonTypeType(),
              new SmilesStructureType(), new InChIStructureType(), new InChIKeyStructureType(),
              new PrecursorMZType(), new RTType(), new CCSType(), new CommentType()})
      .sorted(Comparator.comparing(DataType::getUniqueID)).toList();

  protected CompoundAnnotationViewBuilder(CompoundAnnotationModel model, Runnable onSave,
      Runnable onCancel) {
    super(model);
    this.onSave = onSave;
    this.onCancel = onCancel;
  }

  @Override
  public Region build() {
    final CompoundDatabaseMatchesType dbType = new CompoundDatabaseMatchesType();
    final List<DataType> subDataTypes = dbType.getSubDataTypes().stream()
        .sorted(Comparator.comparing(DataType::getUniqueID)).toList();

    final BorderPane main = new BorderPane();
    final GridPane fields = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    fields.setPadding(FxLayout.DEFAULT_PADDING_INSETS);

    final HBox structWrapper = new HBox();
    main.setTop(structWrapper);
    int rowCounter = 0;
    for (int i = 0; i < userTypes.size(); i++) {
      DataType type = userTypes.get(i);
      final TextField tf = createTextField(type, structWrapper);
      if (tf == null) {
        continue;
      }
      Label label = new Label(type.getHeaderString());
      fields.add(label, 0, rowCounter);
      fields.add(tf, 1, rowCounter);

      Label parsed = new Label();
      parsed.textProperty().bind(Bindings.createStringBinding(
          () -> type.getFormattedStringCheckType(model.getDataModel().get(type)),
          tf.textProperty()));
      fields.add(parsed, 2, rowCounter);
      rowCounter++;
    }
    fields.getColumnConstraints().addAll(new ColumnConstraints(100),
        new ColumnConstraints(100, 300, Double.MAX_VALUE, Priority.ALWAYS, HPos.LEFT, true),
        new ColumnConstraints(100));
    final ScrollPane scrollPane = new ScrollPane(fields);
    scrollPane.setFitToHeight(true);
    scrollPane.setFitToWidth(true);
    main.setCenter(scrollPane);

    final Button save = FxButtons.createSaveButton(onSave);
    final Button cancel = FxButtons.createCancelButton(onCancel);
    final FlowPane buttons = FxLayout.newFlowPane(Pos.CENTER_RIGHT, save, cancel);
    final TextFlow required = FxTextFlows.newTextFlow(TextAlignment.RIGHT, text("Fields "),
        boldText("\"Compound\""), text(" and either of: "), boldText("\"Precursor m/z\""),
        text(", "), boldText(" \"SMILES\" and \"Adduct\""), text(", or "),
        boldText("\"Formula\" and \"Adduct\""), text(" must be defined."));
    final HBox textWrapper = FxLayout.newHBox(Pos.TOP_RIGHT, required);
    final VBox vBox = FxLayout.newVBox(Pos.TOP_RIGHT, buttons, textWrapper);
    main.setBottom(vBox);

    valid.bind(Bindings.createBooleanBinding(this::validate, model.getDataModel()));
    required.disableProperty().bind(valid);
    textWrapper.disableProperty().bind(valid);
    save.disableProperty().bind(valid.not());

    return main;
  }

  @Nullable
  private TextField createTextField(DataType type, HBox structWrapper) {
    return switch (type) {
      case SmilesStructureType smiles -> {
        final TextField text = createStringField(smiles);
        text.textProperty().addListener((_, s, t1) -> {
          structWrapper.getChildren().clear();
          var structure = StructureParser.silent().parseStructure(t1, (String) null);
          if (structure != null) {
            structWrapper.getChildren().setAll(new Structure2DComponent(structure.structure()));
            model.setType(smiles, t1);
          }
        });
        yield text;
      }
      case InChIStructureType inchi -> {
        final TextField text = createStringField(inchi);
        text.textProperty().addListener((_, s, t1) -> {
          structWrapper.getChildren().clear();
          var structure = StructureParser.silent().parseStructure(null, t1);
          if (structure != null) {
            structWrapper.getChildren().setAll(new Structure2DComponent(structure.structure()));
            model.setType(inchi, t1);
          }
        });
        yield text;
      }
      case IonTypeType ion -> createAdductField(ion);
      case NumberType numberType -> createNumberField(numberType);
      case StringType strType -> createStringField(strType);
      default -> {
        logger.finest(
            "Skipping creation of a text field for type %s".formatted(type.getUniqueID()));
        yield null;
      }
    };
  }

  private TextField createNumberField(NumberType numberType) {
    TextField tf = new TextField();

    ObjectProperty<Number> numberProperty = new SimpleObjectProperty<>(
        (Number) model.getDataModel().get(numberType));
    StringProperty stringProperty = new SimpleStringProperty();

    StringConverter<Number> converter = new StringConverter<>() {
      @Override
      public java.lang.String toString(Number object) {
        if (object == null) {
          return "";
        }
        return numberType.getFormattedStringCheckType(object);
      }

      @Override
      public Number fromString(java.lang.String string) {
        if (string == null || string.isBlank()) {
          return null;
        }
        try {
          return switch (numberType) {
            case DoubleType _ -> numberType.getFormat().parse(string).doubleValue();
            case FloatType _ -> numberType.getFormat().parse(string).floatValue();
            case IntegerType _ -> numberType.getFormat().parse(string).intValue();
            case LongType _ -> numberType.getFormat().parse(string).longValue();
            default -> throw new RuntimeException(
                "Unknown number type %s".formatted(numberType.getClass().getName()));
          };
        } catch (ParseException e) {
          logger.finest("Cannot parse value %s in field for type %s".formatted(string, numberType));
          return null;
        }
      }
    };

    Bindings.bindBidirectional(stringProperty, numberProperty, converter);
    numberProperty.addListener((_, _, newValue) -> {
      if (!Objects.equals(model.getDataModel().get(numberType), newValue)) {
        model.getDataModel().put(numberType, newValue);
      }
    });
    model.getDataModel().addListener((MapChangeListener<DataType, Object>) change -> {
      if (change.getKey().equals(numberType) && change.wasAdded() && !Objects.equals(
          change.getValueAdded(), numberProperty.get())) {
        numberProperty.set((Number) change.getValueAdded());
      }
    });

    tf.textProperty().bindBidirectional(stringProperty);
    tf.textProperty().addListener((_, _, _) -> numberProperty.get());
    return tf;
  }

  private TextField createStringField(StringType stringType) {
    TextField tf = new TextField();
    final SimpleStringProperty property = new SimpleStringProperty(
        (String) model.getDataModel().get(stringType));
    tf.textProperty().bindBidirectional(property);

    property.addListener((_, _, newValue) -> {
      if (!Objects.equals(model.getDataModel().get(stringType), newValue)) {
        model.getDataModel().put(stringType, newValue);
      }
    });
    model.getDataModel().addListener((MapChangeListener<DataType, Object>) change -> {
      if (change.getKey().equals(stringType) && change.wasAdded() && !Objects.equals(
          change.getValueAdded(), property.get())) {
        property.set((String) change.getValueAdded());
      }
    });

    tf.textProperty().addListener((_, _, _) -> property.get());
    return tf;
  }

  private TextField createAdductField(IonTypeType ionType) {
    TextField tf = new TextField();

    ObjectProperty<IonType> ionTypeProperty = new SimpleObjectProperty<>(
        (IonType) model.getDataModel().get(ionType));
    StringProperty stringProperty = new SimpleStringProperty();

    StringConverter<IonType> converter = new StringConverter<>() {
      @Override
      public java.lang.String toString(IonType object) {
        if (object == null) {
          return "";
        }
        return ionType.getFormattedStringCheckType(object);
      }

      @Override
      public IonType fromString(java.lang.String string) {
        if (string == null || string.isBlank()) {
          return null;
        }
        return IonTypeParser.parse(string);
      }
    };

    Bindings.bindBidirectional(stringProperty, ionTypeProperty, converter);
    ionTypeProperty.addListener((_, _, newValue) -> {
      if (!Objects.equals(model.getDataModel().get(ionType), newValue)) {
        model.getDataModel().put(ionType, newValue);
      }
    });
    model.getDataModel().addListener((MapChangeListener<DataType, Object>) change -> {
      if (change.getKey().equals(ionType) && change.wasAdded() && !Objects.equals(
          change.getValueAdded(), ionTypeProperty.get())) {
        ionTypeProperty.set((IonType) change.getValueAdded());
      }
    });

    tf.textProperty().bindBidirectional(stringProperty);
    tf.textProperty().addListener((_, _, _) -> ionTypeProperty.get());
    return tf;
  }

  private boolean validate() {
    return ((model.getDataModel().get(DataTypes.get(PrecursorMZType.class)) != null
        // calc from formula + adduct
        || (model.getDataModel().get(DataTypes.get(FormulaType.class)) != null
        && model.getDataModel().get(DataTypes.get(IonTypeType.class)) != null))
        // calc from smiles + adduct
        || (model.getDataModel().get(DataTypes.get(SmilesStructureType.class)) != null
        && model.getDataModel().get(DataTypes.get(IonTypeType.class)) != null))

        && model.getDataModel().get(DataTypes.get(CompoundNameType.class)) != null
        && !((String) model.getDataModel().get(DataTypes.get(CompoundNameType.class))).isEmpty();
  }
}
