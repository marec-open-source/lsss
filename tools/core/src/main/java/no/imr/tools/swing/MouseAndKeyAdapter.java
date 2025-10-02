package no.imr.tools.swing;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;

/**
 * Abstract adapter class for all mouse and key input interfaces.
 */
public abstract class MouseAndKeyAdapter extends MouseAdapter implements FocusListener, KeyListener {
   protected MouseAndKeyAdapter() {
   }

   @Override
   public void focusGained(FocusEvent e) {
   }

   @Override
   public void focusLost(FocusEvent e) {
   }

   @Override
   public void keyTyped(KeyEvent e) {
   }

   @Override
   public void keyPressed(KeyEvent e) {
   }

   @Override
   public void keyReleased(KeyEvent e) {
   }
}
