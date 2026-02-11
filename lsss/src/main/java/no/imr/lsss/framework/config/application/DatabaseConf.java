package no.imr.lsss.framework.config.application;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import org.dom4j.Element;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * For configuring the database.
 */
public final class DatabaseConf extends ConfigurationUnit {
   public final BooleanParameter useLocalDatabase = new BooleanParameter(
         new Name("UseLocalDatabase", "Use survey local database"),
         false) {
      @Override
      public boolean isEnabled() {
         if (!getConfigurationManager().canEdit(UserProfile.SURVEY_SETUP)) {
            return false;
         }
         if (getBooleanValue() || getConfigurationManager().getSurveyConf().useLocalDatabase.getBooleanValue()) {
            return true;
         }
         return getLSSS().getSurveyManager().isOpen() && getConfigurationManager().getSurveyConf().getSurvey() != null;
      }
   };

   DatabaseConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("DatabaseConf", "Database"),
            "Configuration of database connection");
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            useLocalDatabase
      );
   }

   @Override
   public void setup() {
      super.setup();

      getLSSS().getSurveyManager().getChangeManager().addListener(() -> {
         if (!getLSSS().getSurveyManager().isOpen()) {
            doApply(false, false);
         }
         updateSurveyLocalDescription();
      });
      getConfigurationManager().getSurveyConf().useLocalDatabase.subscribe(_ -> {
         boolean useLocal = getConfigurationManager().getSurveyConf().useLocalDatabase.getBooleanValue();
         useLocal &= getLSSS().getSurveyManager().isOpen();
         doApply(useLocal, false);
      });
      getConfigurationManager().getSurveyConf().mSurvey.subscribe(_ -> updateSurveyLocalDescription());

      updateSurveyLocalDescription();
   }

   private void updateSurveyLocalDescription() {
      String description = "";
      if (!getLSSS().getSurveyManager().isOpen()) {
         description = "No survey opened";
      } else if (getConfigurationManager().getSurveyConf().getSurvey() == null) {
         description = "No survey selected";
      }
      useLocalDatabase.setDescription(description);
      useLocalDatabase.notifyListeners();
   }

   @Override
   public boolean apply() {
      boolean useLocal = useLocalDatabase.getBooleanValue();
      doApply(useLocal, true);
      return true;
   }

   private void doApply(boolean useSurveyLocal, boolean doImport) {
      // Synchronizes all UseLocalDatabase variables.
      getLSSS().getDatabaseManager().setUseSurveyLocal(useSurveyLocal, doImport);
      useLocalDatabase.setBooleanValue(useSurveyLocal);
      getConfigurationManager().getSurveyConf().useLocalDatabase.setBooleanValue(useSurveyLocal);
   }

   @Override
   public void cancelled() {
      useLocalDatabase.setBooleanValue(getConfigurationManager().getSurveyConf().useLocalDatabase.getBooleanValue());
   }

   @Override
   public JComponent getComponent() {
      JPanel globalDatabasePanel = new JPanel(new BorderLayout());
      globalDatabasePanel.setBorder(BorderFactory.createTitledBorder("Global database"));
      globalDatabasePanel.add(new DatabaseConnectionEditor(getLSSS()).getComponent());

      JPanel localDatabasePanel = new JPanel(new BorderLayout());
      localDatabasePanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Survey local database"),
            GuiUtils.DEFAULT_MARGIN));
      ParameterEditor parameterEditor = new ParameterEditor(List.of(useLocalDatabase));
      localDatabasePanel.add(parameterEditor.getEditorComponent());

      JPanel configurationPanel = new JPanel(new BorderLayout());
      configurationPanel.add(globalDatabasePanel);
      configurationPanel.add(localDatabasePanel, BorderLayout.SOUTH);
      return GuiUtils.createScrollPane(globalDatabasePanel, localDatabasePanel);
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      configurationElement.add(getLSSS().getDatabaseManager().toXml());
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      Element element = configurationElement.elements().getFirst();
      getLSSS().getDatabaseManager().fromXml(element);
   }
}
