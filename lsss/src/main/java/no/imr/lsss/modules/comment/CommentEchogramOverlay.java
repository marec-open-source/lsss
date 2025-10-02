package no.imr.lsss.modules.comment;

import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiText;
import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public final class CommentEchogramOverlay extends BaseEchogramOverlay {
   private static final float Y = 5;

   private final Supplier<CommentDataModule> commentDataModule = moduleSupplier(CommentDataModule.class);

   public CommentEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
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
      return commentDataModule.get().getPopupMenu(commentDataModule.get().getActiveComment());
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
            .map(comment -> {
               float x = pingSettings.millisToX(comment.timeInMillis());
               if (x < 0 || x > width) {
                  return null;
               }
               return new Marker(comment, x);
            })
            .filter(Objects::nonNull)
            .toList();
      if (markers.isEmpty()) {
         return null;
      }
      return new DisplayData(markers);
   }

   private record Marker(Comment comment, float x) {
   }

   private final class DisplayData extends OverlayDisplayData {
      private final List<Marker> markers;

      private DisplayData(List<Marker> markers) {
         this.markers = markers;
      }

      @Override
      public void draw(Graphics2D g2d) {
         Comment activeComment = commentDataModule.get().getActiveComment();
         Set<Comment> selectedComments = commentDataModule.get().getSelection().getSelectedComments();
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
      }

      private static void drawMarker(Graphics2D g2d, Marker marker, Color color) {
         GuiText.draw(g2d, "C", color,
               marker.x, Y,
               GuiText.HorizontalAlignment.CENTER, GuiText.VerticalAlignment.CENTER, null);
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         Comment comment = getIntersectingComment(rectangle);
         commentDataModule.get().setActiveComment(comment);
         return comment != null;
      }

      private @Nullable Comment getIntersectingComment(Rectangle2D rectangle) {
         for (Marker marker : markers) {
            if (rectangle.contains(marker.x, Y)) {
               return marker.comment;
            }
         }
         return null;
      }
   }
}
