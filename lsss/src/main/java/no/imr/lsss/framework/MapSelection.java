package no.imr.lsss.framework;

import no.imr.tools.misc.SelectionAction;

import java.awt.geom.Rectangle2D;

public record MapSelection(SelectionAction action, Rectangle2D geoRect) {
}
