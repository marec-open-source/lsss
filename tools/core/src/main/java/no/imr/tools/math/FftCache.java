package no.imr.tools.math;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.tools.concurrent.Exec;
import org.jtransforms.fft.DoubleFFT_1D;
import pl.edu.icm.jlargearrays.ConcurrencyUtils;

import java.util.concurrent.TimeUnit;

public final class FftCache {
   static {
      ConcurrencyUtils.setThreadPool(Exec.CACHED_THREAD_POOL);
      ConcurrencyUtils.setNumberOfThreads(1);
   }

   private static final LoadingCache<Integer, DoubleFFT_1D> DOUBLE_1D = CacheBuilder.newBuilder()
         .maximumSize(100)
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(new CacheLoader<>() {
            @Override
            public DoubleFFT_1D load(Integer size) {
               return new DoubleFFT_1D(size);
            }
         });

   private FftCache() {
   }

   public static DoubleFFT_1D getDouble1D(int size) {
      return DOUBLE_1D.getUnchecked(size);
   }
}
