package no.imr.tools.math;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.tools.concurrent.Exec;
import org.jtransforms.fft.DoubleFFT_1D;
import org.visnow.jlargearrays.ConcurrencyUtils;

import java.util.concurrent.TimeUnit;

public final class FftCache {
   static {
      ConcurrencyUtils.setThreadPool(Exec.CACHED_THREAD_POOL);
      ConcurrencyUtils.setNumberOfThreads(1);
   }

   private static final LoadingCache<Long, DoubleFFT_1D> DOUBLE_1D = CacheBuilder.newBuilder()
         .maximumSize(100)
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(CacheLoader.from(DoubleFFT_1D::new));

   private FftCache() {
   }

   public static DoubleFFT_1D getDouble1D(int size) {
      return DOUBLE_1D.getUnchecked((long) size);
   }
}
