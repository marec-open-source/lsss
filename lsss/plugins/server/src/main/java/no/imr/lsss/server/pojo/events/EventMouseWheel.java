package no.imr.lsss.server.pojo.events;

import java.awt.event.MouseWheelEvent;

public final class EventMouseWheel {
   public int wheelRotation;
   public int modifiersEx;

   public EventMouseWheel(MouseWheelEvent mouseWheelEvent) {
      wheelRotation = mouseWheelEvent.getWheelRotation();
      modifiersEx = mouseWheelEvent.getModifiersEx();
   }

   @Override
   public String toString() {
      return "EventMouseWheel{" +
            "wheelRotation=" + wheelRotation +
            ", modifiersEx=" + modifiersEx +
            '}';
   }
}
