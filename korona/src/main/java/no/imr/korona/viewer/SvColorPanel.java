package no.imr.korona.viewer;

import no.imr.korona.color.Colormap;
import no.imr.korona.color.Colormaps;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.viewer.coloring.CategoryColorConverter;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.ColorConverterType;
import no.imr.korona.viewer.coloring.LightCategoryColorConverter;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.coloring.ThresholdCategoryColorConverter;
import no.imr.korona.viewer.variables.BaseVariable;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.korona.viewer.variables.categorization.ProbabilityVariable;
import no.imr.korona.viewer.variables.plankton.BioVolumeVariable;
import no.imr.korona.viewer.variables.plankton.PlanktonVariable;
import no.imr.korona.viewer.variables.plankton.ResidualVariable;
import no.imr.korona.viewer.variables.raw.SvVariable;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.overlay.OverlaidComponent;
import no.imr.tools.swing.overlay.Overlay;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Panel for viewing and changing color map settings.
 */
public final class SvColorPanel {
   private static final int PREFERRED_WIDTH = 52;

   private final OverlaidComponent<Overlay> overlaidComponent = new OverlaidComponent<>();

   private final List<Consumer<JPopupMenu>> popupMenuExtenders = new ArrayList<>();

   private final ColorConverterContainer converterContainer;

   private List<Colormap> colormaps = Colormaps.ALL;

   private Class<? extends ContinuousVariable> defaultContinuousVariable = SvVariable.class;
   private boolean useAdvancedDialog;
   private boolean usePopupMenu = true;

   public SvColorPanel(ColorConverterContainer converterContainer) {
      this.converterContainer = converterContainer;
      addOverlay(new PopUpOverlay());
      addOverlay(new SvColorPanelColorBarOverlay(converterContainer));
      overlaidComponent.setPreferredSize(new Dimension(PREFERRED_WIDTH, 600));
      converterContainer.getChangeManager().addListener(this::repaint);
   }

   public JComponent getComponent() {
      return overlaidComponent;
   }

   public List<Colormap> getColormaps() {
      return colormaps;
   }

   public void setColormaps(List<Colormap> colormaps) {
      this.colormaps = colormaps;
   }

   public void setConfigurationItems(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      converterContainer.updateEvaluationContext(configurationItems, configFileSettings);
   }

   public void setDefaultContinuousVariable(Class<? extends ContinuousVariable> defaultContinuousVariable) {
      this.defaultContinuousVariable = defaultContinuousVariable;
   }

   public void setUseAdvancedDialog(boolean useAdvancedDialog) {
      this.useAdvancedDialog = useAdvancedDialog;
   }

   public void setUsePopupMenu(boolean usePopupMenu) {
      this.usePopupMenu = usePopupMenu;
   }

   public void addPopupMenuExtender(Consumer<JPopupMenu> popupMenuExtender) {
      popupMenuExtenders.add(popupMenuExtender);
   }

   private JPopupMenu makePopupMenu() {
      JPopupMenu menu = new JPopupMenu();

      ContinuousVariable continuousVariable = converterContainer.getColorConverter().getContinuousVariable();
      ContinuousVariableSettings settings = continuousVariable != null ? continuousVariable.getSettings() : null;

      JMenuItem thresholdsItem = menu.add("Set thresholds...");
      JMenuItem limitsItem = menu.add("Set colour scale limits...");
      if (settings == null) {
         thresholdsItem.setEnabled(false);
         limitsItem.setEnabled(false);
      } else {
         thresholdsItem.addActionListener(e -> {
            new MinMaxDialog(overlaidComponent, "Thresholds",
                  settings.getRange(), settings.getDelta(), settings.getMaxRange(), settings::setRange);
         });
         limitsItem.addActionListener(e -> {
            new MinMaxDialog(overlaidComponent, "Colour scale limits",
                  settings.getMaxRange(), settings.getDelta(), null, settings::setMaxRange);
         });
      }

      menu.addSeparator();

      boolean clipAbove = settings != null && settings.isClipAbove();
      JMenuItem clipAboveItem = MiscIcons.checkBox(clipAbove).on(menu.add("Clip values above"));
      if (settings == null) {
         clipAboveItem.setEnabled(false);
      } else {
         clipAboveItem.addActionListener(e -> {
            settings.setClipAbove(!clipAbove);
         });
      }

      menu.addSeparator();

      addColormapItems(menu);

      if (useAdvancedDialog) {
         menu.addSeparator();
         JMenuItem advancedItem = menu.add("Advanced...");
         advancedItem.addActionListener(e -> new AdvancedDialog(converterContainer, colormaps, overlaidComponent).show());
      }

      for (Consumer<JPopupMenu> popupMenuExtender : popupMenuExtenders) {
         popupMenuExtender.accept(menu);
      }
      return menu;
   }

   private void addColormapItems(JPopupMenu popupMenu) {
      for (Colormap colormap : colormaps) {
         addContinuousItem(popupMenu, false, converterContainer.getContinuousVariable(defaultContinuousVariable), colormap);
      }

      CategoryVariable category = converterContainer.getDiscreteVariable(CategoryVariable.class);
      if (category.isUsableInContext()) {
         popupMenu.addSeparator();

         addDiscreteItem(popupMenu, category);
         addDiscreteLightItem(popupMenu, category, converterContainer.getContinuousVariable(ProbabilityVariable.class));
         addDiscreteThresholdItem(popupMenu, category, converterContainer.getContinuousVariable(ProbabilityVariable.class));
      }

      PlanktonVariable plankton = converterContainer.getDiscreteVariable(PlanktonVariable.class);
      if (plankton.isUsableInContext()) {
         popupMenu.addSeparator();

         addDiscreteItem(popupMenu, plankton);
         addContinuousItem(popupMenu, true, converterContainer.getContinuousVariable(ResidualVariable.class), Colormaps.COMBINED);
         addContinuousItem(popupMenu, true, converterContainer.getContinuousVariable(BioVolumeVariable.class), Colormaps.COMBINED);
      }
   }

   private void addContinuousItem(JPopupMenu popupMenu, boolean useVariableName, ContinuousVariable continuousVariable, Colormap colormap) {
      boolean selected = isSelected(ColorConverterType.CONTINUOUS, null, continuousVariable, colormap);
      JMenuItem item = MiscIcons.check(selected).on(popupMenu.add(useVariableName ? continuousVariable.getDisplayName() : colormap.getName()));
      item.setEnabled(continuousVariable.isUsableInContext());
      item.addActionListener(e -> {
         converterContainer.setColorConverter(new SingleValueColorConverter(continuousVariable, colormap));
      });
      if (!useVariableName) {
         item.addChangeListener(e -> {
            if (item.isArmed()) {
               item.setIcon(new ColormapIcon(colormap));
               item.setDisabledIcon(null);
            } else {
               MiscIcons.check(selected).on(item);
            }
         });
      }
   }

   private void addDiscreteItem(JPopupMenu popupMenu, DiscreteVariable discreteVariable) {
      boolean selected = isSelected(ColorConverterType.DISCRETE, discreteVariable, null, null);
      JMenuItem item = MiscIcons.check(selected).on(popupMenu.add(discreteVariable.getDisplayName()));
      item.setEnabled(discreteVariable.isUsableInContext());
      item.addActionListener(e -> {
         converterContainer.setColorConverter(new CategoryColorConverter(discreteVariable));
      });
   }

   private void addDiscreteLightItem(JPopupMenu popupMenu, DiscreteVariable discreteVariable, ContinuousVariable continuousVariable) {
      boolean selected = isSelected(ColorConverterType.DISCRETE_LIGHT, discreteVariable, continuousVariable, null);
      JMenuItem item = MiscIcons.check(selected).on(popupMenu.add(discreteVariable.getDisplayName() + " " + continuousVariable.getDisplayName() + " light"));
      item.setEnabled(discreteVariable.isUsableInContext() && continuousVariable.isUsableInContext());
      item.addActionListener(e -> {
         converterContainer.setColorConverter(new LightCategoryColorConverter(discreteVariable, continuousVariable));
      });
   }

   private void addDiscreteThresholdItem(JPopupMenu popupMenu, DiscreteVariable discreteVariable, ContinuousVariable continuousVariable) {
      boolean selected = isSelected(ColorConverterType.DISCRETE_THRESHOLD, discreteVariable, continuousVariable, null);
      JMenuItem item = MiscIcons.check(selected).on(popupMenu.add(discreteVariable.getDisplayName() + " " + continuousVariable.getDisplayName() + " threshold"));
      item.setEnabled(discreteVariable.isUsableInContext() && continuousVariable.isUsableInContext());
      item.addActionListener(e -> {
         converterContainer.setColorConverter(new ThresholdCategoryColorConverter(discreteVariable, continuousVariable));
      });
   }

   private boolean isSelected(ColorConverterType type, @Nullable DiscreteVariable discreteVariable,
                              @Nullable ContinuousVariable continuousVariable, @Nullable Colormap colormap) {
      ColorConverter colorConverter = converterContainer.getColorConverter();
      return colorConverter.getType() == type
            && colorConverter.getContinuousVariable() == continuousVariable
            && colorConverter.getDiscreteVariable() == discreteVariable
            && colorConverter.getColormap() == colormap;
   }

   private final class PopUpOverlay extends Overlay {
      private PopUpOverlay() {
      }

      @Override
      public boolean overlaps(Rectangle2D rectangle2D) {
         return true;
      }

      @Override
      public @Nullable JPopupMenu getPopupMenu(Point point) {
         return usePopupMenu ? makePopupMenu() : null;
      }
   }

   private void repaint() {
      overlaidComponent.repaint();
   }

   public int getWidth() {
      return overlaidComponent.getWidth();
   }

   public int getHeight() {
      return overlaidComponent.getHeight();
   }

   public void addOverlay(Overlay overlay) {
      overlaidComponent.addOverlay(overlay);
   }

   public ColorConverterContainer getConverterContainer() {
      return converterContainer;
   }

   private static final class AdvancedDialog {
      private final JDialog dialog;
      private final List<Colormap> colormaps;
      private final JComponent referenceComponent;
      private final ColorConverterContainer converterContainer;
      private final ColorConverter backupColorConverter;

      private final List<ColorConverterType> types;
      private final List<DiscreteVariable> discreteVariables;
      private final List<ContinuousVariable> continuousVariables;

      private AdvancedDialog(ColorConverterContainer converterContainer, List<Colormap> colormaps, JComponent referenceComponent) {
         this.converterContainer = converterContainer;
         backupColorConverter = converterContainer.getColorConverter();
         this.colormaps = colormaps;
         this.referenceComponent = referenceComponent;
         dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), "Advanced", Dialog.ModalityType.DOCUMENT_MODAL);

         discreteVariables = converterContainer.getDiscreteVariables().stream()
               .filter(BaseVariable::isUsableInContext)
               .toList();
         continuousVariables = converterContainer.getContinuousVariables().stream()
               .filter(BaseVariable::isUsableInContext)
               .toList();
         types = Arrays.stream(ColorConverterType.values())
               .filter(type -> !(type.isContinuous() && continuousVariables.isEmpty()))
               .filter(type -> !(type.isDiscrete() && discreteVariables.isEmpty()))
               .toList();
      }

      private void show() {
         ObjectParameter<ColorConverterType> type = new ObjectParameter<>(
               new Name("VisualizationType", "Visualization type"),
               getInitialValue(converterContainer.getColorConverter().getType(), types), types);

         ObjectParameter<Optional<DiscreteVariable>> discreteVariable = new ObjectParameter<>(
               new Name("DiscreteVariable", "Discrete variable"),
               getInitialOptionalValue(converterContainer.getColorConverter().getDiscreteVariable(), discreteVariables),
               discreteVariables.isEmpty() ? List.of(Optional.empty()) : Utils.toOptionals(discreteVariables)) {
            @Override
            public String toString(Optional<DiscreteVariable> value) {
               return value.map(BaseVariable::toString).orElse("");
            }
         };

         ObjectParameter<ContinuousVariable> continuousVariable = new ObjectParameter<>(
               new Name("ContinuousVariable", "Continuous variable"),
               getInitialValue(converterContainer.getColorConverter().getContinuousVariable(), continuousVariables), continuousVariables) {
            @Override
            public String toString(ContinuousVariable value) {
               return value.getVariableGroup().name() + ": " + value.getDisplayName();
            }
         };

         ObjectParameter<Colormap> colorMap = new ObjectParameter<>(
               new Name("Colormap", "Colormap"),
               getInitialValue(converterContainer.getColorConverter().getColormap(), colormaps), colormaps);

         type.addListenerAndNotify(t -> {
            discreteVariable.setEnabled(t.isDiscrete());
            continuousVariable.setEnabled(t.isContinuous());
            colorMap.setEnabled(t == ColorConverterType.CONTINUOUS);
         });

         ParameterEditor parameterEditor = new ParameterEditor(List.of(type, discreteVariable, continuousVariable, colorMap));
         parameterEditor.getParameterChangeManager().addListener(() -> {
            converterContainer.setColorConverter(createColorConverter(type.getValue(), discreteVariable.getValue(), continuousVariable.getValue(), colorMap.getValue()));
         });

         JButton okButton = new JButton("OK");
         okButton.addActionListener(e -> dialog.dispose());

         JButton cancelButton = new JButton("Cancel");
         cancelButton.addActionListener(e -> {
            converterContainer.setColorConverter(backupColorConverter);
            dialog.dispose();
         });
         GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
         buttonPanel.add(okButton);
         buttonPanel.add(cancelButton);

         JPanel editorComponent = parameterEditor.getEditorComponent();
         editorComponent.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(new JScrollPane(editorComponent));
         panel.add(buttonPanel, BorderLayout.SOUTH);

         dialog.getRootPane().setDefaultButton(okButton);
         dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
         dialog.getContentPane().add(panel);
         dialog.pack();
         dialog.setLocationRelativeTo(referenceComponent);
         dialog.setVisible(true);
      }

      private static <T> T getInitialValue(@Nullable T value, List<T> values) {
         if (value == null || !values.contains(value)) {
            value = values.getFirst();
         }
         return value;
      }

      private static <T> Optional<T> getInitialOptionalValue(@Nullable T value, List<T> values) {
         if (!values.contains(value) && !values.isEmpty()) {
            value = values.getFirst();
         }
         return Optional.ofNullable(value);
      }

      private static ColorConverter createColorConverter(ColorConverterType type, Optional<DiscreteVariable> discreteVariable,
                                                         ContinuousVariable continuousVariable, Colormap colormap) {
         return switch (type) {
            case CONTINUOUS -> new SingleValueColorConverter(continuousVariable, colormap);
            case DISCRETE -> new CategoryColorConverter(discreteVariable.orElseThrow());
            case DISCRETE_LIGHT -> new LightCategoryColorConverter(discreteVariable.orElseThrow(), continuousVariable);
            case DISCRETE_THRESHOLD -> new ThresholdCategoryColorConverter(discreteVariable.orElseThrow(), continuousVariable);
         };
      }
   }
}
