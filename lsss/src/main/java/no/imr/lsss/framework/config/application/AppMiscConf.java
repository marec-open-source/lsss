package no.imr.lsss.framework.config.application;

import com.google.common.base.Splitter;
import no.imr.korona.data.datamanager.PingLoadingStrategy;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.application.packages.PackagesConf;
import no.imr.lsss.framework.config.application.preview.PreviewFeaturesConf;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.web.Wms;
import no.imr.tools.xml.XmlParse;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Document;
import org.dom4j.Element;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Various application specific configuration.
 */
public final class AppMiscConf extends ConfigurationUnit {
   public final ObjectParameter<OnApplicationStartAction> onApplicationStart = new ObjectParameter<>(
         new Name("OnApplicationStart", "On application start"),
         OnApplicationStartAction.DO_NOTHING, OnApplicationStartAction.values(),
         "What to do when starting LSSS");

   public final ObjectParameter<OnSurveyOpenAction> onSurveyOpen = new ObjectParameter<>(
         new Name("OnSurveyOpen", "On survey open"),
         OnSurveyOpenAction.SHOW_CONFIG_DIALOG, OnSurveyOpenAction.values(),
         "What to do when opening a survey");

   private final SeparatorParameter separatorMap = SeparatorParameter.line();

   public final StringParameter mapURL = new StringParameter(
         new Name("MapURL", "Map URL"),
         Wms.DEFAULT_WMS_URL,
         "URL to WMS map server");

   public final StringParameter mapLayers = new StringParameter(
         new Name("MapLayers", "Map layers"),
         Wms.DEFAULT_WMS_LAYERS,
         "Comma-separated list of layers from WMS map server");

   public final ButtonParameter mapSelectLayers = new ButtonParameter(
         new Name("MapSelectLayers", "Select layers..."),
         "Downloads WMS capabilities and select layers",
         this::selectLayersFromCapabilities);

   public final ButtonParameter mapReset = new ButtonParameter(
         new Name("MapResetLayers", "Reset"),
         "Reset to default WMS map layers",
         () -> {
            mapURL.setValue(Wms.DEFAULT_WMS_URL);
            mapLayers.setValue(Wms.DEFAULT_WMS_LAYERS);
         });

   private final SeparatorParameter separatorPing = SeparatorParameter.line();

   public final IntParameter maxPings = new IntParameter(
         new Name("MaxPings", "Max pings"),
         10000, Unit.COUNT, ValueConstraints.gte(0),
         "Maximum pings in DETAIL mode");

   public final ObjectParameter<PingLoadingStrategy> pingLoading = new ObjectParameter<>(
         new Name("PingLoading", "Ping loading"),
         PingLoadingStrategy.LONGEST_GAP_LEFT_TO_RIGHT, PingLoadingStrategy.values(),
         "Strategy for reading pings from file");

   public final BooleanParameter preload = new BooleanParameter(
         new Name("Preload"),
         true,
         "Preload pings in next segment in background");

   public final BooleanParameter preloadPreprocessed = new BooleanParameter(
         new Name("PreloadPreprocessed", "Preload preprocessed"),
         false,
         "Preload preprocessed data in background");

   public final BooleanParameter useEnglish = new BooleanParameter(
         new Name("UseEnglish", "Use English"),
         true,
         "Use English names for acoustic categories");

   public final BooleanParameter verticalScrollBar = new BooleanParameter(
         new Name("VerticalScrollBar", "Vertical scrollbar"),
         true,
         "Show vertical scrollbar for echogram");

   private final SeparatorParameter separatorUpdate = SeparatorParameter.line();

   private final LsssServerConf lsssServerConf;
   private final PackagesConf packagesConf;
   private final PreviewFeaturesConf previewFeaturesConf;

   AppMiscConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("AppMiscConf", "Miscellaneous"),
            "Miscellaneous settings common for all surveys");

      lsssServerConf = addSubConfigurationUnit(new LsssServerConf(plugin));
      packagesConf = addSubConfigurationUnit(new PackagesConf(plugin, lsssServerConf));
      previewFeaturesConf = addSubConfigurationUnit(new PreviewFeaturesConf(plugin));

      mapURL.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
      mapLayers.setProperty(BaseParameter.KEY_LEFT_ALIGNED, true);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            onApplicationStart,
            onSurveyOpen,
            //---
            separatorMap,
            mapURL,
            mapLayers,
            mapSelectLayers,
            mapReset,
            //---
            separatorPing,
            maxPings,
            pingLoading,
            preload,
            preloadPreprocessed,
            useEnglish,
            verticalScrollBar,
            //---
            separatorUpdate
      );
   }

   public LsssServerConf getLsssServerConf() {
      return lsssServerConf;
   }

   public PackagesConf getPackagesConf() {
      return packagesConf;
   }

   public PreviewFeaturesConf getPreviewFeaturesConf() {
      return previewFeaturesConf;
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return UserProfile.ADMINISTRATOR_MODE;
   }

   private void selectLayersFromCapabilities() {
      Document capabilities = new WorkerDialog(getLSSS().getReferenceComponent(), "Downloading WMS capabilities")
            .setWaitUntilFinishedIfCancelled(false)
            .setOnError(e -> getLSSS().showError("Error downloading WMS capabilities", e))
            .startMakeValue(asyncHandle -> Wms.downloadCapabilities(mapURL.getValue()));
      if (capabilities == null) {
         return;
      }
      Set<String> selectedLayers = Set.copyOf(Splitter.on(",").trimResults().splitToList(mapLayers.getValue()));
      List<BooleanParameter> parameters = Utils.getAllOfType(capabilities.selectNodes("//Layer"), Element.class)
            .map(layer -> {
               String name = XmlParse.stringElement(layer, "Name", "");
               if (name.isEmpty()) {
                  return null;
               }
               String description = XmlParse.stringElement(layer, "Abstract", "");
               return new BooleanParameter(new Name(name), selectedLayers.contains(name), description);
            })
            .filter(Objects::nonNull)
            .toList();
      Listener layersListener = () -> {
         String value = parameters.stream()
               .filter(BooleanParameter::getBooleanValue)
               .map(BaseParameter::getPersistentName)
               .collect(Collectors.joining(","));
         mapLayers.setValue(value);
      };
      layersListener.addTo(parameters);
      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      boolean ok = new ConfigurableGUIDialog(getConfigurationManager().getDialog(), "Select map layers", new ParameterCollection(parameters))
            .setCloseOnOk(parameterEditor::commitEdits)
            .setGUI(parameterEditor.getEditorComponent())
            .setMaximumSize(getConfigurationManager().getDialog().getWidth(), Integer.MAX_VALUE)
            .show();
      if (ok) {
         layersListener.listen();
      }
   }
}
