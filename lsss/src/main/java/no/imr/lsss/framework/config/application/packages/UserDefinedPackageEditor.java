package no.imr.lsss.framework.config.application.packages;

import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

final class UserDefinedPackageEditor implements ParameterContainer {
   final StringParameter id = new StringParameter(new Name("Id"),
         "", this::validateId);

   final StringParameter label = new StringParameter(new Name("Label"));

   private final StringParameter version = new StringParameter(new Name("Version"));

   private final TextParameter description = new TextParameter(new Name("Description"));

   private final UserDefinedPackage userDefinedPackage;

   UserDefinedPackageEditor(UserDefinedPackage userDefinedPackage) {
      this.userDefinedPackage = userDefinedPackage;

      id.setValue(userDefinedPackage.id);
      label.setValue(userDefinedPackage.info.label);
      version.setValue(userDefinedPackage.info.version);
      description.setValue(userDefinedPackage.info.description);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            id,
            label,
            version,
            description
      );
   }

   private @Nullable String validateId(String newId) {
      if (newId.isEmpty()) {
         // Empty id must be allowed when creating an action.
         // Will be tested on ok in edit dialog.
         return null;
      }
      if (newId.equals(userDefinedPackage.id)) {
         return null;
      }
      Path newPackageDir = userDefinedPackage.getPackagesConf().getPackagesDir().resolve(newId);
      if (Files.exists(newPackageDir.resolve(UserDefinedPackage.PACKAGE_INFO_JSON))) {
         return "A package already exists with that ID";
      }
      return null;
   }

   boolean isOK(ParameterEditor parameterEditor) {
      if (id.getValue().isEmpty()) {
         JOptionPane.showMessageDialog(parameterEditor.getEditorComponent(), "ID cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
         parameterEditor.getInputComponent(id).requestFocusInWindow();
         return false;
      }
      return true;
   }

   void apply() {
      Path packagesDir = userDefinedPackage.getPackagesConf().getPackagesDir();
      if (!userDefinedPackage.id.equals(id.getValue()) && !userDefinedPackage.id.isEmpty()) {
         try {
            Files.move(packagesDir.resolve(userDefinedPackage.id), packagesDir.resolve(id.getValue()));
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error renaming package directory", e);
         }
      }

      userDefinedPackage.id = id.getValue();
      userDefinedPackage.info.label = label.getValue();
      userDefinedPackage.info.version = version.getValue();
      userDefinedPackage.info.description = description.getValue();

      userDefinedPackage.savePackageInfo();
   }
}
