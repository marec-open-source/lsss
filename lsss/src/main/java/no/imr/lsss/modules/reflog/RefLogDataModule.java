package no.imr.lsss.modules.reflog;

import com.google.common.collect.ImmutableList;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.TableToolTipBuilder;
import no.imr.tools.swing.WorkerDialog;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Module loading ref log data.
 */
public final class RefLogDataModule extends BaseDataModule {
   static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("yyyy.MM.dd HH:mm:ss");

   private List<BooleanParameter> stationParameters = new ArrayList<>();
   private volatile @Nullable Set<String> selectedStationTypes;
   private final Object stationParametersLock = new Object();
   private final Listener stationParameterListener = this::onStationParameterChange;

   private final Map<BaseLsssModule, List<LogLine>> logLinesMap = new ConcurrentHashMap<>();
   private final ChangeManager changeManager = new ChangeManager();

   private @Nullable LogLine activeLogLine;
   private final ChangeManager activeLogLineChangeManager = new ChangeManager();
   private final ArgChangeManager<LogLine> logLineClickChangeManager = new ArgChangeManager<>();

   public RefLogDataModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return stationParameters;
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   public BaseParameter<?> possiblyCreateNewParameter(String persistentName) {
      BooleanParameter parameter = newStationTypeParameter(persistentName);
      stationParameters.add(parameter);
      stationParameters.sort(Comparator.comparing(BooleanParameter::getPersistentName));
      selectedStationTypes = null;
      return parameter;
   }

   @Override
   public void fromXml(Element element) {
      synchronized (stationParametersLock) {
         super.fromXml(element);
      }
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::updateDataDir), List.of(
            getConfigurationManager().getDataConf().getDir(DataConfLSSS.REF_LOG_SUB_DIR),
            getInterpretationSettings().getReloadChangeManager()
      ));
   }

   public void setLogLines(BaseLsssModule owner, List<LogLine> logLines) {
      executeIfEnabled(() -> {
         if (logLines.isEmpty()) {
            logLinesMap.remove(owner);
         } else {
            logLinesMap.put(owner, logLines);
         }
         updateStationParameters();
         changeManager.notifyListeners();
      });
   }

   @Nullable LogLine getActiveLogLine() {
      return activeLogLine;
   }

   void setActiveLogLine(@Nullable LogLine activeLogLine) {
      this.activeLogLine = activeLogLine;
      activeLogLineChangeManager.notifyListeners();
   }

   ChangeManager getActiveLogLineChangeManager() {
      return activeLogLineChangeManager;
   }

   public ArgChangeManager<LogLine> getLogLineClickChangeManager() {
      return logLineClickChangeManager;
   }

   void clickActiveLogLine() {
      if (activeLogLine != null) {
         logLineClickChangeManager.notifyListeners(activeLogLine);
      }
   }

   private void updateStationParameters() {
      synchronized (stationParametersLock) {
         stationParameters = createUpdatedStationParameters();
      }
   }

   private List<BooleanParameter> createUpdatedStationParameters() {
      Map<String, BooleanParameter> existingParameters = stationParameters.stream()
            .collect(Collectors.toMap(BooleanParameter::getPersistentName, Function.identity()));
      return getAllLogLines()
            .map(LogLine::stationType)
            .distinct()
            .sorted()
            .map(stationType -> {
               BooleanParameter parameter = existingParameters.get(stationType);
               return parameter != null ? parameter : newStationTypeParameter(stationType);
            })
            .collect(Collectors.toList());
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   @Override
   public JComponent createConfigurationEditor() {
      JComponent configurationEditor = super.createConfigurationEditor();
      if (configurationEditor == null) {
         return new JScrollPane(new JLabel("No ref log data available.", JLabel.CENTER));
      }
      return configurationEditor;
   }

   private void updateDataDir() {
      ImmutableList.Builder<LogLine> logLinesBuilder = ImmutableList.builder();
      getConfigurationManager().getDataConf().getDir(DataConfLSSS.REF_LOG_SUB_DIR).getValue().ifPresent(dir -> {
         new WorkerDialog(getLSSS()::getReferenceComponent, "Loading ref log data from\n" + dir)
               .setModalDialog(false)
               .start(asyncHandle -> {
                  List<Path> files;
                  try {
                     files = FileUtils.listFiles(dir, asyncHandle);
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, "Error listing files in " + dir, e);
                     return;
                  }
                  LoaderJson loaderJson = new LoaderJson();
                  for (Path file : files) {
                     if (asyncHandle.isCancelled()) {
                        break;
                     }
                     try {
                        String name = file.getFileName().toString();
                        if (name.endsWith(".json")) {
                           logLinesBuilder.addAll(loaderJson.load(file));
                        } else if (Utils.startsWithIgnoringCase(name, "ref") && Utils.endsWithIgnoringCase(name, ".csv")) {
                           logLinesBuilder.addAll(LoaderRefCsv.load(file));
                        }
                     } catch (Exception e) {
                        Log.global.log(Level.WARNING, "Error loading file " + file, e);
                     }
                  }
               });
      });
      setLogLines(this, logLinesBuilder.build());
   }

   static @Nullable String getToolTip(@Nullable LogLine logLine) {
      if (logLine == null) {
         return null;
      }
      TableToolTipBuilder toolTip = new TableToolTipBuilder()
            .addLine(DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(logLine.timeInMillis())))
            .addLine(logLine.stationType())
            .addVerticalSpace();
      for (int i = 0; i < logLine.fields().size(); i++) {
         String name = logLine.fields().get(i).nameAndUnit();
         String value = logLine.fieldValues().get(i);
         toolTip.addRow(name, value);
      }
      return toolTip.build();
   }

   public Set<String> getSelectedStationTypes() {
      Set<String> stationTypes = selectedStationTypes;
      if (stationTypes == null) {
         stationTypes = stationParameters.stream()
               .filter(BooleanParameter::getBooleanValue)
               .map(BaseParameter::getPersistentName)
               .collect(Collectors.toUnmodifiableSet());
         selectedStationTypes = stationTypes;
      }
      return stationTypes;
   }

   public List<LogLine> getLogLines(BaseLsssModule owner) {
      return logLinesMap.getOrDefault(owner, List.of());
   }

   public Stream<LogLine> getAllLogLines() {
      return logLinesMap.values().stream()
            .flatMap(Collection::stream);
   }

   public Stream<LogLine> getAllDisplayableLogLines() {
      Set<String> stationTypes = getSelectedStationTypes();
      return getAllLogLines()
            .filter(logLine -> stationTypes.contains(logLine.stationType()));
   }

   public Stream<LogLine> getAllDisplayableLogLines(PingRange pingRange) {
      return getAllDisplayableLogLines()
            .filter(logLine -> pingRange.containsTimeInMillis(logLine.timeInMillis()));
   }

   private BooleanParameter newStationTypeParameter(String stationType) {
      BooleanParameter parameter = new BooleanParameter(new Name(stationType, ""), true, stationType);
      parameter.subscribe(stationParameterListener);
      return parameter;
   }

   private void onStationParameterChange() {
      selectedStationTypes = null;
      changeManager.notifyListeners();
   }
}
