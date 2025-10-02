package no.imr.tools.parameter.gui.input;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.UiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * Base class for GUI elements used by {@link ParameterEditor}.
 *
 * @param <P> the parameter type for which this GUI applies
 */
public abstract class ParameterGUI<P extends BaseParameter<?>> {
   private final P parameter;
   private final GUIConfig guiConfig;
   private final NameLabel nameLabel;
   private final JLabel unitLabel = new JLabel();
   private final JTextPane descriptionLabel;
   private final JPanel unitAndDescriptionPanel = new JPanel(new BorderLayout());
   private List<? extends BaseParameter<?>> parameters = List.of();
   private @Nullable ParameterGUI<?> parentParameterGui;

   /**
    * Default constructor invoked reflectively by {@link ParameterGUIFactory}.
    *
    * @param parameter the parameter to show gui for
    */
   ParameterGUI(P parameter, GUIConfig guiConfig) {
      this.parameter = parameter;
      this.guiConfig = guiConfig;

      nameLabel = new NameLabel(parameter.getDisplayName());
      parameter.getProperty(BaseParameter.KEY_ICON).ifPresent(icon -> icon.on(nameLabel));
      nameLabel.setToolTipText(ParameterGuiUtils.getNameToolTip(parameter));

      String unit = parameter.getUnit().text();
      boolean hasUnit = !unit.isEmpty();
      if (hasUnit) {
         unitLabel.setText('[' + unit + ']');
         unitAndDescriptionPanel.add(unitLabel, BorderLayout.WEST);
      }

      String description = parameter.getDescription();
      if (!description.isEmpty()) {
         descriptionLabel = GuiUtils.labelLikeHtmlTextPane(toHtml(parameter.getDescription()));
         descriptionLabel.setBorder(BorderFactory.createEmptyBorder(2, hasUnit ? 12 : 0, 2, 0));
         descriptionLabel.setFocusable(false);
         descriptionLabel.setDisabledTextColor(UiUtils.labelDisabledForeground());

         unitAndDescriptionPanel.add(descriptionLabel);
      } else {
         descriptionLabel = new JTextPane();
      }
   }

   private static String toHtml(String text) {
      if (text.startsWith("<html>")) {
         return text.substring(6);
      } else {
         return HtmlEscapers.htmlEscaper().escape(text);
      }
   }

   public GUIConfig getGUIConfig() {
      return guiConfig;
   }

   NameLabel getNameLabel() {
      return nameLabel;
   }

   JLabel getUnitLabel() {
      return unitLabel;
   }

   P getParameter() {
      return parameter;
   }

   void init(List<? extends BaseParameter<?>> parameters) {
      this.parameters = parameters;
      nameLabel.addFocusListenerTo(getInputComponent());
   }

   void setParentParameterGui(ParameterGUI<?> parentParameterGui) {
      this.parentParameterGui = parentParameterGui;
   }

   public void addMouseClickListener(Listener listener) {
      MouseListener mouseListener = new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            listener.listen();
         }
      };
      nameLabel.addMouseListener(mouseListener);
      unitLabel.addMouseListener(mouseListener);
      descriptionLabel.addMouseListener(mouseListener);
   }

   void addName(GridBag gridBag) {
      gridBag.add(nameLabel);
   }

   void addInputAndDescription(GridBag gridBag, JComponent inputComponent) {
      if (guiConfig.combineInputAndDescription(parameter)) {
         if (hasDescriptionOrUnit(parameter)) {
            Insets insets = gridBag.getConstraints().insets;
            int margin = insets.left + insets.right;
            unitAndDescriptionPanel.setBorder(BorderFactory.createEmptyBorder(0, margin, 0, 0));
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(inputComponent, BorderLayout.WEST);
            panel.add(unitAndDescriptionPanel);
            gridBag.activateHorizontalFill();
            gridBag.getConstraints().weightx = Double.MIN_VALUE;
            addLast(gridBag, panel);
         } else {
            addLast(gridBag, inputComponent);
         }
      } else {
         if (someParameterHasDescriptionOrUnit()) {
            gridBag.add(inputComponent);
            gridBag.activateHorizontalFill();
            gridBag.getConstraints().weightx = Double.MIN_VALUE;
            addLast(gridBag, unitAndDescriptionPanel);
         } else {
            addLast(gridBag, inputComponent);
         }
      }
   }

   private static boolean hasDescriptionOrUnit(BaseParameter<?> parameter) {
      return !parameter.getDescription().isEmpty() || !parameter.getUnit().equals(Unit.NONE);
   }

   private boolean someParameterHasDescriptionOrUnit() {
      return parameters.stream().anyMatch(ParameterGUI::hasDescriptionOrUnit);
   }

   private static void addLast(GridBag gridBag, JComponent lastComponent) {
      gridBag.getConstraints().anchor = GridBagConstraints.WEST;
      gridBag.addWithLineBreak(lastComponent);
   }

   protected void updateEnabledState(Component component) {
      updateEnabledState(List.of(component));
   }

   void updateEnabledState(List<? extends Component> components) {
      update(nameLabel);
      update(unitLabel);
      update(descriptionLabel);
      update(unitAndDescriptionPanel);
      components.forEach(this::update);
      updateInputToolTip();
   }

   private void update(Component component) {
      boolean enabled = isParameterEnabled();
      if (component instanceof JTextComponent textComponent && textComponent != descriptionLabel) {
         textComponent.setEditable(enabled);
         textComponent.setForeground(UiUtils.textFieldForeground(enabled));
         textComponent.setBackground(UiUtils.textFieldBackground(enabled));
      } else {
         component.setEnabled(enabled);
      }
      component.setVisible(parameter.isVisible());
   }

   private boolean isParameterEnabled() {
      return guiConfig.isParameterEnabled(parameter)
            && (parentParameterGui == null || parentParameterGui.isParameterEnabled());
   }

   private void updateInputToolTip() {
      JComponent inputComponent = getInputComponent();
      inputComponent.setToolTipText(ParameterGuiUtils.getInputToolTip(parameter));
   }

   public abstract void installGUI(GridBag gridBag);

   /**
    * Update the GUI when the parameter value has changed.
    * Implementation should call {@link #updateEnabledState(List)}.
    */
   public abstract void updateInput();

   public abstract JComponent getInputComponent();

   public boolean commitEdit() {
      return true;
   }

   public void setHighlight(@Nullable Color color) {
      nameLabel.setHighlight(color);
   }
}
