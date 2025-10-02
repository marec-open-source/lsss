package no.imr.lsss.modules.broadband.sv;

import no.imr.tools.range.FloatRange;

public record BroadbandSvData(FloatRange depthRange, float[] sv) {
}
