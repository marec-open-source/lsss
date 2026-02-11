package no.imr.lsss.framework.config.application.packages;

import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.nio.file.Path;

final class IconSelector implements ViewHolder.View {
   private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
   private final JLabel label = new JLabel();
   private final JButton removeButton = MiscIcons.DELETE.on(new JButton("Remove"));
   private final Path dir;
   private String path;

   IconSelector(Path dir, String path) {
      this.dir = dir;
      this.path = path;

      JButton browseButton = MiscIcons.OPEN.on(new JButton("Browse"));

      panel.add(label);
      panel.add(browseButton);
      panel.add(removeButton);

      Insets margin = new Insets(0, 0, 0, 0);
      browseButton.setMargin(margin);
      removeButton.setMargin(margin);

      browseButton.addActionListener(_ -> browse());
      removeButton.addActionListener(_ -> setPath(""));

      updateIconButton();
   }

   @Override
   public JComponent getComponent() {
      return panel;
   }

   String getPath() {
      return path;
   }

   private void setPath(String path) {
      this.path = FileUtils.toSlashSeparatorChar(path);
      updateIconButton();
   }

   private void updateIconButton() {
      if (path.isEmpty()) {
         label.setText("No icon");
         label.setIcon(null);
         removeButton.setVisible(false);
      } else {
         label.setText(path);
         UserDefinedAction.createIcon(dir, path).on(label);
         removeButton.setVisible(true);
      }
   }

   private void browse() {
      JFileChooser fileChooser = new JFileChooser(dir.toFile());
      GuiUtils.setFileListTransferHandler(fileChooser);
      if (!path.isEmpty()) {
         fileChooser.setSelectedFile(dir.resolve(FileUtils.toNativeSeparatorChar(path)).toFile());
      }
      fileChooser.setFileFilter(new SuffixFileFilter("SVG files", ".svg"));
      int returnVal = fileChooser.showOpenDialog(GuiUtils.windowForComponent(panel));
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         Path file = fileChooser.getSelectedFile().toPath();
         String relativePath = FileUtils.relativePath(file, dir);
         setPath(relativePath != null ? relativePath : file.toString());
      }
   }
}
