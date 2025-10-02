package no.imr.korona.ekremote.requests.parameter;

import no.imr.korona.ekremote.requests.MessageRequest;
import no.imr.korona.ekremote.responses.parameter.ParameterServerResponse;

public interface ParameterServerRequest<T extends ParameterServerResponse> extends MessageRequest<T> {
   @Override
   default String getTargetComponent() {
      return "ParameterServer";
   }
}
