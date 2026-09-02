package no.imr.lsss.framework.config.survey.survey;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.LsssQuery;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.PlatformCodes;
import no.imr.lsss.database.tables.hibernate.PlatformName;
import no.imr.lsss.database.tables.hibernate.PlatformPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.DateParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.TimeParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * For selecting, editing and creating surveys.
 */
public final class SurveyConf extends ConfigurationUnit {
   private List<Platform> mPlatforms = List.of();
   private List<Survey> mSurveys = List.of();
   public final ListenableProperty<Optional<Platform>> mPlatform = new ListenableProperty<>(Optional.empty());

   public final BooleanParameter useLocalDatabase = new BooleanParameter(new Name("UseLocalDatabase"),
         false);

   public final ObjectParameter<Optional<Nation>> mNation = new ObjectParameter<>(
         new Name("Nation"),
         Optional.empty()) {

      @Override
      public String toString(Optional<Nation> optionalNation) {
         return optionalNation
               .map(Nation::getNationName)
               .orElse(getAllowedValues().size() == 1 ? "No nations available" : "Select nation...");
      }
   };

   public final OptionalIntParameter mPlatformId = new OptionalIntParameter(
         new Name("PlatformId"),
         Optional.empty(), Unit.NONE);

   public final ObjectParameter<Optional<PlatformAndName>> mPlatformAndName = new ObjectParameter<>(
         new Name("Platform"),
         Optional.empty()) {

      @Override
      public String toString(Optional<PlatformAndName> optionalPlatformAndName) {
         return optionalPlatformAndName
               .map(PlatformAndName::toString)
               .orElse(getAllowedValues().size() == 1 ? "No platforms available" : "Select platform...");
      }

      @Override
      public void fromXml(Element element) {
         // Platform set from mPlatformId
      }
   };

   private final SeparatorParameter mSeparator = SeparatorParameter.line();

   public final OptionalIntParameter mSurveyId = new OptionalIntParameter(
         new Name("SurveyId", "Survey number"),
         Optional.empty(), Unit.NONE);

   public final ObjectParameter<Optional<Survey>> mSurvey = new ObjectParameter<>(
         new Name("Survey", "Survey title"),
         Optional.empty()) {

      @Override
      public String toString(Optional<Survey> optionalSurvey) {
         return optionalSurvey
               .map(survey -> survey.getSurveyTitle() + " (" + survey.getCompId().getSurvey() + ")")
               .orElse(getAllowedValues().size() == 1 ? "No surveys available" : "Select survey...");
      }

      @Override
      public void fromXml(Element element) {
         // Survey set from mSurveyId
      }
   };

   public final DateParameter startDate = new DateParameter(
         new Name("StartDate", "Start date (UTC) (optional)"));
   public final TimeParameter startTime = new TimeParameter(
         new Name("StartTime", "Start time (UTC) (optional)"));
   public final DateParameter stopDate = new DateParameter(
         new Name("StopDate", "Stop date (UTC) (optional)"));
   public final TimeParameter stopTime = new TimeParameter(
         new Name("StopTime", "Stop time (UTC) (optional)"));

   public final OptionalFloatParameter boundaryNorth = new OptionalFloatParameter(
         new Name("BoundaryNorth", "Northern survey boundary (N+, S-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   public final OptionalFloatParameter boundarySouth = new OptionalFloatParameter(
         new Name("BoundarySouth", "Southern survey boundary (N+, S-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   public final OptionalFloatParameter boundaryWest = new OptionalFloatParameter(
         new Name("BoundaryWest", "Western survey boundary (E+, W-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   public final OptionalFloatParameter boundaryEast = new OptionalFloatParameter(
         new Name("BoundaryEast", "Eastern survey boundary (E+, W-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   public final TextParameter surveyDescription = new TextParameter(
         new Name("Comment", "Short survey description (optional)"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_COMMENT_LENGTH));

   public SurveyConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("SurveyConf", "Survey"),
            "Selection of nation, platform and survey");

      mPlatform.subscribe(optionalPlatform -> {
         mPlatformId.setValue(optionalPlatform.map(platform -> (int) platform.getCompId().getPlatform()));
         updateAllowedPlatformAndNames();
         updateAllowedSurveys();
      });

      useLocalDatabase.setVisible(false);

      mNation.addListenerAndNotify(optionalNation -> {
         mNation.setPersistable(optionalNation.isPresent());
         mNation.setEnabled(mNation.getAllowedValues().size() > 1);
      });
      mNation.subscribe(_ -> updateAllowedPlatforms());

      mPlatformId.setVisible(false);
      mPlatformId.subscribe(optionalId -> {
         Optional<Platform> platform = optionalId.flatMap(id -> {
            return mPlatforms.stream()
                  .filter(p -> p.getCompId().getPlatform() == id)
                  .findFirst();
         });
         mPlatform.setValue(platform);
         if (platform.isEmpty()) {
            mPlatformId.setEmpty();
         }
      });

      mPlatformAndName.addListenerAndNotify(optionalPlatformAndName -> {
         mPlatformId.setPersistable(optionalPlatformAndName.isPresent());
         mPlatformAndName.setPersistable(optionalPlatformAndName.isPresent());
         mPlatformAndName.setEnabled(mPlatformAndName.getAllowedValues().size() > 1);
      });
      mPlatformAndName.subscribe(optionalPlatformAndName -> {
         Optional<Integer> platformId = optionalPlatformAndName.map(PlatformAndName::platformId);
         mPlatformId.setValue(platformId);
      });

      mSurveyId.setEnabled(false);
      mSurveyId.subscribe(optionalSurveyId -> {
         Optional<Survey> survey = optionalSurveyId.flatMap(surveyId -> {
            return mSurveys.stream()
                  .filter(s -> s.getCompId().getSurvey() == surveyId)
                  .findFirst();
         });
         mSurvey.setValue(survey);
         if (survey.isEmpty()) {
            mSurveyId.setEmpty();
         }
      });

      mSurvey.addListenerAndNotify(optionalSurvey -> {
         useLocalDatabase.setPersistable(optionalSurvey.isPresent());
         mSurveyId.setPersistable(optionalSurvey.isPresent());
         mSurvey.setPersistable(optionalSurvey.isPresent());
         mSurvey.setEnabled(mSurvey.getAllowedValues().size() > 1);
      });
      mSurvey.subscribe(_ -> updateParametersFromSurvey());

      startDate.subscribe(_ -> updateAllowedPlatformAndNames());

      startDate.setEnabled(false);
      startTime.setEnabled(false);
      stopDate.setEnabled(false);
      stopTime.setEnabled(false);
      boundaryNorth.setEnabled(false);
      boundarySouth.setEnabled(false);
      boundaryWest.setEnabled(false);
      boundaryEast.setEnabled(false);
      surveyDescription.setEnabled(false);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            useLocalDatabase,
            mNation,
            mPlatformId,
            mPlatformAndName,
            mSeparator,
            mSurveyId,
            mSurvey,
            startDate,
            startTime,
            stopDate,
            stopTime,
            boundaryNorth,
            boundarySouth,
            boundaryWest,
            boundaryEast,
            surveyDescription
      );
   }

   @Override
   public void setup() {
      super.setup();

      getLSSS().getDatabaseManager().getConnectionChangeManager().addListener(() -> {
         Nation n = getNation();
         Platform p = getPlatform();
         Survey s = getSurvey();
         updateAllowedNations();
         updateAllowedPlatforms();
         updateAllowedSurveys();
         setIfValid(n, p, s);
      });
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      // Since nation, platform, survey are not persisted when absent:
      Element parametersElement = configurationElement.element(ParameterCollection.XML_PARAMETERS);
      Set<String> parameterNames;
      if (parametersElement != null) {
         parameterNames = parametersElement.elements().stream()
               .map(e -> e.attributeValue(Configurable.XML_NAME))
               .filter(Objects::nonNull)
               .collect(Collectors.toSet());
      } else {
         parameterNames = Set.of();
      }
      if (!parameterNames.contains(mNation.getPersistentName())) {
         mNation.setValue(Optional.empty());
      } else if (!parameterNames.contains(mPlatformAndName.getPersistentName())) {
         mPlatformAndName.setValue(Optional.empty());
      } else if (!parameterNames.contains(mSurvey.getPersistentName())) {
         mSurvey.setValue(Optional.empty());
      }

      super.fromConfigurationXml(configurationElement);
   }

   @Override
   public void prepareForSaveDefault() {
      mSurvey.setValue(Optional.empty());
   }

   private List<Platform> getPlatformsFromDatabase() {
      Nation nation = getNation();
      if (nation != null) {
         DatabaseConnection databaseConnection = getLSSS().getDatabaseManager().getDatabaseConnection();
         List<Platform> platforms = databaseConnection.executeFetchQuery(LsssQuery.fetch(Platform.class, DatabaseData.NATION, nation.getNation()));
         if (platforms.stream().noneMatch(platform -> platform.getCompId().getPlatform() == 0)) {
            platforms = new ArrayList<>(platforms);
            platforms.addAll(DatabaseData.copyFromDefaultNation(databaseConnection, Platform.class, nation.getNation()));
            DatabaseData.copyFromDefaultNation(databaseConnection, PlatformName.class, nation.getNation());
            DatabaseData.copyFromDefaultNation(databaseConnection, PlatformCodes.class, nation.getNation());
         }
         List<PlatformName> platformNames = databaseConnection.executeFetchQuery(LsssQuery.fetch(PlatformName.class, DatabaseData.NATION, nation.getNation()));
         SetMultimap<Short, PlatformName> nameMap = HashMultimap.create();

         for (PlatformName platformName : platformNames) {
            nameMap.put(platformName.getCompId().getPlatform(), platformName);
         }

         for (Platform platform : platforms) {
            platform.setPlatformNames(nameMap.get(platform.getCompId().getPlatform()));
            platform.setNation(nation);
         }

         return platforms.stream()
               .filter(platform -> platform.getCompId().getPlatform() != 0)
               .toList();
      } else {
         return List.of();
      }
   }

   private void updateAllowedNations() {
      List<Nation> nations = getLSSS().getDatabaseManager().getDatabaseData().getNations().getAll();
      mNation.setAllowedValuesAndPossiblyValue(toOptionalList(nations), Optional.empty());
   }

   public void updateAllowedPlatforms() {
      mPlatforms = getPlatformsFromDatabase();
      mPlatform.getValue().ifPresent(p -> {
         if (!mPlatforms.contains(p)) {
            mPlatform.setValue(Optional.empty());
         }
      });
      updateAllowedPlatformAndNames();
   }

   private void updateAllowedPlatformAndNames() {
      List<PlatformAndName> platformAndNames = new ArrayList<>();
      for (Platform platform : mPlatforms) {
         for (PlatformName platformName : platform.getPlatformNames()) {
            platformAndNames.add(new PlatformAndName(platform, platformName.getPlatformName()));
         }
      }

      PlatformAndName selectedPlatformAndName;
      Platform platform = mPlatform.getValue().orElse(null);
      if (platform == null) {
         selectedPlatformAndName = null;
      } else {
         String name;
         Survey survey = getSurvey();
         if (survey != null) {
            name = platform.findPlatformName(startDate.getIntValue());
         } else {
            name = platform.findLatestPlatformName();
         }
         selectedPlatformAndName = new PlatformAndName(platform, name);
         if (!platformAndNames.contains(selectedPlatformAndName)) {
            platformAndNames.add(selectedPlatformAndName);
         }
      }
      platformAndNames.sort(null);
      mPlatformAndName.setAllowedValuesAndValue(toOptionalList(platformAndNames), Optional.ofNullable(selectedPlatformAndName));
   }

   private List<Survey> getSurveysFromDatabase() {
      Platform platform = getPlatform();
      if (platform != null) {
         List<Survey> surveys = getLSSS().getDatabaseManager().getDatabaseConnection().executeFetchQuery(
               LsssQuery.fetch(Survey.class, platform));
         return surveys.stream()
               .sorted(Utils.comparingIgnoringCase(Survey::getSurveyTitle).thenComparingInt(s -> s.getCompId().getSurvey()))
               .peek(survey -> survey.setPlatform(platform))
               .toList();
      } else {
         return List.of();
      }
   }

   public void updateAllowedSurveys() {
      mSurveys = getSurveysFromDatabase();
      mSurvey.setAllowedValuesAndPossiblyValue(toOptionalList(mSurveys), Optional.empty());
   }

   private static <T> List<Optional<T>> toOptionalList(List<T> values) {
      return Stream.concat(
            Stream.of(Optional.<T>empty()),
            values.stream().map(Optional::of)
      ).toList();
   }

   @Override
   public JComponent getComponent() {
      return new SurveyConfView(this).getComponent();
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      if (parameter == mSurvey) {
         return UserProfile.SURVEY_SETUP;
      }
      return UserProfile.ADMINISTRATOR_MODE;
   }

   private void updateParametersFromSurvey() {
      Survey survey = getSurvey();

      if (survey != null) {
         mSurveyId.setIntValue(survey.getCompId().getSurvey());
         startDate.setIntValue(survey.getStartDate());
         startTime.setIntValue(survey.getStartTime());
         stopDate.setIntValue(survey.getStopDate());
         stopTime.setIntValue(survey.getStopTime());
         surveyDescription.setValue(survey.getSurveyDescription());
         boundaryNorth.setFloatValue(survey.getBoundaryNorth());
         boundarySouth.setFloatValue(survey.getBoundarySouth());
         boundaryWest.setFloatValue(survey.getBoundaryWest());
         boundaryEast.setFloatValue(survey.getBoundaryEast());
      } else {
         mSurveyId.setEmpty();
         startDate.setEmpty();
         startTime.setEmpty();
         stopDate.setEmpty();
         stopTime.setEmpty();
         surveyDescription.setValue("");
         boundaryNorth.setEmpty();
         boundarySouth.setEmpty();
         boundaryWest.setEmpty();
         boundaryEast.setEmpty();
      }
   }

   public @Nullable Nation getNation() {
      return mNation.getValue().orElse(null);
   }

   public @Nullable Platform getPlatform() {
      return mPlatform.getValue().orElse(null);
   }

   List<Platform> getPlatforms() {
      return mPlatforms;
   }

   public @Nullable Survey getSurvey() {
      return mSurvey.getValue().orElse(null);
   }

   List<Survey> getSurveys() {
      return mSurveys;
   }

   public void setIfValid(@Nullable Nation aNation, @Nullable Platform aPlatform, @Nullable Survey aSurvey) {
      mNation.setValue(find(mNation.getAllowedValues(),
            aNation != null ? aNation.getNation() : null, Nation::getNation));

      mPlatformAndName.setValue(find(mPlatformAndName.getAllowedValues(),
            aPlatform != null ? aPlatform.getCompId() : null, PlatformAndName::platformPK));

      mSurvey.setValue(find(mSurvey.getAllowedValues(),
            aSurvey != null ? aSurvey.getCompId() : null, Survey::getCompId));
   }

   private static <A, B> Optional<A> find(List<Optional<A>> list, @Nullable B item, Function<A, B> f) {
      return list.stream()
            .filter(optA -> Objects.equals(item, optA.map(f).orElse(null)))
            .findFirst()
            .orElse(Optional.empty());
   }

   public record PlatformAndName(Platform platform, String name) implements Comparable<PlatformAndName> {
      private PlatformPK platformPK() {
         return platform.getCompId();
      }

      private int platformId() {
         return platformPK().getPlatform();
      }

      @Override
      public String toString() {
         return name + " (" + platformId() + ")";
      }

      @Override
      public int compareTo(PlatformAndName other) {
         int nameComparison = name.compareToIgnoreCase(other.name);
         if (nameComparison != 0) {
            return nameComparison;
         }
         return Integer.compare(other.platformId(), platformId()); // Reverse sorting on id.
      }
   }
}
