package no.imr.lsss.modules.frequencyresponse;

import no.imr.korona.computation.categorization.Category;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.Name;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class KoronaCategoriesSelection {
   public final BooleanParameter plotCategories = new BooleanParameter(
         new Name("PlotCategories", "Plot KORONA categories"),
         false,
         "Plot frequency response for KORONA categories");

   public final MultiParameter<BooleanParameter> koronaCategories = new MultiParameter<>(
         new Name("KoronaCategories", "KORONA categories"),
         "Which KORONA categories to plot") {
      @Override
      protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
         return addParameter(newKoronaCategoryParameter(persistentName, false));
      }
   };

   public KoronaCategoriesSelection() {
      plotCategories.subscribe(value -> {
         koronaCategories.getParameters().forEach(parameter -> parameter.setEnabled(value));
      });
   }

   public boolean isSelected(Category category) {
      BooleanParameter parameter = koronaCategories.getParameter(category.getName());
      return parameter != null && parameter.getBooleanValue();
   }

   public void update(List<Category> categories) {
      if (categories.size() != koronaCategories.getParameters().size() ||
            categories.stream().anyMatch(category -> koronaCategories.getParameter(category.getName()) == null)) {
         updateKoronaCategories(categories);
      }
   }

   private void updateKoronaCategories(List<Category> categories) {
      Set<String> selectedCategories = koronaCategories.getParameters().stream()
            .filter(BooleanParameter::getBooleanValue)
            .map(BaseParameter::getPersistentName)
            .collect(Collectors.toSet());
      koronaCategories.clear();
      for (Category category : categories) {
         String name = category.getName();
         boolean selected = selectedCategories.contains(name);
         koronaCategories.addParameter(newKoronaCategoryParameter(name, selected));
      }
   }

   private BooleanParameter newKoronaCategoryParameter(String persistentName, boolean initialValue) {
      BooleanParameter parameter = new BooleanParameter(new Name(persistentName), initialValue);
      parameter.setEnabled(plotCategories.getBooleanValue());
      return parameter;
   }
}
