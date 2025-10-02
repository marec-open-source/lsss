package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

public final class RotatedLabel extends JLabel {
   private boolean painting;

   public RotatedLabel() {
   }

   public RotatedLabel(String text) {
      super(text);
   }

   @Override
   public @Nullable Dimension getPreferredSize() {
      return flip(super.getPreferredSize());
   }

   @Override
   public @Nullable Dimension getMinimumSize() {
      return flip(super.getMinimumSize());
   }

   @Override
   public @Nullable Dimension getMaximumSize() {
      return flip(super.getMaximumSize());
   }

   private static @Nullable Dimension flip(@Nullable Dimension size) {
      //noinspection SuspiciousNameCombination
      return size != null ? new Dimension(size.height, size.width) : null;
   }

   @Override
   public int getWidth() {
      return painting ? super.getHeight() : super.getWidth();
   }

   @Override
   public int getHeight() {
      return painting ? super.getWidth() : super.getHeight();
   }

   @Override
   public void paint(Graphics g) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.translate(0, getHeight());
      g2d.rotate(Math.toRadians(-90));
      painting = true;
      super.paint(g);
      painting = false;
   }
}
