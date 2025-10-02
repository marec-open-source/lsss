package no.imr.korona.resources;

import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.swing.svg.SvgImage;

import java.awt.Image;

public final class KoronaResource {
   public static final SvgIcon KORONA = SvgIcon.of("no/imr/korona/resources/images/icons/korona.svg");
   public static final Image KORONA_32 = new SvgImage("no/imr/korona/resources/images/korona/korona.svg", 32);
   public static final Image KORONA_64 = new SvgImage("no/imr/korona/resources/images/korona/korona.svg", 64);

   private KoronaResource() {
   }
}
