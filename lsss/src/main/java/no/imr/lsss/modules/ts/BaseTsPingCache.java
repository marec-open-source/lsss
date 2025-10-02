package no.imr.lsss.modules.ts;

import java.util.List;

public interface BaseTsPingCache {
   List<? extends BaseTsData> getTsData(int channel);
}
