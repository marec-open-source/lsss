package no.imr.korona.apps.relay;

import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

final class StatusFileError {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   private final Runnable reCreateStatusXml;
   private @Nullable Exception exception;

   StatusFileError(Runnable reCreateStatusXml) {
      this.reCreateStatusXml = reCreateStatusXml;
   }

   void setError(@Nullable Exception exception) {
      this.exception = exception;
      viewHolder.ifView(view -> view.update(exception));
   }

   JComponent getComponent() {
      return viewHolder.getComponent();
   }

   private static final class View implements ViewHolder.View {
      private final JPanel panel = new JPanel(new BorderLayout());
      private final JTextPane text = new JTextPane();

      private View(StatusFileError statusFileError) {
         text.setEditable(false);
         text.setFont(UiUtils.labelFont().deriveFont(Font.BOLD, 14));
         text.setBackground(ColorUtils.TOMATO);

         JButton reCreateButton = new JButton("Re-create status.xml");
         reCreateButton.addActionListener(_ -> statusFileError.reCreateStatusXml.run());

         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
         buttonPanel.setBackground(ColorUtils.TOMATO);
         buttonPanel.add(reCreateButton);

         panel.setBorder(BorderFactory.createEtchedBorder());
         panel.setBackground(ColorUtils.TOMATO);
         panel.add(text);
         panel.add(buttonPanel, BorderLayout.SOUTH);

         update(statusFileError.exception);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      private void update(@Nullable Exception exception) {
         panel.setVisible(exception != null);
         if (exception != null) {
            StringBuilder message = new StringBuilder()
                  .append(exception.getMessage());
            Throwable cause = exception.getCause();
            if (cause != null) {
               message.append('\n').append(cause);
            }
            text.setText(message.toString());
         } else {
            text.setText(null);
         }
      }
   }
}
