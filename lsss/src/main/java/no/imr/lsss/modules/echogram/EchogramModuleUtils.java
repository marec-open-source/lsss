package no.imr.lsss.modules.echogram;

import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleEditor;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsUtils;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingConf;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class EchogramModuleUtils {
   private EchogramModuleUtils() {
   }

   public static void addZoomItems(JPopupMenu popupMenu, LSSS lsss, EchogramZSettings zSettings) {
      JMenuItem maximallyZoomOutItem = MiscIcons.HOME.on(popupMenu.add("Zoom out maximally"));
      maximallyZoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
      maximallyZoomOutItem.setEnabled(zSettings.isZoomedVertically()
            || !lsss.getInterpretationSettings().getDataFileSet().getTotalRange().equals(lsss.getInterpretationSettings().getPingRange()));
      maximallyZoomOutItem.addActionListener(e -> lsss.getInterpretationSettings().getNavigationHistory().zoomOut());

      JMenuItem verticallyZoomOutItem = popupMenu.add("Zoom out vertically");
      verticallyZoomOutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, KeyEvent.SHIFT_DOWN_MASK));
      verticallyZoomOutItem.setEnabled(zSettings.isZoomedVertically());
      verticallyZoomOutItem.addActionListener(e -> zSettings.zoomOut());

      JMenuItem upItem = MiscIcons.STEP_UP.on(popupMenu.add("Step up"));
      upItem.setEnabled(zSettings.minZoomedZ.getFloatValue() > zSettings.minZ.getFloatValue());
      upItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
      upItem.addActionListener(e -> zSettings.stepUp());

      JMenuItem downItem = MiscIcons.STEP_DOWN.on(popupMenu.add("Step down"));
      downItem.setEnabled(zSettings.maxZoomedZ.getFloatValue() < zSettings.maxZ.getFloatValue());
      downItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
      downItem.addActionListener(e -> zSettings.stepDown());
   }

   public static void addViewPreprocessingMenuItem(JPopupMenu popupMenu, @Nullable Ping ping, PreprocessingConf preprocessingConf) {
      List<Cds0Datagram> cds0Datagrams;
      Path dataDir;
      if (ping != null) {
         cds0Datagrams = ping.getPingConfiguration().getConfigurationItems(Cds0Datagram.class).toList();
         dataDir = ping.getPingConfiguration().getRawFileConfiguration().getDataFile().getParent();
      } else {
         cds0Datagrams = List.of();
         dataDir = null;
      }

      JMenuItem viewPreprocessingItem = popupMenu.add("View preprocessing configuration...");
      viewPreprocessingItem.setEnabled(!cds0Datagrams.isEmpty());
      viewPreprocessingItem.addActionListener(ae -> {
         LSSS lsss = preprocessingConf.getLSSS();
         try {
            ConfigFileSettings configFileSettings = null;
            if (dataDir != null) {
               configFileSettings = ConfigFileSettingsUtils.loadConfigFileSettingsFromCopiedConfigFiles(dataDir, lsss.getKorona());
            }
            if (configFileSettings == null) {
               configFileSettings = preprocessingConf.getMainSetup().createConfigFileSettings();
            }
            ModuleContainer moduleContainer = new ModuleContainer(lsss.getKorona(), configFileSettings);
            for (Cds0Datagram cds0Datagram : cds0Datagrams) {
               moduleContainer.appendXml(cds0Datagram.getDocument().getRootElement());
            }
            new ModuleEditor(moduleContainer, false, lsss.getFrame())
                  .show();
         } catch (IOException e) {
            preprocessingConf.getLSSS().showError(viewPreprocessingItem, "Could not create module configuration editor.", e);
         }
      });
   }

   public static EchogramPoint imagePointToClampedEchogramPoint(Point2D point, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      PingIndex pingIndex = pingSettings.xToClampedContainingPingIndex(point.getX());
      return new EchogramPoint(pingIndex, zSettings.yToClampedDepth(point.getY(), pingIndex));
   }

   public static EchogramRectangle toSelectionEchogramRectangle(EchogramPoint a, EchogramPoint b, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      PingRange pingRange = PingRange.from(List.of(a.pingIndex(), b.pingIndex()), pingSettings.getPingContainer());
      DepthTransform depthTransform = zSettings.getDepthTransform();
      float za = depthTransform.depthToZ(a);
      float zb = depthTransform.depthToZ(b);
      float zPerPixel = zSettings.getZoomedZRange().getSize() / zSettings.getHeight();
      FloatRange zRange = FloatRange.of(
            Math.min(za, zb),
            Math.max(za, zb) + zPerPixel
      );
      return new EchogramRectangle(pingRange, zRange, depthTransform);
   }

   public static Rectangle2D.Float toImageRectangle(EchogramRectangle echogramRectangle, EchogramPingSettings pingSettings, EchogramZSettings zSettings) {
      float x1 = pingSettings.pingIndexToX(echogramRectangle.pingRange().begin());
      float x2 = pingSettings.pingIndexToX(echogramRectangle.pingRange().end());
      float y1 = zSettings.zToY(echogramRectangle.zRange().min());
      float y2 = zSettings.zToY(echogramRectangle.zRange().max());
      float w = Math.max(0, x2 - x1 - 1);
      float h = Math.max(0, y2 - y1 - 1);
      return new Rectangle2D.Float(x1, y1, w, h);
   }
}
