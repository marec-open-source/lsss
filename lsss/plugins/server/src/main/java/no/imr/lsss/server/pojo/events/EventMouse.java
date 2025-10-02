package no.imr.lsss.server.pojo.events;

import java.awt.event.MouseEvent;

public final class EventMouse {
   public int button;
   public int clickCount;
   public int modifiersEx;

   public EventMouse(MouseEvent mouseEvent) {
      button = mouseEvent.getButton();
      clickCount = mouseEvent.getClickCount();
      modifiersEx = mouseEvent.getModifiersEx();
   }

   @Override
   public String toString() {
      return "EventMouse{" +
            "button=" + button +
            ", clickCount=" + clickCount +
            ", modifiersEx=" + modifiersEx +
            '}';
   }
}
