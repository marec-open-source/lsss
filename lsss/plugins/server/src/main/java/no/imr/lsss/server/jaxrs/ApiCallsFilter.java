package no.imr.lsss.server.jaxrs;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import no.imr.lsss.server.pojo.ApiCall;
import no.imr.tools.Utils;
import no.imr.tools.io.TeeInputStream;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

@PreMatching
@Provider
public final class ApiCallsFilter implements ContainerRequestFilter, ContainerResponseFilter {
   private static final String PROPERTY_KEY = "no.marec.lsss.ApiCall";
   private static final AtomicInteger COUNTER = new AtomicInteger();

   private final JaxRsApplication jaxRsApplication;

   @Inject
   public ApiCallsFilter(JaxRsApplication jaxRsApplication) {
      this.jaxRsApplication = jaxRsApplication;
   }

   @Override
   public void filter(ContainerRequestContext requestContext) {
      String url = requestContext.getUriInfo().getRequestUri().toString();
      String method = requestContext.getRequest().getMethod();
      ApiCall apiCall = new ApiCall(COUNTER.incrementAndGet(), method, url);
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      requestContext.setEntityStream(new TeeInputStream(requestContext.getEntityStream(), byteArrayOutputStream));
      requestContext.setProperty(PROPERTY_KEY, new ApiCallWrapper(apiCall, byteArrayOutputStream));
   }

   @Override
   public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
      ApiCallWrapper apiCallWrapper = (ApiCallWrapper) requestContext.getProperty(PROPERTY_KEY);
      if (apiCallWrapper == null) {
         return;
      }
      ApiCall apiCall = apiCallWrapper.apiCall;
      apiCall.requestBody = apiCallWrapper.byteArrayOutputStream.toString(Utils.UTF_8);
      apiCall.status = responseContext.getStatus();
      jaxRsApplication.getApiCallChangeManager().notifyListeners(apiCall);
   }

   private record ApiCallWrapper(ApiCall apiCall, ByteArrayOutputStream byteArrayOutputStream) {
   }
}
