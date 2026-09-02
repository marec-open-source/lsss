package no.imr.lsss.util;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.util.LanguageUtils;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.lsss.modules.schoolparameter.SchoolParameterModule;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.time.TimeUtils;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public final class SchoolVisualizerDialog implements ItemContainer<School> {
   private final LSSS lsss;
   private final DecimalFormat defaultFormat = Utils.createDecimalFormat("0.00");
   private final DecimalFormat intFormat = Utils.createDecimalFormat("0");

   public SchoolVisualizerDialog(LSSS lsss, @Nullable Component referenceComponent) {
      this.lsss = lsss;

      if (!lsss.getConfigurationManager().getSurveyMiscConf().computeSchoolParameters.getBooleanValue()) {
         JOptionPane.showMessageDialog(referenceComponent, "School parameters are not computed.\nGo to the configuration dialog to activate.");
         lsss.getConfigurationManager().getSurveyMiscConf().showInConfigurationDialog();
         return;
      }

      List<ItemFeature<School>> features = new ArrayList<>();

      SchoolParameterModule schoolParameterModule = lsss.getModuleManager().getModule(SchoolParameterModule.class);
      Set<SchoolParameter> perChannelParameters = schoolParameterModule.getPerChannelParameterCollections().stream()
            .flatMap(collection -> collection.getParameters().stream())
            .collect(Collectors.toUnmodifiableSet());
      schoolParameterModule.getSchoolParameters().values().stream()
            .sorted(Utils.comparingIgnoringCase(SchoolParameter::getPersistentName))
            .map(parameter -> createSchoolParameterFeature(parameter, perChannelParameters))
            .forEach(features::add);

      features.add(ItemFeature.Time.fromInstant("Time", Unit.UTC, school -> school.getPingRange().begin().getInstant(), TimeUtils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss")));

      lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getSelectedCategories().stream()
            .map(this::createAssignmentFeature)
            .forEach(features::add);

      features.add(createAssignmentTotalFeature());

      ItemVisualizer<School> itemVisualizer = new ItemVisualizer<>(features, this, lsss.getPreferences("SchoolVisualizerDialog"));
      WhenShowingListening.connect(itemVisualizer.getComponent(), List.of(
                  schoolParameterModule.getChangeManager(),
                  lsss.getInterpretationSettings().getChannelChangeManager(),
                  lsss.getRegionManager().getRegionDefinitionChangeManager(),
                  lsss.getRegionManager().selectedRegions(),
                  lsss.getRegionManager().getInterpretationChangeManager()
            ),
            GuiListeners.coalescingLater(itemVisualizer::update));
      itemVisualizer.show(referenceComponent, "Schools");
   }

   @Override
   public Collection<School> getAllItems() {
      return lsss.getRegionManager().getSchoolManager().getSchools();
   }

   @Override
   public Set<School> getSelectedItems() {
      return lsss.getRegionManager().getSchoolManager().getSelectedRegions();
   }

   @Override
   public void setSelectedItems(Set<School> items) {
      lsss.getRegionManager().replaceSelectedRegions(items);
   }

   private ItemFeature<School> createSchoolParameterFeature(SchoolParameter parameter, Set<SchoolParameter> perChannelParameters) {
      ToDoubleFunction<School> toValue;
      if (perChannelParameters.contains(parameter)) {
         toValue = school -> {
            ImmutableMap<String, Float> perChannelValues = school.getParameters().perChannelValues().get(lsss.getInterpretationSettings().getChannel());
            if (perChannelValues != null) {
               Float value = perChannelValues.get(parameter.getPersistentName());
               if (value != null) {
                  return value;
               }
            }
            return Double.NaN;
         };
      } else {
         toValue = school -> {
            Float value = school.getParameters().values().get(parameter.getPersistentName());
            if (value != null) {
               return value;
            }
            return Double.NaN;
         };
      }
      DecimalFormat format = parameter.unit().equals(Unit.COUNT) ? intFormat : defaultFormat;
      return new ItemFeature.Number<>(parameter.name().displayName(), parameter.unit(), toValue, format);
   }

   private ItemFeature<School> createAssignmentFeature(AcousticCategory category) {
      LanguageUtils languageUtils = lsss.getConfigurationManager().getLanguageUtils();
      return new ItemFeature.Number<>("Assignment " + languageUtils.getAcCatInitials(category), Unit.NONE, school -> {
         ChannelInterpretation channelInterpretation = school.getChannelInterpretation(lsss.getInterpretationSettings().getChannel());
         return channelInterpretation.getAssignment(category.getCompId().getAcousticCategory());
      });
   }

   private ItemFeature<School> createAssignmentTotalFeature() {
      return new ItemFeature.Number<>("Assignment total", Unit.NONE, school -> {
         ChannelInterpretation channelInterpretation = school.getChannelInterpretation(lsss.getInterpretationSettings().getChannel());
         return channelInterpretation.getTotalAssignment();
      });
   }
}
