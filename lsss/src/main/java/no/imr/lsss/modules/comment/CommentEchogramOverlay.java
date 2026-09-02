package no.imr.lsss.modules.comment;

import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiText;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class CommentEchogramOverlay extends BaseEchogramOverlay {
   private final FloatParameter markerFontSize = new FloatParameter(
         new Name("MarkerFontSize", "Marker font size"),
         12, Unit.PT, ValueConstraints.gte(1f),
         "Font size for comment markers");

   private final Supplier<CommentDataModule> commentDataModule = moduleSupplier(CommentDataModule.class);

   private final Listener recomputeListener = createRecomputeListener();
   private Font markerFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

   public CommentEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      markerFontSize.subscribe(size -> {
         markerFont = markerFont.deriveFont(size);
         recomputeListener.listen();
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            markerFontSize
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(recomputeListener, List.of(
            commentDataModule.get().getChangeManager(),
            getEchogramModule().echogramArea()
      ));
      registry.add(this::repaint, List.of(
            commentDataModule.get().getSelection().getChangeManager(),
            commentDataModule.get().getActiveCommentChangeManager()
      ));
   }

   @Override
   public void onDeactivate() {
      commentDataModule.get().setActiveComment(null);
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      return commentDataModule.get().getToolTipText(commentDataModule.get().getActiveComment());
   }

   @Override
   public @Nullable JPopupMenu getPopupMenu(Point point) {
      JPopupMenu popupMenu = commentDataModule.get().getPopupMenu(commentDataModule.get().getActiveComment());
      if (popupMenu != null) {
         popupMenu.addSeparator();
         popupMenu.add(createConfigureMenuItem());
      }
      return popupMenu;
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      Collection<Comment> comments = commentDataModule.get().getOpenedComments();
      if (comments.isEmpty()) {
         return null;
      }
      int width = getWidth();
      EchogramPingSettings pingSettings = getPingSettings();
      List<Marker> markers = comments.stream()
            .<Marker>mapMulti((comment, consumer) -> {
               float x = pingSettings.instantToX(comment.time());
               if (x < 0 || x > width) {
                  return;
               }
               consumer.accept(new Marker(comment, x));
            })
            .toList();
      if (markers.isEmpty()) {
         return null;
      }
      return new DisplayData(markers, markerFont, commentDataModule.get());
   }

   private record Marker(Comment comment, float x) {
   }

   private record DisplayData(
         List<Marker> markers,
         Font markerFont,
         CommentDataModule commentDataModule
   ) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         Font previousFont = g2d.getFont();
         g2d.setFont(markerFont);

         Comment activeComment = commentDataModule.getActiveComment();
         Set<Comment> selectedComments = commentDataModule.getSelection().getSelectedComments();
         Marker activeMarker = null;
         for (Marker marker : markers) {
            if (marker.comment == activeComment) {
               activeMarker = marker;
            } else if (selectedComments.contains(marker.comment)) {
               // Nothing here.
            } else {
               drawMarker(g2d, marker, Color.GREEN);
            }
         }
         for (Marker marker : markers) {
            if (selectedComments.contains(marker.comment)) {
               drawMarker(g2d, marker, Color.BLUE);
            }
         }
         if (activeMarker != null) {
            drawMarker(g2d, activeMarker, Color.RED);
         }

         g2d.setFont(previousFont);
      }

      private static void drawMarker(Graphics2D g2d, Marker marker, Color color) {
         GuiText.draw(g2d, "C", color,
               marker.x, 0,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.TOP, null);
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         Comment comment = getIntersectingComment(rectangle);
         commentDataModule.setActiveComment(comment);
         return comment != null;
      }

      private @Nullable Comment getIntersectingComment(Rectangle2D rectangle) {
         double s = markerFont.getSize();
         double y = s / 2;
         double w = Math.max(1, s * 0.75 - rectangle.getWidth());
         double h = Math.max(1, s - rectangle.getHeight());
         for (Marker marker : markers) {
            if (rectangle.intersects(marker.x - w / 2, y - h / 2, w, h)) {
               return marker.comment;
            }
         }
         return null;
      }
   }
}
