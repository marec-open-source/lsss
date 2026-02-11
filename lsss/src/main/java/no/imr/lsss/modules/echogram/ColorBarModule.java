package no.imr.lsss.modules.echogram;

import no.imr.korona.color.Colormap;
import no.imr.korona.color.Colormaps;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.viewer.SvColorPanel;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.ColorConverterType;
import no.imr.korona.viewer.coloring.DiscreteColorConverter;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.raw.RawVariableFactory;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingSetup;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.interpretation.InterpretationUtils;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.util.ApiMenuBuilder;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A module for setting sv thresholds and selecting color mapping.
 */
public final class ColorBarModule extends BaseViewModule implements PojoDataContainer {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   public final ObjectParameter<Colormap> colormap = new ObjectParameter<>(new Name("Colormap"),
         Colormaps.COMBINED, Colormaps.ALL);

   private final ColorConverterContainer converterContainer;

   private boolean updateLastColor;
   private ColorConverter lastOriginalColor;
   private ColorConverter lastPreprocessedColor;

   private final ConditionalMaskingManager conditionalMaskingManager;

   public ColorBarModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      converterContainer = getInterpretationSettings().getColorConverterContainer();
      lastOriginalColor = converterContainer.getColorConverter();
      lastPreprocessedColor = converterContainer.getColorConverter();
      conditionalMaskingManager = new ConditionalMaskingManager(getLSSS());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            colormap
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(converterContainer.getChangeManager(), newCoalescingExecListener(() -> {
         updateLastColor();
         switch (converterContainer.getColorConverter()) {
            case SingleValueColorConverter singleValueColorConverter -> {
               colormap.setValue(singleValueColorConverter.getColormap());
            }
            case DiscreteColorConverter _ -> {
               ConditionalPingMask conditionalPingMask = getRegionManager().getConditionalPingMask();
               if (!conditionalPingMask.isEmpty()) {
                  // Set mask again to trigger update for all modules using the current conditional mask.
                  getRegionManager().setConditionalPingMask(conditionalPingMask);
               }
            }
         }
         viewHolder.ifView(View::updateMaskingGUI);
      }));

      registry.add(viewHolder.coalescingListener(View::updateLowerThresholdButtons), List.of(
            getConfigurationManager().getSurveyMiscConf().preferredLowerThreshold,
            getConfigurationManager().getSurveyMiscConf().additionalLowerThresholds
      ));

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(() -> {
         updateConverterContainer();
         conditionalMaskingManager.update();
         viewHolder.ifView(View::removeMaskingGUI);
      }));

      registry.add(colormap, selectedColormap -> {
         if (converterContainer.getColorConverter() instanceof SingleValueColorConverter singleValueColorConverter
               && !singleValueColorConverter.getColormap().equals(selectedColormap)) {
            converterContainer.setColorConverter(new SingleValueColorConverter(singleValueColorConverter.getContinuousVariable(), selectedColormap));
         }
      });

      Colormap currentColormap = converterContainer.getColorConverter().getColormap();
      if (currentColormap != null) {
         colormap.setValue(currentColormap);
      }
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private void updateLastColor() {
      if (updateLastColor) {
         ColorConverter colorConverter = converterContainer.getColorConverter();
         if (getLSSS().getDataSetManager().getSelectedDataType() == DataType.PROCESSED) {
            lastPreprocessedColor = colorConverter;
         }
         ContinuousVariable continuousVariable = colorConverter.getContinuousVariable();
         if (colorConverter.getDiscreteVariable() == null && continuousVariable != null && continuousVariable.getVariableGroup() == RawVariableFactory.RAW_VARIABLE_GROUP) {
            lastOriginalColor = colorConverter;
         }
      }
   }

   private void updateConverterContainer() {
      updateLastColor = false;

      PreprocessingSetup mainSetup = getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup();
      ConfigFileSettings configFileSettings;
      try {
         configFileSettings = mainSetup.createConfigFileSettings();
      } catch (IOException _) {
         configFileSettings = getLSSS().getKorona().createConfigFileSettings();
      }
      converterContainer.updateEvaluationContext(getInterpretationSettings().getDataFileSet().getPingConfiguration().getConfigurationItems(), configFileSettings);

      ColorConverter colorConverter;
      if (getLSSS().getDataSetManager().getSelectedDataType() == DataType.PROCESSED) {
         if (!lastPreprocessedColor.isUsableInContext()) {
            lastPreprocessedColor = lastOriginalColor;
         }
         colorConverter = lastPreprocessedColor;
      } else {
         colorConverter = lastOriginalColor;
      }
      converterContainer.setColorConverter(colorConverter);

      updateLastColor = true;
   }

   @Override
   public PojoData getPojoData() {
      ColorConverter colorConverter = converterContainer.getColorConverter();
      ColorConverterType type = colorConverter.getType();
      PojoData.Builder builder = PojoData.newBuilder(getPersistentName())
            .with("visualizationType", type.name());

      DiscreteVariable discreteVariable = colorConverter.getDiscreteVariable();
      if (type.isDiscrete() && discreteVariable != null) {
         builder.with("discreteVariable", builder.newBuilder()
               .with("name", discreteVariable.getName().persistentName())
               .with("categories", discreteVariable.getSettings().getCategories().stream()
                     .map(category -> {
                        return builder.newBuilder()
                              .with("name", category.getName())
                              .with("legend", category.getLegend())
                              .with("color", ColorUtils.colorToHex(category.getColor()))
                              .build();
                     })
                     .toList())
               .build());
      }

      ContinuousVariable continuousVariable = colorConverter.getContinuousVariable();
      if (type.isContinuous() && continuousVariable != null) {
         ContinuousVariableSettings settings = continuousVariable.getSettings();
         Unit unit = continuousVariable.getUnit();
         builder.with("continuousVariable", builder.newBuilder()
               .with("name", continuousVariable.getName().persistentName())
               .with("unit", continuousVariable.getUnit().formalName())
               .with("thresholds", unit, builder.newBuilder()
                     .with("min", settings.getRange().min())
                     .with("max", settings.getRange().max())
                     .build())
               .with("limits", unit, builder.newBuilder()
                     .with("min", settings.getMaxRange().min())
                     .with("max", settings.getMaxRange().max())
                     .build())
               .with("deltaValue", unit, settings.getDelta())
               .with("clipAbove", settings.isClipAbove())
               .build());
      }

      Colormap colormap = colorConverter.getColormap();
      if (type == ColorConverterType.CONTINUOUS && colormap != null && continuousVariable != null) {
         ContinuousVariableSettings settings = continuousVariable.getSettings();
         FloatRange range = settings.getRange();
         int n = Math.round(range.getSize() / settings.getDelta());
         builder.with("colormap", builder.newBuilder()
               .with("name", colormap.name())
               .with("colorAbove", ColorUtils.colorToHex(colormap.aboveRGB()))
               .with("colorBelow", ColorUtils.colorToHex(colormap.belowRGB()))
               .with("colors", IntStream.range(0, n)
                     .mapToObj(i -> {
                        float value = i / (float) n;
                        int rgb = colormap.getRGB(value);
                        return ColorUtils.colorToHex(rgb);
                     })
                     .toList())
               .build());
      }

      return builder
            .build();
   }

   private static final class View extends BaseView {
      private final ColorBarModule module;
      private final JPanel panel = new JPanel(new BorderLayout());
      private final JPanel lowerThresholdButtons = new JPanel(new GridLayout(0, 1));
      private final SvColorPanel svColorPanel;

      private @Nullable ConditionalMaskingGUI currentConditionalMaskingGUI;

      private View(ColorBarModule module) {
         super(module);

         this.module = module;

         svColorPanel = new SvColorPanel(module.converterContainer);
         svColorPanel.setUseAdvancedDialog(true);
         svColorPanel.addPopupMenuExtender(this::addAdditionalPopupItems);

         panel.add(svColorPanel.getComponent());
         panel.add(lowerThresholdButtons, BorderLayout.SOUTH);

         updateLowerThresholdButtons();
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      @Override
      public JComponent getApiComponent() {
         return svColorPanel.getComponent();
      }

      private void addAdditionalPopupItems(JPopupMenu popupMenu) {
         if (module.conditionalMaskingManager.hasContent()) {
            popupMenu.addSeparator();
            JMenuItem item = popupMenu.add("Conditional masking...");
            item.setMnemonic(KeyEvent.VK_M);
            item.addActionListener(_ -> {
               if (currentConditionalMaskingGUI != null) {
                  currentConditionalMaskingGUI.getDialog().toFront();
               } else {
                  currentConditionalMaskingGUI = new ConditionalMaskingGUI(module.getLSSS(), svColorPanel, module.conditionalMaskingManager);
                  currentConditionalMaskingGUI.getDialog().addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosed(WindowEvent e) {
                        currentConditionalMaskingGUI = null;
                     }
                  });
                  currentConditionalMaskingGUI.getDialog().setVisible(true);
               }
            });
         }

         popupMenu.addSeparator();
         new ApiMenuBuilder(module.getLSSS(), panel)
               .moduleMenu(module)
               .addTo(popupMenu);
      }

      private void updateLowerThresholdButtons() {
         List<Integer> values = new ArrayList<>();
         values.add(module.getConfigurationManager().getSurveyMiscConf().preferredLowerThreshold.getIntValue());
         values.addAll(module.getConfigurationManager().getSurveyMiscConf().additionalLowerThresholds.getValue());
         values.sort(Collections.reverseOrder());

         lowerThresholdButtons.removeAll();
         Insets emptyInsets = new Insets(0, 0, 0, 0);
         for (int value : values) {
            JButton button = new JButton(String.valueOf(value));
            button.setToolTipText("<html>Set lower threshold to " + value
                  + "<br>Shift + Click to keep assigned sa on selected regions");
            button.setMargin(emptyInsets);
            button.setFocusable(false);
            button.addActionListener(e -> {
               if (GuiUtils.getKeyModifiers(e) == ActionEvent.SHIFT_MASK) {
                  InterpretationUtils.setLowerThresholdAndKeepAssignedSa(module.getLSSS(), value);
               } else {
                  PingRange pingRange = module.getInterpretationSettings().getPingRange();
                  module.getRegionManager().getThresholdManager().set(pingRange, null, (float) value, null);
               }
            });
            lowerThresholdButtons.add(button);
         }
         lowerThresholdButtons.revalidate();
         lowerThresholdButtons.repaint();
      }

      private void updateMaskingGUI() {
         if (currentConditionalMaskingGUI != null) {
            currentConditionalMaskingGUI.update();
         }
      }

      private void removeMaskingGUI() {
         if (currentConditionalMaskingGUI != null) {
            currentConditionalMaskingGUI.getDialog().dispose();
            currentConditionalMaskingGUI = null;
         }
      }
   }
}
