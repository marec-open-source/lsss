package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseBroadcaster;
import jakarta.ws.rs.sse.SseEventSink;
import no.imr.korona.data.ping.PingIndex;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.server.jaxrs.JaxRsApplication;
import no.imr.lsss.server.pojo.events.EventEchogramPos;
import no.imr.lsss.server.pojo.events.EventEmpty;
import no.imr.lsss.server.pojo.events.EventGeoPos;
import no.imr.lsss.server.pojo.events.EventKey;
import no.imr.lsss.server.pojo.events.EventModule;
import no.imr.lsss.server.pojo.events.EventMouse;
import no.imr.lsss.server.pojo.events.EventMouseWheel;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.observing.Subscription;

import javax.swing.SwingUtilities;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

@Singleton
public final class EventsResource {
   private final LSSS lsss;
   private List<DeepInputListener> viewModuleListeners = List.of();
   private List<Subscription> subscriptions = List.of();
   private final Sse sse;
   private final SseBroadcaster sseBroadcaster;
   private final List<SseEventSink> sseEventSinks = new ArrayList<>();

   @Inject
   EventsResource(JaxRsApplication jaxRsApplication, Sse sse) {
      lsss = jaxRsApplication.getLSSS();
      jaxRsApplication.addOnClose(this::close);

      this.sse = sse;
      sseBroadcaster = sse.newBroadcaster();
      sseBroadcaster.onClose(eventSink -> checkForClosedSinks());
      sseBroadcaster.onError(this::onEventSinkError);
   }

   @GET
   @Produces(MediaType.SERVER_SENT_EVENTS)
   public void getServerSentEvents(@Context SseEventSink eventSink) {
      SwingUtilities.invokeLater(() -> {
         if (sseEventSinks.isEmpty()) {
            addListeners();
         }
         sseEventSinks.add(eventSink);
         sseBroadcaster.register(eventSink);
         Log.global.info("Added event sink, size = " + sseEventSinks.size());
      });
   }

   private void onEventSinkError(SseEventSink eventSink, Throwable throwable) {
      checkForClosedSinks();
      if (eventSink.isClosed()) {
         return;
      }
      Log.global.log(Level.INFO, "Event sink error", throwable);
   }

   private void checkForClosedSinks() {
      SwingUtilities.invokeLater(() -> {
         if (sseEventSinks.removeIf(SseEventSink::isClosed)) {
            if (sseEventSinks.isEmpty()) {
               removeListeners();
            }
            Log.global.info("Removed event sink, size = " + sseEventSinks.size());
         }
      });
   }

   private void addListeners() {
      Log.global.info("Adding listeners");
      viewModuleListeners = lsss.getModuleManager().getModules(BaseViewModule.class)
            .map(viewModule -> new DeepInputListener(viewModule.getComponent(), new ViewModuleListener(viewModule)))
            .toList();
      Listener echogramPointListener = this::newEchogramPoint;
      subscriptions = List.of(
            lsss.getInterpretationSettings().mouseover().pingIndex().subscribe(echogramPointListener),
            lsss.getInterpretationSettings().mouseover().depth().subscribe(echogramPointListener),
            lsss.getInterpretationSettings().mouseover().geoPos().subscribe(this::newGeoPos),
            lsss.getInterpretationSettings().getEventChangeManager().subscribe(this::sendJsonEvent)
      );
   }

   private void removeListeners() {
      Log.global.info("Removing listeners");
      viewModuleListeners.forEach(DeepInputListener::stop);
      viewModuleListeners = List.of();
      subscriptions.forEach(Subscription::unsubscribe);
      subscriptions = List.of();
   }

   private void newEchogramPoint() {
      PingIndex pingIndex = lsss.getInterpretationSettings().mouseover().getPingIndex();
      Float depth = lsss.getInterpretationSettings().mouseover().getDepth();
      sendJsonEvent("echogramPos", pingIndex != null ? new EventEchogramPos(pingIndex, depth) : new EventEmpty());
   }

   private void newGeoPos(Optional<GeoPoint> geoPos) {
      sendJsonEvent("geoPos", geoPos.isPresent() ? new EventGeoPos(geoPos.get()) : new EventEmpty());
   }

   private void sendJsonEvent(InterpretationSettings.Event event) {
      sendJsonEvent(event.name(), event.pojoValue());
   }

   private void sendJsonEvent(String name, Object data) {
      OutboundSseEvent event = sse.newEventBuilder()
            .name(name)
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(data)
            .build();
      sseBroadcaster.broadcast(event);
   }

   private void close() {
      sseBroadcaster.close();
      SwingUtilities.invokeLater(this::removeListeners);
   }

   private final class ViewModuleListener extends MouseAndKeyAdapter {
      private final BaseViewModule viewModule;
      private boolean inside;

      private ViewModuleListener(BaseViewModule viewModule) {
         this.viewModule = viewModule;
      }

      @Override
      public void mouseEntered(MouseEvent e) {
         if (!inside && contains(e)) {
            inside = true;
            sendJsonEvent("mouseEntered", new EventModule(viewModule));
         }
      }

      @Override
      public void mouseExited(MouseEvent e) {
         if (inside && !contains(e)) {
            inside = false;
            sendJsonEvent("mouseExited", new EventModule(viewModule));
         }
      }

      private boolean contains(MouseEvent e) {
         Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), viewModule.getComponent());
         return viewModule.getComponent().contains(p);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
         sendJsonEvent("mouseClicked", new EventMouse(e));
      }

      @Override
      public void mousePressed(MouseEvent e) {
         sendJsonEvent("mousePressed", new EventMouse(e));
      }

      @Override
      public void mouseReleased(MouseEvent e) {
         sendJsonEvent("mouseReleased", new EventMouse(e));
      }

      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
         sendJsonEvent("mouseWheelMoved", new EventMouseWheel(e));
      }

      @Override
      public void keyTyped(KeyEvent e) {
         sendJsonEvent("keyTyped", new EventKey(e));
      }

      @Override
      public void keyPressed(KeyEvent e) {
         sendJsonEvent("keyPressed", new EventKey(e));
      }

      @Override
      public void keyReleased(KeyEvent e) {
         sendJsonEvent("keyReleased", new EventKey(e));
      }
   }
}
