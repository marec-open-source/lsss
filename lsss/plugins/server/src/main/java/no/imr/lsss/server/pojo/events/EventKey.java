package no.imr.lsss.server.pojo.events;

import java.awt.event.KeyEvent;

public final class EventKey {
   public int keyCode;
   public char keyChar;
   public int modifiersEx;

   public EventKey(KeyEvent keyEvent) {
      keyCode = keyEvent.getKeyCode();
      keyChar = keyEvent.getKeyChar();
      modifiersEx = keyEvent.getModifiersEx();
   }

   @Override
   public String toString() {
      return "EventKey{" +
            "keyCode=" + keyCode +
            ", keyChar=" + keyChar +
            ", modifiersEx=" + modifiersEx +
            '}';
   }
}
