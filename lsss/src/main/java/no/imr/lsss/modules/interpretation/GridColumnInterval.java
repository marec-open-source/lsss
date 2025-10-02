package no.imr.lsss.modules.interpretation;

import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.DoubleRange;

record GridColumnInterval(double horizontalSize, DoubleRange valueRange, PingRange pingRange) {
}
