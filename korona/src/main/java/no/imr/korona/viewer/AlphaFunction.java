package no.imr.korona.viewer;

import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.FloatRange;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.Arrays;

public final class AlphaFunction {
   private static final String XML_ALPHAS = "AlphaValues";

   private final float[] alphas = new float[1024];
   private final ColorConverterContainer colorConverterContainer;
   private final ObservableValue<Float> maxAlpha;
   private final ChangeManager changeManager = new ChangeManager();

   AlphaFunction(ColorConverterContainer colorConverterContainer, ObservableValue<Float> maxAlpha) {
      this.colorConverterContainer = colorConverterContainer;
      this.maxAlpha = maxAlpha;

      float alpha = 0.75f * getMaxAlpha();
      draw(-60, alpha, -50, alpha);

      maxAlpha.subscribe(__ -> {
         for (int i = 0; i < alphas.length; i++) {
            alphas[i] = Math.min(alphas[i], getMaxAlpha());
         }
         alphaFunctionChanged();
      });
   }

   public float getMaxAlpha() {
      return maxAlpha.getValue();
   }

   public void alphaFunctionChanged() {
      changeManager.notifyListeners();
   }

   public void reset() {
      //Set alpha values to a reasonable curve. RK
      float maxAlpha = getMaxAlpha();
      //int indexUpper = valueToIndex(-42);
      //int indexUpper2 = valueToIndex(-35);
      //int indexUpperCut = valueToIndex(-30);
      FloatRange svRange = colorConverterContainer.getSV().getSettings().getRange();
      float svMin = svRange.min();  //-74
      float svMax = svRange.max();  //-40
      int indexUpper = Math.max(0, Math.min(valueToIndex(svMax - 7), alphas.length));
      int indexUpper2 = Math.max(0, Math.min(valueToIndex(svMax), alphas.length));
      int indexUpperCut = Math.max(0, Math.min(valueToIndex(svMax + 5), alphas.length));

      for (int i = indexUpper; i < alphas.length; i++) {
         alphas[i] = maxAlpha;
      }
      for (int i = indexUpperCut; i < alphas.length; i++) {
         alphas[i] = 0.05f * maxAlpha;
      }
      for (int i = indexUpper2; i < indexUpperCut; i++) {
         float diff = indexUpper2 - i;
         alphas[i] = maxAlpha - 0.95f * maxAlpha * diff / (indexUpper2 - indexUpperCut);
      }
      //int indexLower = valueToIndex(-70);
      int indexLower = Math.max(0, Math.min(valueToIndex(svMin + 12), alphas.length));
      float diffSquare = (indexUpper - indexLower) * (indexUpper - indexLower);
      for (int i = indexLower; i < indexUpper; i++) {
         float diff = indexLower - i;
         alphas[i] = 0.95f * maxAlpha * diff * diff / diffSquare + 0.05f * maxAlpha;
      }
      for (int i = 0; i < indexLower; i++) {
         alphas[i] = 0.05f * maxAlpha;
      }
      alphaFunctionChanged();
   }

   public void reset2() {
      //Set alpha values to a reasonable curve. RK
      float maxAlpha = getMaxAlpha();
      int indexUpper = valueToIndex(-42);
      int indexUpper2 = valueToIndex(-35);
      int indexUpperCut = valueToIndex(-30);
      for (int i = indexUpper; i < alphas.length; i++) {
         alphas[i] = maxAlpha;
      }
      for (int i = indexUpperCut; i < alphas.length; i++) {
         alphas[i] = 0.05f * maxAlpha;
      }
      for (int i = indexUpper2; i < indexUpperCut; i++) {
         float diff = indexUpper2 - i;
         alphas[i] = maxAlpha - 0.95f * maxAlpha * diff / (indexUpper2 - indexUpperCut);
      }
      int indexLower = valueToIndex(-70);
      float diffSquare = (indexUpper - indexLower) * (indexUpper - indexLower);
      for (int i = indexLower; i < indexUpper; i++) {
         float diff = indexLower - i;
         alphas[i] = 0.95f * maxAlpha * diff * diff / diffSquare + 0.05f * maxAlpha;
      }
      for (int i = 0; i < indexLower; i++) {
         alphas[i] = 0.05f * maxAlpha;
      }
      alphaFunctionChanged();

      //int minDb = getConfigurationManager().getSurveyMiscConf().preferredLowerThreshold.getIntValue();
      //float x = colorConverterContainer.getSV().getSettings().getRange().getMin();
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public void draw(float value0, float alpha0, float value1, float alpha1) {
      if (value0 > value1) {
         draw(value1, alpha1, value0, alpha0);
         return;
      }

      int i0 = valueToIndex(value0);
      int i1 = valueToIndex(value1);
      float maxAlpha = getMaxAlpha();
      float deltaAlpha = i0 == i1 ? 0 : (alpha1 - alpha0) / (i1 - i0);
      float alpha = alpha0;
      for (int i = i0; i <= i1; i++, alpha += deltaAlpha) {
         if (i >= 0 && i < alphas.length) {
            alphas[i] = Math.clamp(alpha, 0, maxAlpha);
         }
      }
   }

   public void shift(float value0, float value1) {
      int i0 = valueToIndex(value0);
      int i1 = valueToIndex(value1);
      int n = i1 - i0;
      if (n >= 0) {
         System.arraycopy(alphas, 0, alphas, n, alphas.length - n);
         Arrays.fill(alphas, 0, n, 0);
      } else {
         n = -n;
         System.arraycopy(alphas, n, alphas, 0, alphas.length - n);
         Arrays.fill(alphas, alphas.length - n, alphas.length, 0);
      }
   }

   public float getAlpha(float value) {
      int i = valueToIndex(value);
      if (i >= 0 && i < alphas.length) {
         return alphas[i];
      } else {
         return 0;
      }
   }

   private int valueToIndex(float value) {
      SingleValueColorConverter singleValueConverter = (SingleValueColorConverter) colorConverterContainer.getColorConverter();
      float x = singleValueConverter.getContinuousVariable().getSettings().getMaxRange().valueToFraction(value);
      return (int) Math.floor(x * alphas.length);
   }

   public Element toXml() {
      StringBuilder sb = new StringBuilder(10 * alphas.length);
      for (int i = 0; i < alphas.length; i++) {
         if (i > 0) {
            sb.append(i % 100 == 0 ? '\n' : ' ');
         }
         sb.append(Utils.toString(alphas[i]));
      }
      return DocumentHelper.createElement(XML_ALPHAS)
            .addText(sb.toString());
   }

   public void fromXml(Element element) {
      String s = element.getText().trim();
      String[] tokens = s.split("\\s+");
      int n = Math.max(tokens.length, alphas.length);
      for (int i = 0; i < n; i++) {
         alphas[i] = Float.parseFloat(tokens[i]);
      }
      alphaFunctionChanged();
   }
}
