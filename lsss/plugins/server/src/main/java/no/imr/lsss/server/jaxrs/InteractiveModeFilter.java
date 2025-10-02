package no.imr.lsss.server.jaxrs;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import no.imr.lsss.framework.InterpretationSettings;

@PreMatching
@Provider
public final class InteractiveModeFilter implements ContainerRequestFilter, ContainerResponseFilter {
   private final InterpretationSettings interpretationSettings;
   private final Object lock = new Object();
   private int activeRequestCounter;

   @Inject
   public InteractiveModeFilter(JaxRsApplication jaxRsApplication) {
      interpretationSettings = jaxRsApplication.getLSSS().getInterpretationSettings();
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {
      synchronized (lock) {
         if (activeRequestCounter == 0) {
            interpretationSettings.setInteractiveMode(false);
         }
         activeRequestCounter++;
      }
   }

   @Override
   public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
      synchronized (lock) {
         activeRequestCounter--;
         if (activeRequestCounter == 0) {
            interpretationSettings.setInteractiveMode(true);
         }
      }
   }
}
