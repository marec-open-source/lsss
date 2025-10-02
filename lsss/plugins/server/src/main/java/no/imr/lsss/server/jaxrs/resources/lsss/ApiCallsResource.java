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
import no.imr.lsss.server.jaxrs.JaxRsApplication;
import no.imr.tools.logging.Log;

import java.util.logging.Level;

@Singleton
public final class ApiCallsResource {
   private final Sse sse;
   private final SseBroadcaster sseBroadcaster;

   @Inject
   ApiCallsResource(JaxRsApplication jaxRsApplication, Sse sse) {
      jaxRsApplication.addOnClose(this::close);

      this.sse = sse;
      sseBroadcaster = sse.newBroadcaster();
      sseBroadcaster.onError(ApiCallsResource::onEventSinkError);

      jaxRsApplication.getApiCallChangeManager().addListener(this::sendJsonEvent);
   }

   @GET
   @Produces(MediaType.SERVER_SENT_EVENTS)
   public void getServerSentEvents(@Context SseEventSink eventSink) {
      sseBroadcaster.register(eventSink);
   }

   private static void onEventSinkError(SseEventSink eventSink, Throwable throwable) {
      if (eventSink.isClosed()) {
         return;
      }
      Log.global.log(Level.INFO, "Event sink error", throwable);
   }

   private void sendJsonEvent(Object data) {
      OutboundSseEvent event = sse.newEventBuilder()
            .name("api-call")
            .mediaType(MediaType.APPLICATION_JSON_TYPE)
            .data(data)
            .build();
      sseBroadcaster.broadcast(event);
   }

   private void close() {
      sseBroadcaster.close();
   }
}
