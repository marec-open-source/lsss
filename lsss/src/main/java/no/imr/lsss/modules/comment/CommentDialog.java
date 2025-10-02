package no.imr.lsss.modules.comment;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.StandardComment;
import no.imr.lsss.database.tables.hibernate.StandardCommentPK;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DoubleParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ScrollablePanel;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.List;

final class CommentDialog {
   private final ObjectParameter<StandardComment> standardComment;
   private final DoubleParameter value;
   private final TextParameter text;

   private @Nullable Comment editedComment;

   CommentDialog(LSSS lsss, Platform platform, Comment comment, Mode mode) {
      JFrame frame = lsss.getFrame();
      JDialog dialog = new JDialog(frame, mode.text + " comment", Dialog.ModalityType.DOCUMENT_MODAL);

      StringParameter time = new StringParameter(new Name("Time", "Time [UTC]"));
      time.setEnabled(false);
      time.setValue(CommentDataModule.DATE_TIME_FORMATTER.format(comment.toInstant()));

      List<StandardComment> standardComments = lsss.getDatabaseManager().getDatabaseData().getStandardComments(platform).getAll();
      StandardComment referencedStandardComment = standardComments.stream()
            .filter(standardComment -> standardComment.getCompId().getStandardComment() == comment.standardComment())
            .findFirst()
            .orElse(null);
      if (referencedStandardComment == null) {
         StandardCommentPK standardCommentPK = new StandardCommentPK(
               platform.getCompId().getNation(),
               platform.getCompId().getPlatform(),
               comment.standardComment());
         referencedStandardComment = new StandardComment(standardCommentPK, "<Standard comment not in database>");
         standardComments = Utils.toList(standardComments, List.of(referencedStandardComment));
      }

      standardComment = new ObjectParameter<>(
            new Name("StandardComment", "Standard comment"),
            referencedStandardComment, standardComments) {
         @Override
         public String toString(StandardComment standardComment) {
            return standardComment.getText();
         }
      };

      value = new DoubleParameter(new Name("Value"),
            comment.value(), Unit.NONE);

      text = new TextParameter(new Name("Text"),
            comment.text(), ValueConstraints.maxLength(DatabaseData.MAX_COMMENT_LENGTH));

      standardComment.addListenerAndNotify(stdComment -> {
         if (stdComment.getCompId().getStandardComment() == StandardComment.FREE_TEXT_STANDARD_COMMENT) {
            text.setEnabled(true);
         } else {
            text.setEnabled(false);
            text.setValue("");
         }
      });

      ParameterEditor parameterEditor = new ParameterEditor(List.of(time, standardComment, value, text));
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      if (mode == Mode.VIEW) {
         parameterEditor.getGUIConfig().setParameterEnabledDecider(__ -> false);
      }
      BaseParameter<?> focusedParameter = comment.standardComment() == StandardComment.FREE_TEXT_STANDARD_COMMENT ? text : value;
      SwingUtilities.invokeLater(parameterEditor.getInputComponent(focusedParameter)::requestFocusInWindow);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      JButton okButton = new JButton("OK");
      buttonPanel.add(okButton);
      okButton.addActionListener(e -> {
         if (!parameterEditor.commitEdits()) {
            return;
         }
         editedComment = new Comment(
               comment.timeInMillis(),
               standardComment.getValue().getCompId().getStandardComment(),
               value.getDoubleValue(),
               text.getValue()
         );
         dialog.dispose();
      });

      JButton cancelButton = new JButton("Cancel");
      buttonPanel.add(cancelButton);
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(e -> dialog.dispose());

      ScrollablePanel editorComponent = parameterEditor.getEditorComponent();
      editorComponent.setBorder(GuiUtils.DEFAULT_MARGIN);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(new JScrollPane(editorComponent));
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.add(mainPanel);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.pack();
      dialog.setLocationRelativeTo(frame);
      dialog.setVisible(true);
   }

   @Nullable Comment getEditedComment() {
      return editedComment;
   }

   enum Mode {
      ADD("Add"),
      EDIT("Edit"),
      VIEW("View");

      final String text;

      Mode(String text) {
         this.text = text;
      }
   }
}
