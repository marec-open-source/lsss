package no.imr.korona.data.datamanager;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingData;

record LoadedPing(Ping ping, PingData pingData) {
}
