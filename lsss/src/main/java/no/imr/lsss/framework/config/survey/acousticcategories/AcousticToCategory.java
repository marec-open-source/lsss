package no.imr.lsss.framework.config.survey.acousticcategories;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.korona.viewer.variables.plankton.PlanktonVariable;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.modules.echogram.CategoryConditionalPingMask;
import no.imr.tools.logging.Log;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AcousticToCategory {
   static final String XML_SPECIES_MAPPINGS = "speciesMappings";
   private static final String XML_CATEGORY = "category";
   private static final String XML_PLANKTON = "plankton";

   private final LSSS lsss;
   private final CategoryVariable categoryVariable;
   private final PlanktonVariable planktonVariable;

   private @Nullable Dimension dialogSize;
   private @Nullable List<AcousticCategory> acousticCategories;
   private Map<AcousticCategory, KoronaMapping> koronaMappings = Map.of();

   AcousticToCategory(LSSS lsss) {
      this.lsss = lsss;
      ColorConverterContainer colorConverterContainer = lsss.getInterpretationSettings().getColorConverterContainer();
      categoryVariable = colorConverterContainer.getDiscreteVariable(CategoryVariable.class);
      planktonVariable = colorConverterContainer.getDiscreteVariable(PlanktonVariable.class);
   }

   @Nullable Dimension getDialogSize() {
      return dialogSize;
   }

   void setDialogSize(Dimension dialogSize) {
      this.dialogSize = dialogSize;
   }

   Map<AcousticCategory, KoronaMapping> getKoronaMappings() {
      return koronaMappings;
   }

   public @Nullable KoronaMapping getKoronaMapping(AcousticCategory acousticCategory) {
      return koronaMappings.get(acousticCategory);
   }

   List<AcousticCategory> getAcousticCategories() {
      List<AcousticCategory> list = acousticCategories;
      return list != null ? list : List.of();
   }

   void setAcousticCategories(List<AcousticCategory> acousticCategories) {
      this.acousticCategories = acousticCategories;

      if (koronaMappings.keySet().containsAll(acousticCategories)
            && koronaMappings.size() == acousticCategories.size()) {
         return;
      }

      koronaMappings = acousticCategories.stream()
            .map(acousticCategory -> {
               KoronaMapping koronaMapping = koronaMappings.get(acousticCategory);
               return koronaMapping != null ? koronaMapping : newKoronaMapping(acousticCategory);
            })
            .collect(Collectors.toUnmodifiableMap(KoronaMapping::acousticCategory, Function.identity()));

      lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryChangeManager().notifyListeners();
   }

   public ConditionalPingMask getExcludeMask(AcousticCategory acousticCategory) {
      KoronaMapping koronaMapping = koronaMappings.get(acousticCategory);
      if (koronaMapping == null) {
         return ConditionalPingMask.EMPTY;
      }
      Multimap<DiscreteVariable, String> maskedVariables = HashMultimap.create();
      maskedVariables.putAll(categoryVariable, koronaMapping.categoryNames());
      maskedVariables.putAll(planktonVariable, koronaMapping.planktonNames());
      return CategoryConditionalPingMask.make(lsss, maskedVariables, true);
   }

   public Map<AcousticCategory, Set<String>> getAcousticCategoryToKoronaCategories() {
      return koronaMappings.values().stream()
            .collect(Collectors.toMap(
                  KoronaMapping::acousticCategory,
                  KoronaMapping::categoryNames));
   }

   Element localToXml() {
      Element element = DocumentHelper.createElement(XML_SPECIES_MAPPINGS);
      if (acousticCategories == null) {
         return element;
      }
      for (AcousticCategory acousticCategory : acousticCategories) {
         Element speciesElement = DocumentHelper.createElement(AcousticCategoryConf.XML_SPECIES)
               .addAttribute(AcousticCategoryConf.XML_ID, String.valueOf(acousticCategory.getCompId().getAcousticCategory()))
               .addAttribute(AcousticCategoryConf.XML_NAME, lsss.getConfigurationManager().getLanguageUtils().getAcCatName(acousticCategory));

         // Add category names in a sub element
         KoronaMapping koronaMapping = koronaMappings.get(acousticCategory);
         if (koronaMapping != null) {
            for (String categoryName : koronaMapping.categoryNames()) {
               speciesElement.addElement(XML_CATEGORY)
                     .addAttribute(AcousticCategoryConf.XML_NAME, categoryName);
            }
            // Add plankton categories in a sub element
            for (String planktonName : koronaMapping.planktonNames()) {
               speciesElement.addElement(XML_PLANKTON)
                     .addAttribute(AcousticCategoryConf.XML_NAME, planktonName);
            }
         }
         element.add(speciesElement);
      }
      return element;
   }

   void localFromXml(Element element) {
      Map<Integer, AcousticCategory> acousticCategoryMap = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
      koronaMappings = element.elements(AcousticCategoryConf.XML_SPECIES).stream()
            .map(speciesElement -> {
               int id = Integer.parseInt(speciesElement.attributeValue(AcousticCategoryConf.XML_ID));
               AcousticCategory acousticCategory = acousticCategoryMap.get(id);
               if (acousticCategory == null) {
                  Log.global.warning("No species with id " + id);
                  return null;
               }
               KoronaMapping koronaMapping = newKoronaMapping(acousticCategory);
               for (Element categoryElement : speciesElement.elements(XML_CATEGORY)) {
                  koronaMapping.categoryNames().add(categoryElement.attributeValue(AcousticCategoryConf.XML_NAME));
               }
               for (Element planktonElement : speciesElement.elements(XML_PLANKTON)) {
                  koronaMapping.planktonNames().add(planktonElement.attributeValue(AcousticCategoryConf.XML_NAME));
               }
               return koronaMapping;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toUnmodifiableMap(KoronaMapping::acousticCategory, Function.identity()));
   }

   private static KoronaMapping newKoronaMapping(AcousticCategory acousticCategory) {
      return new KoronaMapping(
            acousticCategory,
            new HashSet<>(),
            new HashSet<>()
      );
   }

   public record KoronaMapping(
         AcousticCategory acousticCategory,
         Set<String> categoryNames,
         Set<String> planktonNames
   ) {
      public boolean isEmpty() {
         return categoryNames().isEmpty() && planktonNames().isEmpty();
      }
   }
}
