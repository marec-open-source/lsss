package no.imr.tools.swing;

import no.imr.tools.parameter.Unit;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class SimpleInputDialog<T> {
   private final String title;
   private final String inputLabel;
   private final String startValue;
   private final Function<String, T> stringToValue;
   private @Nullable String rightText;
   private @Nullable String aboveText;
   private @Nullable String belowText;

   public SimpleInputDialog(String title, String inputLabel, String startValue, Function<String, T> stringToValue) {
      this.title = title;
      this.inputLabel = inputLabel;
      this.startValue = startValue;
      this.stringToValue = stringToValue;
   }

   public SimpleInputDialog<T> setUnit(Unit unit) {
      rightText = '[' + unit.text() + ']';
      return this;
   }

   public SimpleInputDialog<T> setAboveText(String aboveText) {
      this.aboveText = aboveText;
      return this;
   }

   public SimpleInputDialog<T> setBelowText(String belowText) {
      this.belowText = belowText;
      return this;
   }

   public Optional<T> show(@Nullable Component referenceComponent) {
      AtomicReference<@Nullable T> value = new AtomicReference<>();

      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.DOCUMENT_MODAL);

      JLabel label = new JLabel(inputLabel + ':');
      label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

      JTextField textField = new JTextField(startValue, 15);
      textField.selectAll();

      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> {
         try {
            value.set(stringToValue.apply(textField.getText()));
         } catch (Exception _) {
            // Conversion from string to value failed. Do nothing, try again.
            return;
         }
         dialog.dispose();
      });

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> dialog.dispose());

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
      buttonPanel.add(okButton);
      buttonPanel.add(Box.createHorizontalStrut(5));
      buttonPanel.add(cancelButton);

      JPanel contentPanel = new JPanel(new BorderLayout());
      contentPanel.add(label, BorderLayout.WEST);
      contentPanel.add(textField);
      if (rightText != null) {
         JLabel rightLabel = new JLabel(rightText);
         rightLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
         contentPanel.add(rightLabel, BorderLayout.EAST);
      }
      if (aboveText != null) {
         JLabel aboveLabel = new JLabel(aboveText);
         aboveLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
         contentPanel.add(aboveLabel, BorderLayout.NORTH);
      }
      if (belowText != null) {
         JLabel belowLabel = new JLabel(belowText);
         belowLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
         contentPanel.add(belowLabel, BorderLayout.SOUTH);
      }

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
      mainPanel.add(contentPanel);
      mainPanel.add(BorderLayout.SOUTH, buttonPanel);

      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.getContentPane().add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(referenceComponent);

      dialog.setVisible(true);

      return Optional.ofNullable(value.get());
   }
}
