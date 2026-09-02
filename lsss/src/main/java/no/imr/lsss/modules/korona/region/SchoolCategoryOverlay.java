package no.imr.lsss.modules.korona.region;

import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.math.MathUtils;
import no.imr.tools.swing.PaintFactory;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.Map;

/**
 * If school categorization is run, this draws a filled shape over the school with the color
 * corresponding to the category when the color map is set to category.
 */
public final class SchoolCategoryOverlay extends KoronaRegionEchogramOverlay {
   public SchoolCategoryOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      getConfigurationManager().getSurveyMiscConf().useSchoolCategorization.subscribe(this::setEnabledByUser);
   }

   @Override
   public boolean isUserVisibleForegroundOverlay() {
      return false;
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (regionPaths.isEmpty()) {
         return null;
      }
      return new DisplayData();
   }

   public static @Nullable CategoryVariable getCategoryVariable(ColorConverter converter) {
      DiscreteVariable discreteVariable = converter.getDiscreteVariable();
      if (discreteVariable instanceof CategoryVariable categoryVariable) {
         return categoryVariable;
      } else {
         return null;
      }
   }

   private final class DisplayData implements OverlayDisplayData {
      private DisplayData() {
      }

      @Override
      public void draw(Graphics2D g2d) {
         Cac0Datagram cac0Datagram = getInterpretationSettings().getDataFileSet().getConfigurationItem(Cac0Datagram.class);
         if (cac0Datagram == null) {
            return;
         }

         CategoryVariable categoryVariable = getCategoryVariable(getInterpretationSettings().getColorConverterContainer().getColorConverter());
         if (categoryVariable == null) {
            return;
         }

         for (Map.Entry<KoronaRegionLSSS, KoronaRegionEchogramOverlay.RegionPath> entry : regionPaths.entrySet()) {
            KoronaRegionLSSS koronaRegion = entry.getKey();
            Cas0Datagram cas0Datagram = koronaRegion.getCas0Datagram();
            if (cas0Datagram == null) {
               continue;
            }
            byte categoryNumber = cas0Datagram.getBestCategory(categoryVariable, (byte) -1);
            if (categoryNumber < 0) {
               continue;
            }
            Color color = cac0Datagram.numberToCategory(categoryNumber).getColor();
            int textureSize = 9;
            int offset = MathUtils.mod((int) entry.getValue().bounds().getMinX(), textureSize);
            Paint paint = PaintFactory.createCrossedPaint(color, textureSize, 3, offset);
            g2d.setPaint(paint);
            g2d.fill(entry.getValue().path());
         }
      }
   }
}
