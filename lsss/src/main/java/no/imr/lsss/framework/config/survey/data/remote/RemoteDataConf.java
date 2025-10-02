package no.imr.lsss.framework.config.survey.data.remote;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.config.survey.data.SurveyDirectoryConf;
import no.imr.tools.Utils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.swing.ViewHolder;
import org.dom4j.Element;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class RemoteDataConf extends ConfigurationUnit {
   private final ViewHolder<RemoteDataConfView> viewHolder = new ViewHolder<>(() -> new RemoteDataConfView(this));
   private final List<BaseParameter<?>> remoteDirectoryParameters = new ArrayList<>();

   public RemoteDataConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("RemoteDataConf", "Remote directories"),
            "Remote directories used when copying remote survey data");
   }

   @Override
   public void setup() {
      for (SurveyDirectoryConf conf : getConfigurationManager().getDataConf().getAllSurveyDirectoryConfs()) {
         if (!(conf instanceof DataConfLSSS)) {
            remoteDirectoryParameters.add(SeparatorParameter.line());
            remoteDirectoryParameters.add(new HeaderParameter(conf.getDisplayName(), conf.getIcon()));
         }
         for (SurveyDirectoryParameter parameter : conf.getAllDirectoryParameters()) {
            remoteDirectoryParameters.add(new RemoteDirectoryParameter(parameter));
         }
      }
   }

   @Override
   public void fromXml(Element element) {
      removeUnknownParameters();
      super.fromXml(element);
   }

   private void removeUnknownParameters() {
      remoteDirectoryParameters.removeIf(parameter -> parameter instanceof UnknownParameter);
   }

   @Override
   public BaseParameter<?> possiblyCreateNewParameter(String persistentName) {
      StringParameter parameter = new UnknownParameter(persistentName);
      remoteDirectoryParameters.add(parameter);
      return parameter;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return remoteDirectoryParameters;
   }

   @Override
   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   @Override
   public void prepareForSaveDefault() {
      removeUnknownParameters();
      remoteDirectoryParameters().forEach(parameter -> parameter.setFile(null));
   }

   public Stream<RemoteDirectoryParameter> remoteDirectoryParameters() {
      return Utils.getAllOfType(remoteDirectoryParameters, RemoteDirectoryParameter.class);
   }

   private static final class UnknownParameter extends StringParameter {
      private UnknownParameter(String persistentName) {
         super(new Name(persistentName));

         setVisible(false);
      }
   }
}
