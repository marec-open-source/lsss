package no.imr.tools.swing.svg;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import java.awt.Image;

public final class SvgIcon {
   private final SvgImage svgImage;
   private @Nullable ImageIcon icon;
   private @Nullable ImageIcon disabledIcon;

   private SvgIcon(String resource) {
      this(new SvgImage(resource));
   }

   public SvgIcon(SvgImage svgImage) {
      this.svgImage = svgImage;
   }

   public static SvgIcon of(String resource) {
      return new SvgIcon(resource);
   }

   public <T extends AbstractButton> T on(T button) {
      button.setIcon(getIcon());
      button.setDisabledIcon(getDisabledIcon());
      return button;
   }

   public <T extends JLabel> T on(T label) {
      label.setIcon(getIcon());
      label.setDisabledIcon(getDisabledIcon());
      return label;
   }

   public static void remove(JLabel label) {
      label.setIcon(null);
      label.setDisabledIcon(null);
   }

   public Image getImage() {
      return svgImage;
   }

   private ImageIcon getIcon() {
      ImageIcon icon = this.icon;
      if (icon == null) {
         icon = new ImageIcon(svgImage);
         this.icon = icon;
      }
      return icon;
   }

   private ImageIcon getDisabledIcon() {
      ImageIcon disabledIcon = this.disabledIcon;
      if (disabledIcon == null) {
         disabledIcon = new ImageIcon(new SvgDisabledImage(svgImage));
         this.disabledIcon = disabledIcon;
      }
      return disabledIcon;
   }

   public SvgIcon withSize(int size) {
      return new SvgIcon(svgImage.withSize(size));
   }
}
