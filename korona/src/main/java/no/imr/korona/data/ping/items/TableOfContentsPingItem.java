package no.imr.korona.data.ping.items;

import java.time.Instant;
import java.util.List;

public interface TableOfContentsPingItem extends PingItem {
   List<Instant> getInstants();
}
