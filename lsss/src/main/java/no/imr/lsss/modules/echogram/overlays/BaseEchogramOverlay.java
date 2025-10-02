package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.modules.BaseModuleOverlay;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramArea;
import no.imr.lsss.modules.echogram.EchogramModule;

import java.awt.Graphics2D;

/**
 * Base class for echogram overlays.
 */
public abstract class BaseEchogramOverlay extends BaseModuleOverlay {
   private final EchogramModule echogramModule;

   protected BaseEchogramOverlay(ModuleInfo<?> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      this.echogramModule = echogramModule;
   }

   @Override
   public BaseOverlaidModule<?> getOverlaidModule() {
      return echogramModule;
   }

   public EchogramModule getEchogramModule() {
      return echogramModule;
   }

   public EchogramPingSettings getPingSettings() {
      return echogramModule.getPingSettings();
   }

   public EchogramZSettings getZSettings() {
      return echogramModule.getZSettings();
   }

   protected abstract class TransformedDisplayData extends OverlayDisplayData {
      private final EchogramArea echogramArea = echogramModule.getEchogramArea();

      protected TransformedDisplayData() {
      }

      @Override
      public final void draw(Graphics2D g2d) {
         transform(g2d);
         transformedDraw(g2d);
      }

      protected abstract void transformedDraw(Graphics2D g2d);

      private void transform(Graphics2D g2d) {
         if (echogramModule.getEchogramArea() == echogramArea) {
            return;
         }
         if (echogramArea.pingRange().isEmpty()) {
            return;
         }

         float x0 = getPingSettings().pingIndexToX(echogramArea.pingRange().begin());
         float x1 = getPingSettings().pingIndexToX(echogramArea.pingRange().end());

         float y0 = getZSettings().zToY(echogramArea.zRange().min());
         float y1 = getZSettings().zToY(echogramArea.zRange().max());

         g2d.translate(x0, y0);
         g2d.scale((x1 - x0) / echogramArea.width(), (y1 - y0) / echogramArea.height());
      }
   }
}
