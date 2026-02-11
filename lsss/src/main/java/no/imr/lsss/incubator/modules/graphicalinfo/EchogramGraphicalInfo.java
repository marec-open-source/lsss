package no.imr.lsss.incubator.modules.graphicalinfo;

import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;

import java.awt.Rectangle;

interface EchogramGraphicalInfo {
   RenderedInfo render(EchogramPingSettings pingSettings, EchogramZSettings zSettings, Rectangle bounds);
}
