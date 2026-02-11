package no.imr.lsss.server.pojo.events;

import tools.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public final class EventEmpty {
   public EventEmpty() {
   }

   @Override
   public String toString() {
      return "EventEmpty{}";
   }
}
