package no.imr.korona.region;

import no.imr.korona.data.util.geometry.EchogramRectangle;
import no.imr.tools.misc.SelectionAction;

public record EchogramSelection(SelectionAction action, EchogramRectangle echogramRectangle) {
}
