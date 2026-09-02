package no.imr.lsss.framework.config.application;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.referencedata.DbReferenceDataExporter;
import no.imr.lsss.database.referencedata.DbReferenceDataImporter;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverters;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class DatabaseReferenceDataConf extends ConfigurationUnit {
   private final OptionalParameter<URI> referenceDataUrl = new OptionalParameter<>(
         new Name("referenceDataUrl", "Reference data URL"),
         Optional.empty(), Unit.NONE,
         "Location of database reference data",
         ValueConverters.optional(ValueConverters.of(DatabaseReferenceDataConf::stringToURI, URI::toString)),
         DatabaseReferenceDataConf::validateReferenceDataUri);

   private final ButtonParameter downloadReferenceData = new ButtonParameter(
         new Name("downloadReferenceData", "Download reference data"),
         "Downloads the reference data to a local file",
         this::downloadButtonClick);

   private final ButtonParameter importReferenceData = new ButtonParameter(
         new Name("importReferenceData", "Import reference data"),
         "Imports the downloaded reference data into the current database",
         this::importButtonClick);

   private final ButtonParameter exportReferenceData = new ButtonParameter(
         new Name("exportReferenceData", "Export reference data"),
         "Exports the reference data from the current database",
         this::exportButtonClick);

   DatabaseReferenceDataConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("DatabaseReferenceDataConf", "Reference data (incubating)"),
            "Configuration of database reference data");

      referenceDataUrl.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      referenceDataUrl.setProperty(BaseParameter.KEY_HORIZONTAL_FILL, true);
   }

   @Override
   public void setup() {
      Listener.of(this::updateEnabledState).addToAndNotify(
            referenceDataUrl,
            getLSSS().getDatabaseManager().getConnectionChangeManager()
      );
   }

   private void updateEnabledState() {
      boolean uriOK = referenceDataUrl.getValue().isPresent();
      boolean dbOK = getLSSS().getDatabaseManager().getDatabaseConnection().isConnected();
      downloadReferenceData.setEnabled(uriOK);
      importReferenceData.setEnabled(uriOK && dbOK);
      exportReferenceData.setEnabled(dbOK);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            referenceDataUrl,
            downloadReferenceData,
            importReferenceData,
            exportReferenceData
      );
   }

   @Override
   public JComponent getComponent() {
      Path downloadDir = getDownloadDir();
      return GuiUtils.createScrollPane(
            GuiUtils.addHrefListener(createInfoComponent("""
                  <h2>Database reference data</h2>
                  <p>
                     Database reference data contains definition of
                     biological species, acoustic categories, areas, platforms and standard comments.
                     LSSS comes bundled with database reference data, so specifying additional
                     reference data here is optional.
                  </p>
                  <p>
                     Reference data will be downloaded to: <a href="downloadDir">""" + downloadDir + """
                  </a>.
                     When <a href="initialize">initializing</a> a database, the downloaded reference data
                     will be used in addition to the reference data bundled with LSSS.
                  </p>
                  """), href -> {
               switch (href) {
                  case "downloadDir" -> {
                     try {
                        FileUtils.createDirectories(downloadDir);
                     } catch (IOException e) {
                        getLSSS().showError("Error creating " + downloadDir, e);
                        break;
                     }
                     GuiUtils.desktopBrowse(downloadDir.toUri(), getConfigurationManager().getDialog());
                  }
                  case "initialize" -> {
                     getConfigurationManager().showDialog(getConfigurationManager().getApplicationConfiguration().getDatabaseConf());
                  }
                  default -> {
                  }
               }
            }),
            createParameterEditor().getEditorComponent());
   }

   private Path getDownloadDir() {
      return getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getMainDir()
            .resolve("config", "DB-reference-data");
   }

   private Path getDownloadFile() {
      return getDownloadDir().resolve("DB-reference-data.json");
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      if (parameter == exportReferenceData) {
         return UserProfile.NORMAL_USE;
      }
      return UserProfile.ADMINISTRATOR_MODE;
   }

   private void downloadButtonClick() {
      URI uri = referenceDataUrl.getValue().orElse(null);
      if (uri == null) {
         return;
      }
      LSSS lsss = getLSSS();
      new WorkerDialog(lsss.getReferenceComponent(), "Downloading database reference data...")
            .start(asyncHandle -> {
               DbReferenceDataImporter.download(uri, getDownloadFile(), asyncHandle);
            });
   }

   private void importButtonClick() {
      LSSS lsss = getLSSS();
      new WorkerDialog(lsss.getReferenceComponent(), "Importing database reference data...")
            .start(this::doImportReferenceData);
   }

   private void exportButtonClick() {
      LSSS lsss = getLSSS();
      Path mainDir = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().getMainDir();

      FileParameter destinationDirectory = new FileParameter(
            new Name("Directory", "Destination directory"),
            mainDir.resolve("DB-reference-data-export"),
            FileParameter.Mode.DIRECTORY);

      ParameterEditor parameterEditor = new ParameterEditor(List.of(destinationDirectory));
      parameterEditor.getEditorComponent().setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

      boolean ok = new ConfigurableGUIDialog(lsss.getReferenceComponent(), "Export database reference data", new ParameterCollection(List.of(destinationDirectory)))
            .setMinimumSize(800, 0)
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (!ok) {
         return;
      }
      Path dir = destinationDirectory.getFile();
      if (dir == null) {
         return;
      }

      new WorkerDialog(lsss.getReferenceComponent(), "Exporting database reference data...")
            .startWithoutCancel(() -> {
               DbReferenceDataExporter.export(lsss.getDatabaseManager().getDatabaseConnection(), dir);
            });
   }

   public void doImportReferenceData(AsyncHandle asyncHandle) throws IOException {
      Path downloadFile = getDownloadFile();
      if (Files.exists(downloadFile)) {
         DbReferenceDataImporter.doImport(getLSSS(), downloadFile.toUri(), asyncHandle);
      }
   }

   private static URI stringToURI(String s) {
      try {
         return new URI(s);
      } catch (URISyntaxException e) {
         if (s.contains(File.separator)) {
            if (s.startsWith("\"") && s.endsWith("\"")) {
               s = s.substring(1, s.length() - 1);
            }
            try {
               Path path = Path.of(s);
               if (path.isAbsolute()) {
                  return path.toUri();
               }
            } catch (Exception _) {
               // Not a path.
            }
         }
         throw new IllegalArgumentException(e.getMessage(), e);
      }
   }

   private static @Nullable String validateReferenceDataUri(URI uri) {
      if (!uri.isAbsolute()) {
         return "Must be an absolute URL";
      }
      try {
         var _ = uri.toURL();
         return null;
      } catch (MalformedURLException e) {
         return "Malformed URL: " + e.getMessage();
      }
   }
}
