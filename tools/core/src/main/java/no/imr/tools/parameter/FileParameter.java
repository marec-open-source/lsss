package no.imr.tools.parameter;

import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.parameter.misc.ReferenceDirectoryManager;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Parameter for selecting a file.
 */
public class FileParameter extends OptionalParameter<Path> {
   public static final String XML_REF = "ref";

   public enum Mode {
      FILE, DIRECTORY, FILE_OR_DIRECTORY
   }

   private @Nullable ReferenceDirectoryManager referenceDirectoryManager;
   private final Mode mode;
   private @Nullable Consumer<JPopupMenu> popupMenuExtender;

   public FileParameter(Name name, @Nullable Path initialValue, Mode mode) {
      this(name, initialValue, mode, "");
   }

   public FileParameter(Name name, @Nullable Path initialValue, Mode mode, String description) {
      super(name, Optional.ofNullable(initialValue), Unit.NONE, description, ValueConverters.OPTIONAL_PATH);

      this.mode = mode;
      setProperty(KEY_LEFT_ALIGNED, true);
   }

   public Mode getMode() {
      return mode;
   }

   public @Nullable Path getFile() {
      return getValue().orElse(null);
   }

   public void setFile(@Nullable Path file) {
      setValue(Optional.ofNullable(file));
   }

   public boolean exists() {
      Path file = getFile();
      return file != null && Files.exists(file);
   }

   public boolean isDirectory() {
      Path file = getFile();
      return file != null && Files.isDirectory(file);
   }

   /**
    * Creates directory if not null.
    *
    * @throws IOException if directory could not be created
    */
   public void createDirectories() throws IOException {
      Path file = getFile();
      if (file != null) {
         FileUtils.createDirectories(file);
         notifyListeners();
      }
   }

   public void setReferenceDirectoryManager(ReferenceDirectoryManager referenceDirectoryManager) {
      this.referenceDirectoryManager = referenceDirectoryManager;
   }

   @Override
   public Element toXml() {
      Path file = getFile();
      if (referenceDirectoryManager != null && file != null) {
         ReferenceDirectory referenceDirectory = referenceDirectoryManager.getReferenceDirectory(file);
         if (referenceDirectory != null && referenceDirectory.getFile() != null) {
            String relativePath = FileUtils.relativePath(file, referenceDirectory.getFile());
            if (relativePath != null) {
               return createElement()
                     .addAttribute(XML_REF, referenceDirectory.getPersistentName())
                     .addText(relativePath);
            }
         }
      }

      return super.toXml();
   }

   @Override
   public Optional<Path> xmlToValue(Element element) {
      Path file = xmlToValueUsingReferenceDir(element);
      if (file != null) {
         return Optional.of(file);
      }
      return super.xmlToValue(element);
   }

   private @Nullable Path xmlToValueUsingReferenceDir(Element element) {
      String ref = element.attributeValue(XML_REF);
      if (ref == null) {
         return null;
      }
      if (referenceDirectoryManager == null) {
         Log.global.warning("No reference directory manager for " + XmlUtils.getPath(element));
         return null;
      }
      ReferenceDirectory referenceDirectory = referenceDirectoryManager.getReferenceDirectory(ref);
      if (referenceDirectory == null) {
         Log.global.warning("No reference directory " + ref + " for " + XmlUtils.getPath(element));
         return null;
      }
      Path dir = referenceDirectory.getFile();
      if (dir == null) {
         Log.global.warning("Reference directory " + ref + " not specified for " + XmlUtils.getPath(element));
         return null;
      }
      String relativePath = FileUtils.toNativeSeparatorChar(element.getText());
      return dir.resolve(relativePath).normalize();
   }

   public List<FileFilter> getFileFilters() {
      return List.of();
   }

   public void customizeFileChooser(JFileChooser fileChooser) {
   }

   public @Nullable Path getDefaultBrowseDirectory() {
      return null;
   }

   public JFileChooser createFileChooser() {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

      Path file = getFile();
      if (file != null) {
         if (Files.exists(file)) {
            fileChooser.setSelectedFile(file.toFile());
         } else {
            fileChooser.setCurrentDirectory(FileUtils.toFile(FileUtils.getExistingParent(file)));
         }
      } else {
         fileChooser.setCurrentDirectory(FileUtils.toFile(getDefaultBrowseDirectory()));
      }

      List<FileFilter> fileFilters = getFileFilters();
      for (FileFilter fileFilter : fileFilters) {
         fileChooser.addChoosableFileFilter(fileFilter);
      }
      if (!fileFilters.isEmpty()) {
         fileChooser.setFileFilter(fileFilters.getFirst());
      }

      customizeFileChooser(fileChooser);

      return fileChooser;
   }

   public void applyFileChooserResult(Path selectedFile) {
      setFile(selectedFile);
   }

   public @Nullable Consumer<JPopupMenu> getPopupMenuExtender() {
      return popupMenuExtender;
   }

   public void setPopupMenuExtender(Consumer<JPopupMenu> popupMenuExtender) {
      this.popupMenuExtender = popupMenuExtender;
   }

   public void saveXml(@Nullable Component referenceComponent, Element element) {
      Path file = getFile();
      if (file == null) {
         JFileChooser fileChooser = createFileChooser();
         int returnState = fileChooser.showSaveDialog(referenceComponent);
         if (returnState == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile().toPath();
            setFile(file);
         } else {
            return;
         }
      }

      try {
         XmlUtils.writeDocument(element, file);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(referenceComponent, "Error saving " + file, e);
      }
   }

   public List<Component> makeExtraGuiComponents() {
      return List.of();
   }

   public @Nullable Editor getEditor() {
      return null;
   }

   public @Nullable Copier getCopier() {
      return null;
   }

   public interface Editor {
      boolean edit(@Nullable Component referenceComponent, boolean editable);

      boolean createNew(@Nullable Component referenceComponent);
   }

   public interface Copier {
      void copy(@Nullable Component referenceComponent);

      void move(@Nullable Component referenceComponent);
   }

   public static final class DefaultCopier implements Copier {
      private final FileParameter fileParameter;

      public DefaultCopier(FileParameter fileParameter) {
         this.fileParameter = fileParameter;
      }

      @Override
      public void copy(@Nullable Component referenceComponent) {
         copyOrMove(referenceComponent, true);
      }

      @Override
      public void move(@Nullable Component referenceComponent) {
         copyOrMove(referenceComponent, false);
      }

      private void copyOrMove(@Nullable Component referenceComponent, boolean copy) {
         Path currentFile = fileParameter.getFile();
         if (currentFile == null) {
            GuiUtils.showErrorDialog(referenceComponent, "No file selected");
            return;
         }
         JFileChooser fileChooser = new JFileChooser();
         fileChooser.setSelectedFile(currentFile.toFile());
         fileChooser.setDialogTitle(copy ? "Copy" : "Move");
         int returnVal = fileChooser.showSaveDialog(referenceComponent);
         if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
         }
         Path selectedFile = fileChooser.getSelectedFile().toPath();
         if (currentFile.equals(selectedFile)) {
            return;
         }
         if (Files.exists(selectedFile)) {
            int overwriteAnswer = GuiUtils.showOptionDialog(referenceComponent, "Overwrite",
                  "Overwrite existing file?\n"
                        + selectedFile,
                  new String[]{"Overwrite", "Cancel"});
            if (overwriteAnswer != 0) {
               return;
            }
         }
         try {
            if (copy) {
               FileUtils.copy(currentFile, selectedFile);
            } else {
               FileUtils.move(currentFile, selectedFile);
            }
            fileParameter.setFile(selectedFile);
         } catch (IOException e) {
            GuiUtils.showErrorDialog(referenceComponent, "Error " + (copy ? "copying" : "moving") + " " + currentFile + "\nto " + selectedFile, e);
         }
      }
   }
}
