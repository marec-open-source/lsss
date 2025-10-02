package no.imr.lsss.framework.config.application.packages;

import com.google.common.base.Splitter;
import com.google.common.html.HtmlEscapers;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.MainConfigurationUnit;
import no.imr.lsss.framework.config.application.LsssServerConf;
import no.imr.lsss.framework.config.application.packages.pojo.UserDefinedPackageInfo;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.tools.NoCanDoException;
import no.imr.tools.ResourceUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class PackagesConf extends ConfigurationUnit {
   private final FileParameter pythonExecutable = new FileParameter(
         new Name("PythonExecutable", "Python executable"),
         Path.of("python"), FileParameter.Mode.FILE) {
      @Override
      public List<Component> makeExtraGuiComponents() {
         JButton testButton = new JButton("Test");
         testButton.addActionListener(e -> testPython());
         return List.of(testButton);
      }
   };

   private final ViewHolder<PackagesConfView> viewHolder = new ViewHolder<>(() -> new PackagesConfView(this));

   private final LsssServerConf lsssServerConf;
   private final Path packagesConfigDir = MainConfigurationUnit.getConfigFile("packagesConfig");
   private final Path packagesDir = packagesConfigDir.resolve("packages");
   private final List<UserDefinedPackage> userDefinedPackages = new CopyOnWriteArrayList<>();
   private final ChangeManager changeManager = new ChangeManager();
   private final AsyncHandle asyncHandle = new AsyncHandle();

   private @Nullable UserDefinedPackage currentUserDefinedPackage;

   public PackagesConf(BaseSystemFeaturePlugin plugin, LsssServerConf lsssServerConf) {
      super(plugin, new Name("Packages"),
            "Packages extending LSSS with custom functionality (keystrokes, toolbar buttons, menus)");

      this.lsssServerConf = lsssServerConf;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            pythonExecutable
      );
   }

   @Override
   public void setup() {
      super.setup();

      lsssServerConf.serverPort.addListenerAndNotify(this::updateActionScriptInclude);
      reloadPackages();
   }

   public LsssServerConf getLsssServerConf() {
      return lsssServerConf;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public AsyncHandle getAsyncHandle() {
      return asyncHandle;
   }

   public Path getPackagesDir() {
      return packagesDir;
   }

   public List<UserDefinedPackage> getUserDefinedPackages() {
      return userDefinedPackages;
   }

   @Nullable UserDefinedPackage getUserDefinedPackage(String id) {
      return userDefinedPackages.stream()
            .filter(p -> p.id.equals(id))
            .findFirst()
            .orElse(null);
   }

   @Nullable UserDefinedPackage getCurrentUserDefinedPackage() {
      return currentUserDefinedPackage;
   }

   void setCurrentUserDefinedPackage(@Nullable UserDefinedPackage userDefinedPackage) {
      currentUserDefinedPackage = userDefinedPackage;
   }

   void addUserDefinedPackage(UserDefinedPackage userDefinedPackage) {
      userDefinedPackages.add(userDefinedPackage);
      sortPackages();
      userDefinedPackage.install();
      currentUserDefinedPackage = userDefinedPackage;
      changeManager.notifyListeners();
   }

   public void deleteUserDefinedPackage(UserDefinedPackage userDefinedPackage) {
      userDefinedPackages.remove(userDefinedPackage);
      userDefinedPackage.uninstall();
      Path dir = packagesDir.resolve(userDefinedPackage.id);
      try {
         FileUtils.deleteRecursively(dir);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error deleting " + dir, e);
         reloadPackages();
         return;
      }
      if (currentUserDefinedPackage == userDefinedPackage) {
         currentUserDefinedPackage = userDefinedPackages.isEmpty() ? null : userDefinedPackages.getFirst();
      }
      changeManager.notifyListeners();
   }

   void sortPackages() {
      userDefinedPackages.sort(Utils.comparingIgnoringCase(UserDefinedPackage::getEffectiveLabel));
   }

   private void updateActionScriptInclude(int serverPort) {
      Path file = packagesConfigDir.resolve("include").resolve("lsss.py");
      String content = ResourceUtils.getString("no/imr/lsss/resources/config/packages/lsss.py")
            .replaceFirst("'@baseUrl@'", "'http://127.0.0.1:' + os.environ.get('LSSS_SERVER_PORT', '" + serverPort + "')");
      try {
         FileUtils.replaceFileSafely(file, content, Utils.UTF_8);
      } catch (IOException e) {
         getLSSS().showError("Error updating " + file, e);
      }
   }

   static String generatedHeader() {
      return ResourceUtils.getString("no/imr/lsss/resources/config/packages/generatedHeader.py");
   }

   @Override
   public JComponent getComponent() {
      PackagesConfView view = viewHolder.getView();
      return view.getComponent();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   public void reloadPackages() {
      userDefinedPackages.forEach(UserDefinedPackage::uninstall);
      userDefinedPackages.clear();
      userDefinedPackages.addAll(loadPackages());
      sortPackages();
      if (currentUserDefinedPackage != null) {
         currentUserDefinedPackage = getUserDefinedPackage(currentUserDefinedPackage.id);
      }
      if (currentUserDefinedPackage == null && !userDefinedPackages.isEmpty()) {
         currentUserDefinedPackage = userDefinedPackages.getFirst();
      }
      userDefinedPackages.forEach(UserDefinedPackage::install);
      changeManager.notifyListeners();
   }

   private List<UserDefinedPackage> loadPackages() {
      List<FileInfo> fileInfos;
      try {
         fileInfos = FileUtils.listFilesWithAttributes(packagesDir, FileInfo::isDirectory);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing actions in " + packagesDir, e);
         return List.of();
      }
      return fileInfos.stream()
            .map(fileInfo -> {
               UserDefinedPackageInfo info = UserDefinedPackage.loadPackageInfo(fileInfo.file());
               if (info == null) {
                  return null;
               }
               return new UserDefinedPackage(this, fileInfo.file(), info);
            })
            .filter(Objects::nonNull)
            .toList();
   }

   private void testPython() {
      new WorkerDialog(getLSSS().getReferenceComponent(), "Testing Python")
            .setWaitUntilFinishedIfCancelled(false)
            .setOnError(e -> getLSSS().showError("Error executing Python:\n" + e, e))
            .start(asyncHandle -> {
               String script = ResourceUtils.getString("no/imr/lsss/resources/config/packages/testPython.py");
               String outputString = executePythonScript(script);
               if (asyncHandle.isCancelled()) {
                  return;
               }
               SwingUtilities.invokeLater(() -> {
                  List<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().splitToList(outputString);
                  if (lines.size() == 3 && lines.get(0).equals("test-from-lsss")) {
                     JOptionPane.showMessageDialog(getLSSS().getReferenceComponent(),
                           "Python version: " + lines.get(1)
                                 + "\nRequests library: " + lines.get(2));
                  } else {
                     getLSSS().showError("Unexpected output:\n\n" + outputString);
                  }
               });
            });
   }

   private String executePythonScript(String script) throws IOException, InterruptedException {
      Path dir = Utils.getTmpDir();
      FileUtils.createDirectories(dir);
      Path tmpPy = Files.createTempFile(dir, "tmp-python-", ".py");
      try {
         Files.writeString(tmpPy, script, Utils.UTF_8);
         return executePythonScript(tmpPy, new ActionArgument());
      } finally {
         Files.delete(tmpPy);
      }
   }

   private String executePythonScript(Path file, ActionArgument argument) throws IOException, InterruptedException {
      ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable.getStringValue(), file.toString())
            .directory(file.getParent().toFile())
            .redirectErrorStream(true);
      processBuilder.environment().put("PYTHONIOENCODING", "UTF-8");
      processBuilder.environment().put("LSSS_SERVER_PORT", String.valueOf(lsssServerConf.serverPort.getIntValue()));
      processBuilder.environment().put("LSSS_INPUT", JsonUtils.JSON_MAPPER.writeValueAsString(argument.input()));
      Process process = processBuilder.start();
      byte[] outputBytes = process.getInputStream().readAllBytes();
      String outputString = new String(outputBytes, Utils.UTF_8);
      process.waitFor();
      int exitCode = process.exitValue();
      if (exitCode != 0) {
         throw new IOException("Exit code: " + exitCode + ", Output: " + outputString);
      }
      return outputString;
   }

   void executeActionScript(Supplier<@Nullable Component> referenceComponent, UserDefinedAction action, ActionArgument argument, boolean showDialog) {
      if (showDialog) {
         executeActionScriptWithDialog(referenceComponent, action, argument);
      } else {
         Exec.CACHED_THREAD_POOL.execute(asyncHandle.createManagedRunnable(() -> {
            executeActionScriptWithoutDialog(referenceComponent, action, argument);
         }));
      }
   }

   private void executeActionScriptWithDialog(Supplier<@Nullable Component> referenceComponent, UserDefinedAction action, ActionArgument argument) {
      String message = "<html>Executing action: " + HtmlEscapers.htmlEscaper().escape(action.getEffectiveLabel())
            + " <span style='color: gray;'>(" + HtmlEscapers.htmlEscaper().escape(action.getUserDefinedPackage().getEffectiveLabel()) + ")</span>";
      new WorkerDialog(referenceComponent, message)
            .setWaitUntilFinishedIfCancelled(false)
            .start(asyncHandle -> {
               executeActionScriptWithoutDialog(referenceComponent, action, argument);
            });
   }

   private void executeActionScriptWithoutDialog(Supplier<@Nullable Component> referenceComponent, UserDefinedAction action, ActionArgument argument) {
      try {
         executePythonScript(action.getMainFile(), argument);
      } catch (Exception e) {
         if (getLSSS().getInterpretationSettings().isInteractiveMode()) {
            SwingUtilities.invokeLater(() -> getLSSS().showError(referenceComponent.get(), "Error executing action " + action.getTotalId(), e));
         } else {
            throw new NoCanDoException("Error executing action " + action.getTotalId() + ": " + e);
         }
      }
   }
}
