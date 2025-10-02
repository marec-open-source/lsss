package no.imr.lsss.framework.config.application;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.resources.LsssIcons;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.svg.SvgIcon;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class LsssServerConf extends ConfigurationUnit {
   public final IntParameter serverPort = new IntParameter(
         new Name("ServerPort", "Server port"),
         getLSSS().getLsssConfig().serverPort, Unit.NONE, ValueConstraints.gte(0),
         "Port for the LSSS scripting server");

   public final BooleanParameter serverActive = new BooleanParameter(
         new Name("ServerActive", "Server active"),
         false,
         "Starts the LSSS scripting server");

   public final ButtonParameter openWebPage = new ButtonParameter(
         new Name("ServerOpenWebPage", "Open web page"),
         "The LSSS scripting server documentation",
         () -> GuiUtils.desktopBrowse(URI.create(getServerBaseUri() + "/lsss/doc/"), getLSSS().getReferenceComponent()));

   private boolean lsssServerPluginEnabled;
   private final LsssServerSettings lsssServerSettings = new LsssServerSettings();

   LsssServerConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("LsssServerConf", "LSSS server"),
            "Settings for the LSSS scripting server");

      serverActive.addListenerAndNotify(openWebPage::setEnabled);
   }

   @Override
   public SvgIcon getIcon() {
      return LsssIcons.LSSS_SERVER;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      List<BaseParameter<?>> parameters = new ArrayList<>();
      parameters.add(new HeaderParameter("LSSS scripting server"));
      parameters.add(serverPort);
      parameters.add(serverActive); // After port so that server is not started on wrong port.
      parameters.add(openWebPage);
      parameters.add(new HeaderParameter("JSON settings"));
      parameters.addAll(lsssServerSettings.getParameters());
      return parameters;
   }

   public String getServerBaseUri() {
      return "http://127.0.0.1:" + serverPort.getIntValue();
   }

   public boolean getLsssServerPluginEnabled() {
      return lsssServerPluginEnabled;
   }

   public void setLsssServerPluginEnabled(boolean lsssServerPluginEnabled) {
      this.lsssServerPluginEnabled = lsssServerPluginEnabled;
   }

   public LsssServerSettings getLsssServerSettings() {
      return lsssServerSettings;
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      if (parameter == openWebPage) {
         return UserProfile.NORMAL_USE;
      }
      return UserProfile.ADMINISTRATOR_MODE;
   }

   @Override
   public JComponent getComponent() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(createParameterEditor().getEditorComponent());
      if (!lsssServerPluginEnabled) {
         panel.add(new JLabel("<html><h2 style='color: #ff6347;'>The LSSS server plugin is not enabled.<h2>"), BorderLayout.NORTH);
      }
      return GuiUtils.createScrollPane(panel);
   }
}
