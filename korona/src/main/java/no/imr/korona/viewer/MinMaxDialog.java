package no.imr.korona.viewer;

import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/**
 * Dialog for setting min/max in a dialog using JSpinner.
 */
final class MinMaxDialog {
   private final Consumer<FloatRange> setRange;
   private final FloatRange backupRange;
   private final SpinnerNumberModel minModel;
   private final SpinnerNumberModel maxModel;

   MinMaxDialog(Component referenceComponent, String title,
                FloatRange range, float delta, @Nullable FloatRange maxRange, Consumer<FloatRange> setRange) {
      this.setRange = setRange;
      backupRange = range;

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.DOCUMENT_MODAL);

      minModel = new SpinnerNumberModel((Float) range.min(),
            maxRange != null ? maxRange.min() : null,
            maxRange != null ? maxRange.max() - delta : null,
            (Float) delta);
      maxModel = new SpinnerNumberModel((Float) range.max(),
            maxRange != null ? maxRange.min() + delta : null,
            maxRange != null ? maxRange.max() : null,
            (Float) delta);

      JSpinner minSpinner = createSpinner(minModel, e -> minValueChanged());
      JSpinner maxSpinner = createSpinner(maxModel, e -> maxValueChanged());

      JButton okButton = new JButton("OK");
      okButton.addActionListener(e -> dialog.dispose());

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(e -> {
         setRange.accept(backupRange);
         dialog.dispose();
      });
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);

      JPanel minMaxPanel = new JPanel(new FlowLayout());
      minMaxPanel.setBorder(BorderFactory.createEtchedBorder());
      minMaxPanel.add(new JLabel("Min"));
      minMaxPanel.add(minSpinner);
      minMaxPanel.add(Box.createHorizontalStrut(10));
      minMaxPanel.add(new JLabel("Max"));
      minMaxPanel.add(maxSpinner);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(minMaxPanel);
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.add(mainPanel);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.pack();
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private static JSpinner createSpinner(SpinnerNumberModel model, ChangeListener listener) {
      JSpinner spinner = new JSpinner(model);
      spinner.addChangeListener(listener);
      JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner);
      editor.getTextField().setColumns(4);
      spinner.setEditor(editor);
      return spinner;
   }

   private void minValueChanged() {
      float min = minModel.getNumber().floatValue();
      float max = maxModel.getNumber().floatValue();
      if (min >= max) {
         max = min + maxModel.getStepSize().floatValue();
         maxModel.setValue(max);
      }
      setRange.accept(FloatRange.of(min, max));
   }

   private void maxValueChanged() {
      float min = minModel.getNumber().floatValue();
      float max = maxModel.getNumber().floatValue();
      if (min >= max) {
         min = max - minModel.getStepSize().floatValue();
         minModel.setValue(min);
      }
      setRange.accept(FloatRange.of(min, max));
   }
}
