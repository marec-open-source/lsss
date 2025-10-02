package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.korona.data.ping.Ping;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;

public interface PerChannelComputer {
   void accumulate(Ping ping, double pingWidthMeters, int channel, List<FloatRange> depthRanges);

   Map<String, Float> getValues();
}
