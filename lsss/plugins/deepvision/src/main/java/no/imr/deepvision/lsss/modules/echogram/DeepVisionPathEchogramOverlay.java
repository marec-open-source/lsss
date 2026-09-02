package no.imr.deepvision.lsss.modules.echogram;

import no.imr.deepvision.lsss.DeepVisionPlugin;
import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataAdministrator;
import no.imr.deepvision.lsss.engine.data.DeepVisionDataUtils;
import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.data.DeepVisionFrameInfo;
import no.imr.deepvision.lsss.engine.data.DeepVisionSelectedFrame;
import no.imr.deepvision.lsss.engine.data.LsssDeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;
import no.imr.deepvision.lsss.engine.data.pojo.LsssDeepVisionFrame;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMapping;
import no.imr.deepvision.lsss.engine.mapping.DeepVisionMappingManager;
import no.imr.deepvision.lsss.modules.image.DeepVisionImageViewModule;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.Utils;
import no.imr.tools.geo.Earth;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.TableToolTipBuilder;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.LineStripBuilder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public final class DeepVisionPathEchogramOverlay extends BaseEchogramOverlay {
   public static final Color SELECTED_FRAME_COLOR = Color.ORANGE;

   private final FloatParameter athwartDistanceThreshold = new FloatParameter(
         new Name("AthwartDistanceThreshold", "Athwart distance threshold"),
         100, Unit.METER, ValueConstraints.gte(0f),
         "Threshold before the path is colored with a different color");

   private final OptionalIntParameter maxNumberOfCircles = new OptionalIntParameter(
         new Name("MaxNumberOfCircles", "Maximum number of circles"),
         Optional.of(2000), Unit.COUNT, ValueConstraints.gte(0),
         "Maximum number of circles shown on screen simultaneously");

   private final IntParameter depthSmoothingRadius = new IntParameter(
         new Name("DepthSmoothingRadius", "Depth smoothing radius"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Smooth depths using this number of points before and after. 0 means no smoothing");

   private final DeepVisionDataAdministrator dataAdministrator;
   private final DeepVisionMappingManager deepVisionMappingManager;
   private final DeepVisionSelectedFrame deepVisionSelectedFrame;
   private final Supplier<DeepVisionImageViewModule> deepVisionImageViewModule = moduleSupplier(DeepVisionImageViewModule.class);

   public DeepVisionPathEchogramOverlay(ModuleInfo<DeepVisionPlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      DeepVisionEngine deepVisionEngine = moduleInfo.plugin().getDeepVisionEngine();
      dataAdministrator = deepVisionEngine.getDataAdministrator();
      deepVisionMappingManager = deepVisionEngine.getDeepVisionMappingManager();
      deepVisionSelectedFrame = deepVisionEngine.getDeepVisionSelectedFrame();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            athwartDistanceThreshold,
            maxNumberOfCircles,
            depthSmoothingRadius
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = createRecomputeListener();
      registry.add(recomputeListener, List.of(
            getEchogramModule().echogramArea(),
            deepVisionMappingManager.getChangeManager(),
            deepVisionSelectedFrame.getChangeManager(),
            deepVisionImageViewModule.get().showOnlyActiveImages,
            athwartDistanceThreshold,
            maxNumberOfCircles,
            depthSmoothingRadius
      ));
   }

   private record InteractionPoint(Instant lsssTime, float depth) {
   }

   private InteractionPoint imagePointToInteractionPoint(Point2D imagePoint) {
      double x = Math.clamp(imagePoint.getX(), 0, getWidth());
      double y = Math.clamp(imagePoint.getY(), 0, getHeight());
      Instant lsssTime = getPingSettings().xToInstant(x);
      PingIndex pingIndex = getPingSettings().xToClosestPingIndex(x);
      float depth = getZSettings().yToDepth(y, pingIndex);
      return new InteractionPoint(lsssTime, depth);
   }

   private @Nullable DeepVisionFrameInfo getContainingDeepVisionFrame(InteractionPoint interactionPoint, List<DeepVisionFileInfo> deepVisionFileInfos) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return null;
      }
      // Binary search through deep vision files based on time. If there is match in more than one file, pick the point which is closest in depth.
      DeepVisionMapping deepVisionMapping = deepVisionMappingManager.getDeepVisionMapping();
      List<DeepVisionFrameInfo> candidates = new ArrayList<>();
      for (DeepVisionFileInfo deepVisionFileInfo : deepVisionFileInfos) {
         Instant deepVisionTime = deepVisionMapping.lsssTimeToDeepVisionTime(interactionPoint.lsssTime(), deepVisionFileInfo);
         int i = Utils.binarySearch(deepVisionFileInfo.getDeepVisionFile().frames.frames, deepVisionTime, DeepVisionDataUtils::time);
         if (i < 0) {
            // not exact match
            i = -(i + 1); // conversion to insertion point
            if (i == 0 || i == deepVisionFileInfo.getDeepVisionFile().frames.frames.size()) {
               continue;
            }
            i--;
         }
         candidates.add(new DeepVisionFrameInfo(deepVisionFileInfo, i));
      }
      DeepVisionFrameInfo bestMatch = null;
      float minDistance = Float.POSITIVE_INFINITY;
      for (DeepVisionFrameInfo candidate : candidates) {
         List<DeepVisionFrame> frames = candidate.deepVisionFileInfo().getDeepVisionFile().frames.frames;
         float depth = getSmoothedDepth(frames, candidate.frameIndex());
         float dist = Math.abs(depth - interactionPoint.depth());
         if (dist < minDistance) {
            bestMatch = candidate;
            minDistance = dist;
         }
      }
      if (bestMatch == null) {
         long minTimeDistance = Long.MAX_VALUE;
         for (DeepVisionFileInfo deepVisionFileInfo : deepVisionFileInfos) {
            List<DeepVisionFrame> frames = deepVisionFileInfo.getDeepVisionFile().frames.frames;
            for (int i : new int[]{0, frames.size() - 1}) {
               DeepVisionFrame frame = frames.get(i);
               Instant deepVisionTime = DeepVisionDataUtils.time(frame);
               Instant lsssTime = deepVisionMappingManager.getDeepVisionMapping().deepVisionTimeToLsssTime(deepVisionTime, deepVisionFileInfo);
               long dist = Math.abs(lsssTime.until(interactionPoint.lsssTime, ChronoUnit.NANOS));
               if (dist < minTimeDistance) {
                  bestMatch = new DeepVisionFrameInfo(deepVisionFileInfo, i);
                  minTimeDistance = dist;
               }
            }
         }
      }
      return bestMatch;
   }

   private @Nullable DeepVisionFrameInfo getContainingDeepVisionFrame(InteractionPoint interactionPoint) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      DeepVisionMapping deepVisionMapping = deepVisionMappingManager.getDeepVisionMapping();
      return getContainingDeepVisionFrame(interactionPoint, dataAdministrator.getIntersectingFiles(pingRange, deepVisionMapping));
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      InteractionPoint interactionPoint = imagePointToInteractionPoint(mouseEvent.getPoint());
      DeepVisionFrameInfo containingDeepVisionFrame = getContainingDeepVisionFrame(interactionPoint);
      deepVisionSelectedFrame.setSelectedFrame(containingDeepVisionFrame);
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      InteractionPoint interactionPoint = imagePointToInteractionPoint(mouseEvent.getPoint());
      DeepVisionFrameInfo selectedFrame = deepVisionSelectedFrame.getSelectedFrame();
      if (selectedFrame == null) {
         selectedFrame = getContainingDeepVisionFrame(interactionPoint);
      }
      if (selectedFrame != null) {
         DeepVisionFileInfo deepVisionFileInfo = selectedFrame.deepVisionFileInfo();
         DeepVisionFrameInfo containingDeepVisionFrame = getContainingDeepVisionFrame(interactionPoint, List.of(deepVisionFileInfo));
         deepVisionSelectedFrame.setSelectedFrame(containingDeepVisionFrame);
      }
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      DeepVisionMapping deepVisionMapping = deepVisionMappingManager.getDeepVisionMapping();
      List<DeepVisionFileInfo> intersectingFiles = dataAdministrator.getIntersectingFiles(pingRange, deepVisionMapping);
      if (intersectingFiles.isEmpty()) {
         return null;
      }

      Rectangle bounds = getEchogramModule().getBounds();
      Path2D.Float portPath = new Path2D.Float();
      Path2D.Float inactivePortPath = new Path2D.Float();
      Path2D.Float path = new Path2D.Float();
      Path2D.Float inactivePath = new Path2D.Float();
      Path2D.Float starboardPath = new Path2D.Float();
      Path2D.Float inactiveStarboardPath = new Path2D.Float();
      LineStripBuilder pathBuilder = LineStripBuilders.coalescing(path, bounds);
      LineStripBuilder inactivePathBuilder = LineStripBuilders.coalescing(inactivePath, bounds);
      LineStripBuilder portBuilder = LineStripBuilders.coalescing(portPath, bounds);
      LineStripBuilder inactivePortBuilder = LineStripBuilders.coalescing(inactivePortPath, bounds);
      LineStripBuilder starboardBuilder = LineStripBuilders.coalescing(starboardPath, bounds);
      LineStripBuilder inactiveStarboardBuilder = LineStripBuilders.coalescing(inactiveStarboardPath, bounds);

      List<Circle> circles = new ArrayList<>();
      int width = getWidth();
      LineStripBuilder currentBuilder = pathBuilder;
      for (DeepVisionFileInfo intersectingFile : intersectingFiles) {
         LsssDeepVisionFileInfo lsssDeepVisionFileInfo = dataAdministrator.getLsssDeepVisionFileInfo(intersectingFile);
         Map<Long, LsssDeepVisionFrame> lsssFrameMapping = lsssDeepVisionFileInfo != null
               ? lsssDeepVisionFileInfo.timeFrameMapping()
               : Map.of();
         List<DeepVisionFrame> frames = intersectingFile.getDeepVisionFile().frames.frames;
         for (int i = 0; i < frames.size(); i++) {
            DeepVisionFrame frame = frames.get(i);
            Instant deepVisionTime = DeepVisionDataUtils.time(frame);
            Instant lsssTime = deepVisionMapping.deepVisionTimeToLsssTime(deepVisionTime, intersectingFile);
            float x = getPingSettings().instantToX(lsssTime);
            if (x < 0 || x > width) {
               continue;
            }
            PingIndex pingIndex = getPingSettings().xToClosestPingIndex(x);
            float depth = getSmoothedDepth(frames, i);
            float y = getZSettings().depthToY(depth, pingIndex);
            LsssDeepVisionFrame lsssFrame = lsssFrameMapping.get(frame.time);
            if (lsssFrame != null) {
               Color color = ColorUtils.parseColor(lsssFrame.color);
               if (color == null) {
                  Log.global.warning("Could not parse color: " + lsssFrame.color);
                  color = Color.BLACK;
               }
               float size = lsssFrame.size;
               circles.add(new Circle(x, y, size, color));
            }
            LineStripBuilder current;
            float v = deepVisionMapping.deepVisionTimeToAthwartDistanceMeters(deepVisionTime, intersectingFile);
            boolean active = frame.active;
            if (Math.abs(v) < athwartDistanceThreshold.getValue()) {
               current = active ? pathBuilder : inactivePathBuilder;
            } else if (v < 0) {
               current = active ? portBuilder : inactivePortBuilder;
            } else {
               current = active ? starboardBuilder : inactiveStarboardBuilder;
            }
            if (!currentBuilder.equals(current)) {
               currentBuilder.addPoint(x, y);
               currentBuilder.endLineStrip();
               currentBuilder = current;
            }
            currentBuilder.addPoint(x, y);
         }
         pathBuilder.endLineStrip();
         inactivePathBuilder.endLineStrip();
         portBuilder.endLineStrip();
         inactivePortBuilder.endLineStrip();
         starboardBuilder.endLineStrip();
         inactiveStarboardBuilder.endLineStrip();
      }

      int maxCircleCount = maxNumberOfCircles.getValue().orElse(Integer.MAX_VALUE);
      if (circles.size() > maxCircleCount) {
         circles = switch (maxCircleCount) {
            case 0 -> List.of();
            case 1 -> List.of(circles.get(circles.size() / 2));
            default -> {
               double f = (double) (circles.size() - 1) / (maxCircleCount - 1);
               yield IntStream.range(0, maxCircleCount)
                     .map(i -> (int) Math.round(i * f))
                     .mapToObj(circles::get)
                     .toList();
            }
         };
      }

      Rectangle2D.Float selectedPoint;
      DeepVisionFrameInfo selectedFrame = deepVisionSelectedFrame.getSelectedFrame();
      if (selectedFrame != null) {
         Instant deepVisionTime = DeepVisionDataUtils.time(selectedFrame.frame());
         Instant lsssTime = deepVisionMapping.deepVisionTimeToLsssTime(deepVisionTime, selectedFrame.deepVisionFileInfo());
         float x = getPingSettings().instantToX(lsssTime);
         PingIndex pingIndex = getPingSettings().xToClosestPingIndex(x);
         float depth = getSmoothedDepth(selectedFrame.deepVisionFileInfo().getDeepVisionFile().frames.frames, selectedFrame.frameIndex());
         float y = getZSettings().depthToY(depth, pingIndex);
         float r = 7;
         selectedPoint = new Rectangle2D.Float(x - r, y - r, 2 * r + 1, 2 * r + 1);
      } else {
         selectedPoint = null;
      }
      return transformed(new DisplayData(path, inactivePath, portPath, inactivePortPath, starboardPath, inactiveStarboardPath,
            deepVisionImageViewModule.get().showOnlyActiveImages.getBooleanValue(), selectedPoint, createPaths(circles)));
   }

   private float getSmoothedDepth(List<DeepVisionFrame> frames, int index) {
      int n = depthSmoothingRadius.getIntValue();
      int iMin = Math.max(index - n, 0);
      int iMax = Math.min(index + n, frames.size() - 1);
      double depthSum = 0;
      double weightSum = 0;
      for (int i = iMin; i <= iMax; i++) {
         double weight = n - Math.abs(i - index) + 1;
         depthSum += frames.get(i).depth * weight;
         weightSum += weight;
      }
      return (float) (depthSum / weightSum);
   }

   private static Map<Color, Path2D.Float> createPaths(List<Circle> circles) {
      Map<Color, Path2D.Float> map = new HashMap<>();
      for (Circle circle : circles) {
         float size = circle.size;
         map.computeIfAbsent(circle.color, _ -> new Path2D.Float())
               .append(new Ellipse2D.Float(circle.x - size / 2, circle.y - size / 2, size, size), false);
      }
      return map;
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      InteractionPoint interactionPoint = imagePointToInteractionPoint(point);
      DeepVisionFrameInfo frame = getContainingDeepVisionFrame(interactionPoint);
      if (frame != null) {
         GeoPoint geoPos = DeepVisionDataUtils.geoPoint(frame.frame());
         DeepVisionMapping deepVisionMapping = deepVisionMappingManager.getDeepVisionMapping();
         float athwartDistanceMeters = deepVisionMapping.deepVisionTimeToAthwartDistanceMeters(DeepVisionDataUtils.time(frame.frame()), frame.deepVisionFileInfo());
         return new TableToolTipBuilder()
               .addRow("Frame", Long.toString(frame.frame().time))
               .addRow("Geo pos", geoPos != null ? Earth.formatGeoPoint(geoPos, "%f") : "N/A")
               .addRow("Athwart distance [m]", Utils.format("%.2f", athwartDistanceMeters))
               .build();
      }
      return null;
   }

   private record DisplayData(
         Path2D.Float path,
         Path2D.Float inactivePath,
         Path2D.Float portPath,
         Path2D.Float inactivePortPath,
         Path2D.Float starboardPath,
         Path2D.Float inactiveStarboardPath,
         boolean useWeakerInactiveColors,
         Rectangle2D.@Nullable Float selectedPoint,
         Map<Color, Path2D.Float> ovals
   ) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         for (Map.Entry<Color, Path2D.Float> entry : ovals.entrySet()) {
            g2d.setColor(entry.getKey());
            g2d.fill(entry.getValue());
         }

         g2d.setColor(Color.BLACK);
         g2d.setStroke(GuiUtils.STROKE_2);
         g2d.draw(path);

         g2d.setColor(Color.RED);
         g2d.draw(portPath);

         g2d.setColor(Color.GREEN);
         g2d.draw(starboardPath);

         g2d.setColor(useWeakerInactiveColors ? brightnessAdjustedColor(Color.BLACK, 0.8f) : Color.BLACK);
         g2d.draw(inactivePath);

         g2d.setColor(useWeakerInactiveColors ? saturationAdjustedColor(Color.RED, 0.2f) : Color.RED);
         g2d.draw(inactivePortPath);

         g2d.setColor(useWeakerInactiveColors ? saturationAdjustedColor(Color.GREEN, 0.2f) : Color.GREEN);
         g2d.draw(inactiveStarboardPath);

         if (selectedPoint != null) {
            g2d.setColor(SELECTED_FRAME_COLOR);
            g2d.draw(selectedPoint);
         }
      }

      private static Color brightnessAdjustedColor(Color color, float brightness) {
         float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
         return new Color(Color.HSBtoRGB(hsb[0], hsb[1], brightness));
      }

      private static Color saturationAdjustedColor(Color color, float saturation) {
         float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
         return new Color(Color.HSBtoRGB(hsb[0], saturation, hsb[2]));
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         return selectedPoint != null && selectedPoint.intersects(rectangle)
               || GuiUtils.intersects(path, rectangle)
               || GuiUtils.intersects(inactivePath, rectangle)
               || GuiUtils.intersects(portPath, rectangle)
               || GuiUtils.intersects(inactivePortPath, rectangle)
               || GuiUtils.intersects(starboardPath, rectangle)
               || GuiUtils.intersects(inactiveStarboardPath, rectangle);
      }
   }

   private record Circle(float x, float y, float size, Color color) {
   }
}
