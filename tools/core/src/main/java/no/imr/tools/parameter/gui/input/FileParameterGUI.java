package no.imr.tools.parameter.gui.input;

import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI for a {@link FileParameter}.
 */
public final class FileParameterGUI extends ParameterGUI<FileParameter> {
   private final ParameterTextField textField;

   private final GridBag descriptionGridBag = new GridBag();
   private final JButton browseButton = new JButton("Browse...");
   private final JButton createButton = new JButton("Create");
   private final JButton editButton = new JButton();
   private final FileParameter.@Nullable Editor editor;
   private boolean fileExists;
   private @Nullable String errorMessage;

   FileParameterGUI(FileParameter parameter, GUIConfig guiConfig) {
      super(parameter, guiConfig);

      textField = new ParameterTextField(parameter, guiConfig);
      textField.getComponent().addMouseListener(new PopupMenuMouseListener(e -> makePopupMenu()));
      /* todo: See ticket #677. Setting transfer handler disabled copy / paste.
      textField.getComponent().setTransferHandler(new FileListTransferHandler() {
         @Override
         protected void importFiles(List<File> files) {
            if (!files.isEmpty() && textField.getComponent().isEnabled()) {
               getParameter().setFile(files.get(0));
            }
         }
      });
      */

      browseButton.addActionListener(e -> browse());

      createButton.setVisible(false);
      createButton.setMargin(new Insets(1, 1, 1, 1));
      createButton.setBackground(ColorUtils.LEMONCHIFFON);
      createButton.setToolTipText("Directory does not exist!");
      createButton.addActionListener(e -> createDirectory());

      editor = getParameter().getEditor();
      if (editor != null) {
         editButton.addActionListener(e -> {
            boolean editable = getGUIConfig().isParameterEnabled(getParameter());
            if (getParameter().exists()) {
               editor.edit(editButton, editable);
            } else {
               if (editable) {
                  editor.createNew(editButton);
               } else {
                  JOptionPane.showMessageDialog(editButton, getParameter().getFile() != null ? "File does not exist." : "No file specified.");
               }
            }
            getParameter().notifyListeners();
         });
      }
   }

   @Override
   public void installGUI(GridBag gridBag) {
      descriptionGridBag.add(browseButton);
      descriptionGridBag.getConstraints().insets = new Insets(0, 3, 0, 0);

      JMenuBar menuBar = new JMenuBar();
      JMenu menu = menuBar.add(MiscIcons.MENU.on(new JMenu()));
      GuiUtils.autoCreateContentMenu(menu, () -> addToPopupMenu(menu.getPopupMenu()));
      descriptionGridBag.add(menuBar);

      descriptionGridBag.add(createButton);

      if (editor != null) {
         descriptionGridBag.add(editButton);
      }

      getParameter().makeExtraGuiComponents().forEach(descriptionGridBag::add);

      addName(gridBag);
      gridBag.activateHorizontalFill();
      gridBag.add(textField.getComponent());
      gridBag.deactivateFill();
      addInputAndDescription(gridBag, descriptionGridBag.getPanel());
   }

   @Override
   public void updateInput() {
      descriptionGridBag.getPanel().setVisible(getParameter().isVisible());
      updateEnabledState(List.of(textField.getComponent(), browseButton)); // editButton should always be enabled.
      updateEditButton();
      Exec.CACHED_THREAD_POOL.execute(() -> {
         updateFileAttributes(); // In background thread since remote file system access can be slow.
         SwingUtilities.invokeLater(() -> {
            updateTextField();
            updateCreateButton();
         });
      });
   }

   private void updateFileAttributes() {
      fileExists = false;
      errorMessage = null;
      Path file = getParameter().getFile();
      if (file == null) {
         return;
      }
      BasicFileAttributes attributes;
      try {
         attributes = FileUtils.readAttributesIfExists(file);
      } catch (IOException e) {
         errorMessage = new HtmlStringBuilder().text("Error reading file attributes:").html("<br>").text(e.toString()).build();
         return;
      }
      if (attributes == null) {
         return;
      }
      fileExists = true;
      if (getParameter().getMode() == FileParameter.Mode.DIRECTORY && attributes.isRegularFile()) {
         errorMessage = "Is a regular file, but should be a directory";
      }
      if (getParameter().getMode() == FileParameter.Mode.FILE && attributes.isDirectory()) {
         errorMessage = "Is a directory, but should be a regular file";
      }
   }

   private void updateTextField() {
      textField.updateComponent();
      JFormattedTextField textFieldComponent = textField.getComponent();
      if (errorMessage != null) {
         textFieldComponent.setBackground(ColorUtils.TOMATO);
         textFieldComponent.setToolTipText(errorMessage);
      } else {
         Path file = getParameter().getFile();
         if (file != null && !fileExists) {
            textFieldComponent.setBackground(ColorUtils.LEMONCHIFFON);
            textFieldComponent.setToolTipText("Does not exist");
         }
      }
   }

   @Override
   public JComponent getInputComponent() {
      return textField.getComponent();
   }

   @Override
   public boolean commitEdit() {
      return textField.commitEdit();
   }

   private void updateCreateButton() {
      boolean visible = getParameter().isVisible()                      // parameter is visible
            && getParameter().getMode() == FileParameter.Mode.DIRECTORY // is directory
            && getParameter().getFile() != null                         // is specified
            && !fileExists;                                             // does not exist

      createButton.setVisible(visible);
   }

   private void updateEditButton() {
      if (getGUIConfig().isParameterEnabled(getParameter())) {
         editButton.setText("Edit...");
      } else {
         editButton.setText("View...");
      }
   }

   private void browse() {
      JFileChooser fileChooser = getParameter().createFileChooser();

      int returnState = fileChooser.showOpenDialog(browseButton);
      if (returnState == JFileChooser.APPROVE_OPTION) {
         getParameter().applyFileChooser(fileChooser);
      }
   }

   private void createDirectory() {
      try {
         getParameter().createDirectories();
      } catch (IOException e) {
         GuiUtils.showErrorDialog(createButton, "Could not create directory\n" + getParameter().getFile(), e);
      }
   }

   private JPopupMenu makePopupMenu() {
      JPopupMenu menu = new JPopupMenu();
      addToPopupMenu(menu);
      return menu;
   }

   private void addToPopupMenu(JPopupMenu menu) {
      menu.add(MenuItems.showInFileExplorer(getParameter().getFile()));

      FileParameter.Copier copier = getParameter().getCopier();
      if (copier != null) {
         boolean enabled = fileExists && getGUIConfig().isParameterEnabled(getParameter());

         JMenuItem copyItem = MiscIcons.COPY.on(menu.add("Copy"));
         copyItem.setEnabled(enabled);
         copyItem.addActionListener(e -> {
            copier.copy(textField.getComponent());
         });

         JMenuItem moveItem = menu.add("Move");
         moveItem.setEnabled(enabled);
         moveItem.addActionListener(e -> {
            copier.move(textField.getComponent());
         });
      }

      Consumer<JPopupMenu> popupMenuExtender = getParameter().getPopupMenuExtender();
      if (popupMenuExtender != null) {
         popupMenuExtender.accept(menu);
      }
   }
}
