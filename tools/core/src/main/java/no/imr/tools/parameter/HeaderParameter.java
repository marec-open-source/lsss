package no.imr.tools.parameter;

import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * A "parameter" for adding a header in the gui.
 */
public final class HeaderParameter extends VoidParameter {
   private String htmlContent = "";

   public HeaderParameter(String text) {
      super(new Name("NoName", text), Unit.NONE, "");
   }

   public HeaderParameter(String text, @Nullable SvgIcon icon) {
      this(text);

      if (icon != null) {
         setProperty(KEY_ICON, Optional.of(icon));
      }
   }

   public String getHtmlContent() {
      return htmlContent;
   }

   public void setHtmlContent(String htmlContent) {
      this.htmlContent = htmlContent;
   }
}
