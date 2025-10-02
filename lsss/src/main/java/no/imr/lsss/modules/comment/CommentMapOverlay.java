package no.imr.lsss.modules.comment;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.ExtendedSurveyLine;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.modules.map.overlays.BaseMapOverlay;
import no.imr.tools.geo.GeoTransform;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.Range;
import no.imr.tools.swing.GuiText;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public final class CommentMapOverlay extends BaseMapOverlay {
   private final Supplier<CommentDataModule> commentDataModule = moduleSupplier(CommentDataModule.class);

   public CommentMapOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            commentDataModule.get().getChangeManager(),
            getInterpretationSettings().getMapSettings().getExtendedSurveyLine().getChangeManager(),
            getMapModule().getGeographicalAreaChangeManager()
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
      ExtendedSurveyLine extendedSurveyLine = getInterpretationSettings().getMapSettings().getExtendedSurveyLine();
      Range<Long> millisRange = extendedSurveyLine.getTotalPingRange().toMillisRange();
      Collection<Comment> comments = commentDataModule.get().getComments(millisRange);
      if (comments.isEmpty()) {
         return null;
      }
      Rectangle2D geoRect = getMapModule().getGeoRect();
      GeoTransform geoTransform = getMapModule().getGeoTransform();
      List<Marker> markers = comments.stream()
            .map(comment -> {
               PingIndex pingIndex = extendedSurveyLine.getClosestPingIndex(PingMapping.millisToTimeValue(comment.timeInMillis()), PingMapping.TIME);
               GeoPoint geoPoint = pingIndex.getGeographicalPosition();
               if (geoPoint == null || !geoRect.contains(geoPoint)) {
                  return null;
               }
               Point2D.Float pixPoint = new Point2D.Float();
               geoTransform.geoToPix(geoPoint, pixPoint);
               return new Marker(comment, pixPoint.x, pixPoint.y);
            })
            .filter(Objects::nonNull)
            .toList();
      if (markers.isEmpty()) {
         return null;
      }
      return new DisplayData(markers);
   }

   private record Marker(Comment comment, float x, float y) {
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
               marker.x, marker.y,
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
            if (rectangle.contains(marker.x, marker.y)) {
               return marker.comment;
            }
         }
         return null;
      }
   }
}
