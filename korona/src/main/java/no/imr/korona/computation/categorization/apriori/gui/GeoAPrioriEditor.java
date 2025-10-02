package no.imr.korona.computation.categorization.apriori.gui;

import no.imr.korona.computation.categorization.apriori.APrioriPolygon;
import no.imr.korona.computation.categorization.apriori.GeoAPriori;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.geo.Earth;
import no.imr.tools.geo.GeoBoxBuilder;
import no.imr.tools.geo.GeoZoom;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.swing.GuiText;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.PopupMenuAdapter;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.web.Wms;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public final class GeoAPrioriEditor {
   private final GeoAPriori geoAPriori;

   private List<EditorPolygon> editorPolygons = List.of();

   private boolean zoomInitialized;
   private GeoZoom geoZoom = new GeoZoom();

   private BufferedImage mapImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
   private GeoZoom mapGeoZoom = geoZoom;
   private final CoalescingExecutor mapDownloader = new CoalescingExecutor(Exec.CACHED_THREAD_POOL);
   private final Listener mapUpdater = Listeners.debouncing(() -> mapDownloader.execute(this::downloadMap));

   private final JComponent component = new JComponent() {
      @Override
      protected void paintComponent(Graphics g) {
         GeoAPrioriEditor.this.paint((Graphics2D) g);
      }
   };

   private boolean popupShowing;

   private @Nullable ActivePolygon activePolygon;
   private @Nullable ActivePolygonLine activePolygonLine;
   private @Nullable ActivePolygonPoint activePolygonPoint;

   private @Nullable Point mousePosition;
   private @Nullable EditorInteraction interaction;

   public GeoAPrioriEditor(GeoAPriori geoAPriori) {
      this.geoAPriori = geoAPriori;

      component.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            int width = Math.max(1, getWidth());
            int height = Math.max(1, getHeight());
            mapImage = component.getGraphicsConfiguration().createCompatibleImage(width, height);
            Graphics2D g = mapImage.createGraphics();
            g.setBackground(Color.WHITE);
            g.clearRect(0, 0, width, height);
            g.dispose();
            setGeoZoom(geoZoom.withSize(width, height));
            if (!zoomInitialized) {
               zoomInitialized = true;
               resetZoom();
            }
         }
      });

      component.setFocusable(true);
      component.addKeyListener(new MyKeyListener());
      MyMouseListener mouseListener = new MyMouseListener();
      component.addMouseListener(mouseListener);
      component.addMouseMotionListener(mouseListener);
      component.addMouseWheelListener(mouseListener);
   }

   public JComponent getComponent() {
      return component;
   }

   private int getWidth() {
      return component.getWidth();
   }

   private int getHeight() {
      return component.getHeight();
   }

   GeoAPriori getGeoAPriori() {
      return geoAPriori;
   }

   GeoZoom getGeoZoom() {
      return geoZoom;
   }

   void setGeoZoom(GeoZoom geoZoom) {
      this.geoZoom = geoZoom;
      mapUpdater.listen();
      update();
   }

   private void downloadMap() {
      GeoZoom downloadGeoZoom = geoZoom;
      BufferedImage downloadedImage = Wms.downloadMap(Wms.DEFAULT_WMS_URL, Wms.DEFAULT_WMS_LAYERS, downloadGeoZoom.width, downloadGeoZoom.height, downloadGeoZoom.getGeoRect());
      SwingDelayer.invokeLater(mapDownloader, () -> {
         mapGeoZoom = downloadGeoZoom;
         Graphics2D g = mapImage.createGraphics();
         g.drawImage(downloadedImage, null, 0, 0);
         g.dispose();
         repaint();
      });
   }

   private EditorInteraction pointToInteraction(Point point) {
      if (activePolygonPoint != null) {
         return new MovePointInteraction(this, geoAPriori.getPolygons().get(activePolygonPoint.polygonIndex), activePolygonPoint.pointIndex);
      }
      if (activePolygonLine != null) {
         List<Point2D> pixPoints = editorPolygons.get(activePolygonLine.polygonIndex).pixPoints;
         Point2D p1 = pixPoints.get(activePolygonLine.lineIndex);
         int nextIndex = (activePolygonLine.lineIndex + 1) % pixPoints.size();
         Point2D p2 = pixPoints.get(nextIndex);
         Point2D p = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
         if (mousePosition != null && isPixPointClose(p, mousePosition)) {
            GeoPoint geoPoint = geoZoom.geoTransform.pixToGeo(p);
            APrioriPolygon aPrioriPolygon = geoAPriori.getPolygons().get(activePolygonLine.polygonIndex);
            aPrioriPolygon.getGeoPoints().add(nextIndex, geoPoint);
            aPrioriPolygon.update();
            setActivePolygonPoint(new ActivePolygonPoint(activePolygonLine.polygonIndex, nextIndex));
            setActivePolygonLine(null);
            update();
            return new MovePointInteraction(this, aPrioriPolygon, nextIndex);
         }
      }
      return new PanInteraction(this, geoZoom.geoTransform.pixToGeo(point));
   }

   private void showPopup(MouseEvent mouseEvent) {
      popupShowing = true;

      JPopupMenu menu = new JPopupMenu();

      JMenuItem resetZoomItem = MiscIcons.HOME.on(menu.add("Reset zoom"));
      resetZoomItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));
      resetZoomItem.addActionListener(e -> resetZoom());

      menu.addSeparator();

      JMenuItem editAPrioriItem = MiscIcons.EDIT.on(menu.add("Edit a priori"));
      if (activePolygon != null) {
         editAPrioriItem.addActionListener(e -> {
            APrioriPolygon aPrioriPolygon = geoAPriori.getPolygons().get(activePolygon.polygonIndex);
            new SimpleInputDialog<>("A priori", "A priori", Utils.toString(aPrioriPolygon.getAPrioriValue()), Float::parseFloat)
                  .show(component)
                  .ifPresent(aPrioriPolygon::setAPrioriValue);
         });
      } else if (activePolygonLine == null && activePolygonPoint == null) {
         editAPrioriItem.setText("Edit default a priori");
         editAPrioriItem.addActionListener(e -> {
            new SimpleInputDialog<>("Default a priori", "Default a priori", Utils.toString(geoAPriori.getDefaultAPriori()), Float::parseFloat)
                  .show(component)
                  .ifPresent(geoAPriori::setDefaultAPriori);
         });
      } else {
         editAPrioriItem.setEnabled(false);
      }

      menu.addSeparator();

      JMenuItem deletePointItem = MiscIcons.DELETE.on(menu.add("Delete point"));
      deletePointItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
      if (activePolygonPoint != null && canDelete(activePolygonPoint)) {
         deletePointItem.addActionListener(e -> deletePoint());
      } else {
         deletePointItem.setEnabled(false);
      }

      menu.addSeparator();

      JMenuItem addPolygonItem = MiscIcons.ADD.on(menu.add("Add polygon"));
      addPolygonItem.addActionListener(e -> addPolygon());

      JMenuItem polygonUpItem = MiscIcons.ARROW_UP.on(menu.add("Move polygon up"));
      polygonUpItem.setEnabled(activePolygon != null && activePolygon.polygonIndex > 0);
      polygonUpItem.addActionListener(e -> movePolygon(-1));

      JMenuItem polygonDownItem = MiscIcons.ARROW_DOWN.on(menu.add("Move polygon down"));
      polygonDownItem.setEnabled(activePolygon != null && activePolygon.polygonIndex < geoAPriori.getPolygons().size() - 1);
      polygonDownItem.addActionListener(e -> movePolygon(1));

      JMenuItem deletePolygonItem = MiscIcons.DELETE.on(menu.add("Delete polygon"));
      deletePolygonItem.setEnabled(activePolygon != null);
      deletePolygonItem.addActionListener(e -> deletePolygon());

      menu.show(component, mouseEvent.getX(), mouseEvent.getY());

      menu.addPopupMenuListener(new PopupMenuAdapter() {
         @Override
         public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            popupShowing = false;
            if (mousePosition != null) {
               updateActive(mousePosition);
            }
         }
      });
   }

   private void resetZoom() {
      GeoBoxBuilder geoBoxBuilder = new GeoBoxBuilder();
      geoAPriori.getPolygons().stream()
            .flatMap(polygon -> polygon.getGeoPoints().stream())
            .forEach(geoBoxBuilder::add);
      Rectangle2D geoBox = geoBoxBuilder.build();
      if (geoBox == null) {
         setGeoZoom(geoZoom.withContainingGeoRect(new Rectangle2D.Double(-180, -90, 360, 180), 1));
      } else {
         setGeoZoom(geoZoom.withContainingGeoRect(geoBox, 1.5));
      }
   }

   private void deletePoint() {
      if (activePolygonPoint == null || !canDelete(activePolygonPoint)) {
         return;
      }
      APrioriPolygon polygon = geoAPriori.getPolygons().get(activePolygonPoint.polygonIndex);
      polygon.getGeoPoints().remove(activePolygonPoint.pointIndex);
      polygon.update();

      clearActive();
      update();
   }

   private boolean canDelete(ActivePolygonPoint polygonPoint) {
      return geoAPriori.getPolygons().get(polygonPoint.polygonIndex).getGeoPoints().size() > 3;
   }

   private void addPolygon() {
      clearActive();
      setInteraction(new AddPolygonInteraction(this));
   }

   private void movePolygon(int step) {
      if (activePolygon == null) {
         return;
      }
      int otherIndex = Math.clamp(activePolygon.polygonIndex + step, 0, geoAPriori.getPolygons().size());
      Collections.swap(geoAPriori.getPolygons(), activePolygon.polygonIndex, otherIndex);

      clearActive();
      update();
   }

   private void deletePolygon() {
      if (activePolygon == null) {
         return;
      }
      geoAPriori.getPolygons().remove(activePolygon.polygonIndex);

      clearActive();
      update();
   }

   void repaint() {
      component.repaint();
   }

   private void paint(Graphics2D graphics2D) {
      if (mapGeoZoom != geoZoom) {
         Graphics2D g = (Graphics2D) graphics2D.create();
         Rectangle2D pixRect = geoZoom.geoTransform.geoToPix(mapGeoZoom.getGeoRect());
         g.translate(pixRect.getX(), pixRect.getY());
         g.scale(pixRect.getWidth() / mapGeoZoom.width, pixRect.getHeight() / mapGeoZoom.height);
         g.drawImage(mapImage, null, 0, 0);
         g.dispose();
      } else {
         graphics2D.drawImage(mapImage, null, 0, 0);
      }

      graphics2D.setColor(Color.BLACK);

      for (int i = editorPolygons.size() - 1; i >= 0; i--) {
         editorPolygons.get(i).paint(graphics2D);
      }

      if (mousePosition != null) {
         GeoPoint mouseGeo = geoZoom.geoTransform.pixToGeo(mousePosition);
         String format = Utils.getPrecisionString(geoZoom.latitudeExtent / geoZoom.height);
         GuiText.draw(graphics2D, Earth.formatGeoPoint(mouseGeo, format), Color.BLACK, 2, getHeight() - 2,
               GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.BOTTOM, null);
         float aPrioriValue = geoAPriori.getAPrioriValue(mouseGeo);
         GuiText.draw(graphics2D, "A priori: " + Utils.toString(aPrioriValue), Color.BLACK, 2, getHeight() - 20,
               GuiText.HorizontalAlignment.LEFT, GuiText.VerticalAlignment.BOTTOM, null);
      }

      if (interaction != null) {
         interaction.paint(graphics2D);
      }
   }

   private void clearActive() {
      setActivePolygon(null);
      setActivePolygonLine(null);
      setActivePolygonPoint(null);
   }

   private void updateActive(Point pixPoint) {
      if (popupShowing) {
         return;
      }
      clearActive();
      GeoPoint geoPoint = geoZoom.geoTransform.pixToGeo(pixPoint);
      for (int polygonIndex = 0; polygonIndex < editorPolygons.size(); polygonIndex++) {
         EditorPolygon polygon = editorPolygons.get(polygonIndex);
         List<Point2D> pixPoints = polygon.pixPoints;
         for (int pointIndex = 0; pointIndex < pixPoints.size(); pointIndex++) {
            Point2D p = pixPoints.get(pointIndex);
            if (isPixPointClose(pixPoint, p)) {
               setActivePolygonPoint(new ActivePolygonPoint(polygonIndex, pointIndex));
               return;
            }
         }
         for (int lineIndex = 0; lineIndex < pixPoints.size(); lineIndex++) {
            Point2D p1 = pixPoints.get(lineIndex);
            Point2D p2 = pixPoints.get((lineIndex + 1) % pixPoints.size());
            if (Line2D.ptSegDistSq(p1.getX(), p1.getY(), p2.getX(), p2.getY(), pixPoint.x, pixPoint.y) < 16) {
               setActivePolygonLine(new ActivePolygonLine(polygonIndex, lineIndex));
               return;
            }
         }
         if (geoAPriori.getPolygons().get(polygonIndex).contains(geoPoint)) {
            setActivePolygon(new ActivePolygon(polygonIndex));
            return;
         }
      }
   }

   private void setActivePolygon(@Nullable ActivePolygon activePolygon) {
      this.activePolygon = activePolygon;
      repaint();
   }

   private void setActivePolygonLine(@Nullable ActivePolygonLine activePolygonLine) {
      this.activePolygonLine = activePolygonLine;
      repaint();
   }

   private void setActivePolygonPoint(@Nullable ActivePolygonPoint activePolygonPoint) {
      this.activePolygonPoint = activePolygonPoint;
      repaint();
   }

   void setInteraction(@Nullable EditorInteraction interaction) {
      this.interaction = interaction;
      repaint();
   }

   void update() {
      editorPolygons = IntStream.range(0, geoAPriori.getPolygons().size())
            .mapToObj(EditorPolygon::new)
            .toList();
      if (interaction == null && mousePosition != null) {
         updateActive(mousePosition);
      }
      repaint();
   }

   static boolean isPixPointClose(Point2D p1, Point2D p2) {
      return Math.abs(p1.getX() - p2.getX()) < 7 && Math.abs(p1.getY() - p2.getY()) < 7;
   }

   static Rectangle2D.Double pixPointToMarker(Point2D pixPoint) {
      return new Rectangle2D.Double(pixPoint.getX() - 3, pixPoint.getY() - 3, 6, 6);
   }

   private final class MyKeyListener extends KeyAdapter {
      private MyKeyListener() {
      }

      @Override
      public void keyPressed(KeyEvent e) {
         if (interaction != null) {
            interaction.keyPressed(e);
            return;
         }
         switch (e.getKeyCode()) {
            case KeyEvent.VK_HOME -> {
               resetZoom();
            }
            case KeyEvent.VK_DELETE -> {
               deletePoint();
            }
            default -> {
            }
         }
      }
   }

   private final class MyMouseListener extends MouseAdapter {
      private MyMouseListener() {
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
         setGeoZoom(geoZoom.zoomAroundPixPos(e.getPoint(), Math.pow(1.1, -e.getWheelRotation())));
      }

      @Override
      public void mouseEntered(MouseEvent e) {
         mousePosition = e.getPoint();
         if (interaction != null) {
            interaction.mouseEntered(e);
         } else {
            component.requestFocusInWindow();
            updateActive(mousePosition);
         }
      }

      @Override
      public void mouseExited(MouseEvent e) {
         mousePosition = null;
         if (interaction != null) {
            interaction.mouseExited(e);
         } else if (!popupShowing) {
            clearActive();
            setInteraction(null);
         }
      }

      @Override
      public void mousePressed(MouseEvent e) {
         if (interaction != null) {
            interaction.mousePressed(e);
         } else if (e.isPopupTrigger()) {
            showPopup(e);
         } else if (SwingUtilities.isLeftMouseButton(e)) {
            setInteraction(pointToInteraction(e.getPoint()));
         }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         if (interaction != null) {
            interaction.mouseReleased(e);
         } else if (e.isPopupTrigger()) {
            showPopup(e);
         }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
         if (interaction != null) {
            interaction.mouseClicked(e);
         }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         mousePosition = e.getPoint();
         if (interaction != null) {
            interaction.mouseDragged(e);
         }
      }

      @Override
      public void mouseMoved(MouseEvent e) {
         mousePosition = e.getPoint();
         if (interaction != null) {
            interaction.mouseMoved(e);
         } else {
            updateActive(mousePosition);
         }
      }
   }

   private final class EditorPolygon {
      private final int polygonIndex;
      private final List<Point2D> pixPoints = new ArrayList<>();
      private final Path2D path = new Path2D.Float();

      private EditorPolygon(int polygonIndex) {
         this.polygonIndex = polygonIndex;
         List<GeoPoint> geoPoints = geoAPriori.getPolygons().get(polygonIndex).getGeoPoints();
         for (int i = 0; i < geoPoints.size(); i++) {
            GeoPoint geoPoint = geoPoints.get(i);
            Point2D pixPoint = geoZoom.geoTransform.geoToPix(geoPoint);
            pixPoints.add(pixPoint);
            if (i == 0) {
               path.moveTo(pixPoint.getX(), pixPoint.getY());
            } else {
               path.lineTo(pixPoint.getX(), pixPoint.getY());
            }
         }
         path.closePath();
      }

      private void paint(Graphics2D graphics2D) {
         Color polygonColor;
         if (activePolygon != null && activePolygon.polygonIndex == polygonIndex) {
            polygonColor = new Color(0xaaff0000, true);
         } else {
            polygonColor = new Color(0xcc808080, true);
         }
         graphics2D.setColor(polygonColor);
         graphics2D.fill(path);
         graphics2D.setColor(Color.BLACK);
         graphics2D.draw(path);

         if (activePolygonLine != null && activePolygonLine.polygonIndex == polygonIndex) {
            Point2D p1 = pixPoints.get(activePolygonLine.lineIndex);
            Point2D p2 = pixPoints.get((activePolygonLine.lineIndex + 1) % pixPoints.size());
            Point2D p = new Point2D.Double((p1.getX() + p2.getX()) / 2, (p1.getY() + p2.getY()) / 2);
            if (mousePosition != null && isPixPointClose(p, mousePosition)) {
               graphics2D.setColor(Color.RED);
               graphics2D.setStroke(GuiUtils.STROKE_3);
            }
            graphics2D.draw(pixPointToMarker(p));
            graphics2D.setColor(Color.BLACK);
            graphics2D.setStroke(GuiUtils.STROKE_1);
         }

         pixPoints.forEach(p -> {
            graphics2D.draw(pixPointToMarker(p));
         });
         if (activePolygonPoint != null && activePolygonPoint.polygonIndex == polygonIndex) {
            graphics2D.setColor(Color.RED);
            graphics2D.setStroke(GuiUtils.STROKE_3);
            graphics2D.draw(pixPointToMarker(pixPoints.get(activePolygonPoint.pointIndex)));
            graphics2D.setColor(Color.BLACK);
            graphics2D.setStroke(GuiUtils.STROKE_1);
         }
      }
   }

   private record ActivePolygon(int polygonIndex) {
   }

   private record ActivePolygonLine(int polygonIndex, int lineIndex) {
   }

   private record ActivePolygonPoint(int polygonIndex, int pointIndex) {
   }
}
