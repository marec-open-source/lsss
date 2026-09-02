package no.imr.korona.data.datamanager.labelling;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.CustomGuiParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DataFileLabelEditor {
   private final CustomGuiParameter color;
   private final StringParameter title;
   private final StringParameter description;

   private Color backgroundColor;
   private @Nullable DataFileLabel editedLabel;

   public DataFileLabelEditor(DataFileLabelling dataFileLabelling, DataFileLabel label) {
      Set<String> existingTitles = dataFileLabelling.getAllLabels().stream()
            .filter(l -> l != label)
            .map(l -> l.title)
            .collect(Collectors.toUnmodifiableSet());
      backgroundColor = label.backgroundColor;
      color = new CustomGuiParameter(new Name("Color"));
      color.setComponentSupplier(this::makeColorGui);
      title = new StringParameter(new Name("Title"), label.title, value -> {
         return !value.isEmpty() && existingTitles.contains(value)
               ? "Title already used by another label"
               : null;
      });
      description = new StringParameter(new Name("Description"), label.description);

      title.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      description.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      description.setProperty(BaseParameter.KEY_HORIZONTAL_FILL, true);
   }

   private JComponent makeColorGui() {
      JButton button = new JButton("Select color");
      button.setMargin(new Insets(0, 15, 0, 15));
      Runnable updateButtonColors = () -> {
         button.setBackground(backgroundColor);
         button.setForeground(ColorUtils.contrastingBlackOrWhite(backgroundColor));
      };
      updateButtonColors.run();
      button.addActionListener(_ -> {
         Color chosenColor = JColorChooser.showDialog(GuiUtils.windowForComponent(button),
               "Color", backgroundColor, false);
         if (chosenColor != null) {
            backgroundColor = chosenColor;
            updateButtonColors.run();
         }
      });
      Box panel = Box.createHorizontalBox();
      panel.add(button);
      return panel;
   }

   public DataFileLabelEditor show(@Nullable Component referenceComponent, String dialogTitle) {
      List<BaseParameter<?>> parameters = List.of(color, title, description);
      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      JComponent focusedComponent = parameterEditor.getInputComponent(title.getValue().isEmpty() ? title : description);
      SwingUtilities.invokeLater(focusedComponent::requestFocusInWindow);
      boolean ok = new ConfigurableGUIDialog(referenceComponent, dialogTitle, new ParameterCollection(parameters))
            .setCloseOnOk(() -> {
               if (title.getValue().isEmpty()) {
                  JComponent titleInput = parameterEditor.getInputComponent(title);
                  JOptionPane.showMessageDialog(titleInput, "Title cannot be empty.");
                  titleInput.requestFocusInWindow();
                  return false;
               }
               return true;
            })
            .setMinimumSize(800, 0)
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (ok) {
         editedLabel = new DataFileLabel(title.getValue(), description.getValue(), backgroundColor);
      }
      return this;
   }

   public @Nullable DataFileLabel getEditedLabel() {
      return editedLabel;
   }
}
