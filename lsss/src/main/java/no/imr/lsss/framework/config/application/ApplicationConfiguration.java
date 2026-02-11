package no.imr.lsss.framework.config.application;

import no.imr.korona.config.KoronaConfigFileService;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.MainConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.upgrade.UpgradeEngine;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.xml.XmlUtils;
import no.imr.tools.xml.XslUpgraderFactory;
import org.dom4j.Element;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.FlowLayout;

/**
 * The LSSS configuration that is independent of surveys.
 */
public final class ApplicationConfiguration extends MainConfigurationUnit {
   private static final String XML_NEWEST_VERSION = "1";
   static final UpgradeEngine<Element> XML_UPGRADE_ENGINE = new UpgradeEngine<>(
         "Application configuration", XML_NEWEST_VERSION, XmlUtils::getVersion,
         new XslUpgraderFactory("no/imr/lsss/resources/applicationConfigurationUpgrade"));

   public static final String CONFIG_FILE_NAME = "application.xml";

   private final PluginConf pluginConf;
   private final DatabaseConf databaseConf;
   private final DirectoryConf directoryConf;
   private final AppPreprocessingConf appPreprocessingConf;
   private final AppMiscConf appMiscConf;

   public ApplicationConfiguration(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("ApplicationConfiguration", "Application configuration"),
            "Settings common for all surveys", CONFIG_FILE_NAME);

      pluginConf = addSubConfigurationUnit(new PluginConf(plugin));
      databaseConf = addSubConfigurationUnit(new DatabaseConf(plugin));
      directoryConf = addSubConfigurationUnit(new DirectoryConf(plugin));
      appPreprocessingConf = addSubConfigurationUnit(new AppPreprocessingConf(plugin, new Name("AppPreprocessingConf", "Preprocessing"), KoronaConfigFileService.CONTEXT));
      appMiscConf = addSubConfigurationUnit(new AppMiscConf(plugin));
   }

   public PluginConf getPluginConf() {
      return pluginConf;
   }

   public DatabaseConf getDatabaseConf() {
      return databaseConf;
   }

   public DirectoryConf getDirectoryConf() {
      return directoryConf;
   }

   public AppPreprocessingConf getAppPreprocessingConf() {
      return appPreprocessingConf;
   }

   public AppMiscConf getAppMiscConf() {
      return appMiscConf;
   }

   @Override
   public JComponent getComponent() {
      JPanel labelPanel = new JPanel(new FlowLayout());
      labelPanel.add(new JLabel("""
            <html>
            <body style="text-align: center;">
            <h1>Application Configuration</h1>
            <p>Configuration common for all surveys</p>
            """));

      JButton button = MiscIcons.SAVE.on(new JButton("Save current settings"));
      button.setEnabled(getConfigurationManager().canEdit(UserProfile.ADMINISTRATOR_MODE) && getLSSS().getLsssConfig().isPrimaryLSSS);
      button.addActionListener(_ -> saveDefault());
      JPanel buttonPanel = new JPanel(new FlowLayout());
      buttonPanel.add(button);

      Box box = Box.createVerticalBox();
      box.setBorder(BorderFactory.createEtchedBorder());
      box.add(GuiUtils.createVerticalFiller());
      box.add(labelPanel);
      box.add(Box.createVerticalStrut(20));
      box.add(buttonPanel);
      box.add(GuiUtils.createVerticalFiller());

      return box;
   }

   @Override
   public Element toXml() {
      return super.toXml()
            .addAttribute(XmlUtils.VERSION, XML_NEWEST_VERSION);
   }

   @Override
   public void fromXml(Element element) {
      try {
         element = upgrade(element);
      } catch (Exception e) {
         getLSSS().showError("Error upgrading application configuration", e);
      }
      super.fromXml(element);
   }

   private static Element upgrade(Element element) throws UpgradeException {
      return XML_UPGRADE_ENGINE.upgrade(element);
   }

   public boolean isUnmodifiedOrUserApproved(Component referenceComponent) {
      if (!getLSSS().getLsssConfig().isPrimaryLSSS) {
         return true;
      }

      if (isDefaultModified()) {
         int answer = JOptionPane.showConfirmDialog(referenceComponent, "Application configuration is modified.\nSave changes?", "Save configuration?",
               JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

         switch (answer) {
            case JOptionPane.YES_OPTION -> {
               saveDefault();
               return true;
            }
            case JOptionPane.NO_OPTION -> {
               return true;
            }
            default -> {
               return false;
            }
         }
      } else {
         return true;
      }
   }
}
