package no.imr.korona.viewer;

import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenableProperty;

public final class TransferFunction {
   private final ColorConverterContainer colorConverterContainer = new ColorConverterContainer();
   private final ListenableProperty<Float> maxAlpha = new ListenableProperty<>(0.25f);
   private final AlphaFunction alphaFunction = new AlphaFunction(colorConverterContainer, maxAlpha);
   private final ChangeManager changeManager = new ChangeManager();

   public TransferFunction() {
      alphaFunction.getChangeManager().addListener(changeManager);
      colorConverterContainer.getChangeManager().addListener(changeManager);
   }

   public ColorConverterContainer getColorConverterContainer() {
      return colorConverterContainer;
   }

   public ColorConverter getColorConverter() {
      return colorConverterContainer.getColorConverter();
   }

   public ListenableProperty<Float> getMaxAlpha() {
      return maxAlpha;
   }

   public AlphaFunction getAlphaFunction() {
      return alphaFunction;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
