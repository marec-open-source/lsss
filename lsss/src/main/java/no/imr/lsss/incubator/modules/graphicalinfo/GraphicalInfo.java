package no.imr.lsss.incubator.modules.graphicalinfo;

import no.imr.korona.data.ping.PingRange;

import java.util.List;

record GraphicalInfo(PingRange pingRange, List<EchogramGraphicalInfo> echogramGraphicalInfos) {
}
