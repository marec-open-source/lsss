package no.imr.lsss.framework.config.survey;

import no.imr.korona.config.KoronaConfigFileService;
import no.imr.korona.resources.KoronaHelp;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.MainConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.acousticcategories.AcousticCategoryConf;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.config.survey.misc.SurveyMiscConf;
import no.imr.lsss.framework.config.survey.modules.ModuleConf;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.lsss.framework.config.survey.survey.SurveyConf;
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
import javax.swing.JPanel;
import java.awt.FlowLayout;

/**
 * The LSSS configuration that is specific of a survey.
 */
public final class SurveyConfiguration extends MainConfigurationUnit {
   private static final String XML_NEWEST_VERSION = "4";
   static final UpgradeEngine<Element> XML_UPGRADE_ENGINE = new UpgradeEngine<>(
         "Survey configuration", XML_NEWEST_VERSION, XmlUtils::getVersion,
         new XslUpgraderFactory("no/imr/lsss/resources/surveyConfigurationUpgrade"));

   private final SurveyConf surveyConf;
   private final AcousticCategoryConf acousticCategoryConf;
   private final GridConf gridConf;
   private final DataConfLSSS dataConf;
   private final PreprocessingConf preprocessingConf;
   private final SurveyMiscConf surveyMiscConf;
   private final ModuleConf moduleConf;

   public SurveyConfiguration(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("SurveyConfiguration", "Survey configuration"),
            "Settings for current survey", "survey.xml");

      surveyConf = addSubConfigurationUnit(new SurveyConf(plugin));
      acousticCategoryConf = addSubConfigurationUnit(new AcousticCategoryConf(plugin));
      gridConf = addSubConfigurationUnit(new GridConf(plugin));
      surveyMiscConf = addSubConfigurationUnit(new SurveyMiscConf(plugin));
      dataConf = addSubConfigurationUnit(new DataConfLSSS(plugin));
      preprocessingConf = addSubConfigurationUnit(new PreprocessingConf(plugin,
            new Name("PreprocessingConf", "Preprocessing"),
            dataConf, KoronaConfigFileService.CONTEXT, KoronaHelp.HELP_SET.getTopHelpID()));
      moduleConf = addSubConfigurationUnit(new ModuleConf(plugin));
   }

   public SurveyConf getSurveyConf() {
      return surveyConf;
   }

   public AcousticCategoryConf getAcousticCategoryConf() {
      return acousticCategoryConf;
   }

   public GridConf getGridConf() {
      return gridConf;
   }

   public DataConfLSSS getDataConf() {
      return dataConf;
   }

   public PreprocessingConf getPreprocessingConf() {
      return preprocessingConf;
   }

   public SurveyMiscConf getSurveyMiscConf() {
      return surveyMiscConf;
   }

   public ModuleConf getModuleConf() {
      return moduleConf;
   }

   @Override
   public JComponent getComponent() {
      JPanel labelPanel = new JPanel(new FlowLayout());
      labelPanel.add(new JLabel("""
            <html>
            <body style="text-align: center;">
            <h1>Survey Configuration</h1>
            <p>Configuration specific for this survey</p>
            """));

      JButton button = MiscIcons.SAVE.on(new JButton("Save current settings as default"));
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
         getLSSS().showError("Error upgrading survey configuration", e);
      }
      super.fromXml(element);
   }

   private static Element upgrade(Element element) throws UpgradeException {
      return XML_UPGRADE_ENGINE.upgrade(element);
   }

   @Override
   protected Element toDefaultXml() {
      Element backup = toXml();

      applyRecursively(ConfigurationUnit::prepareForSaveDefault);

      Element defaultXml = toXml();
      fromXml(backup);
      return defaultXml;
   }
}
