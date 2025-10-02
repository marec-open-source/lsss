package no.imr.korona.computation.broadband;

import no.imr.korona.data.datagrams.Fil0Datagram;
import no.imr.korona.data.datagrams.Fil1Datagram;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.tools.Utils;
import no.imr.tools.math.ComplexArray;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class PulseCompressionFilterChain {
   public static final PulseCompressionFilterChain EMPTY = new PulseCompressionFilterChain(List.of());

   private final List<PulseCompressionFilter> filters;
   private final int totalDecimationFactor;

   public PulseCompressionFilterChain(List<PulseCompressionFilter> filters) {
      this.filters = filters;
      totalDecimationFactor = filters.stream()
            .mapToInt(PulseCompressionFilter::decimationFactor)
            .reduce(1, (a, b) -> a * b);
   }

   public static Function<String, PulseCompressionFilterChain> makeChannelIdToFilterChain(List<PingItem> configurationItems) {
      Map<String, List<PulseCompressionFilter>> channelIdToUnsortedFilters = new HashMap<>();
      for (PingItem item : configurationItems) {
         switch (item) {
            case Fil0Datagram fil0Datagram -> {
               PulseCompressionFilterChain filterChain = new PulseCompressionFilterChain(List.of(new PulseCompressionFilter(0, 8, calculateB1(fil0Datagram))));
               return __ -> filterChain;
            }
            case Fil1Datagram fil1Datagram -> {
               channelIdToUnsortedFilters.computeIfAbsent(fil1Datagram.channelId, k -> new ArrayList<>())
                     .add(new PulseCompressionFilter(fil1Datagram.stage, fil1Datagram.decimationFactor, fil1Datagram.coefficients));
            }
            default -> {
            }
         }
      }
      return channelId -> {
         List<PulseCompressionFilter> unsortedFilters = channelIdToUnsortedFilters.get(channelId);
         if (unsortedFilters == null) {
            return EMPTY;
         }
         List<PulseCompressionFilter> sortedFilters = unsortedFilters.stream()
               .filter(Utils.distinctBy(PulseCompressionFilter::stage))
               .sorted(Comparator.comparingInt(PulseCompressionFilter::stage))
               .toList();
         return new PulseCompressionFilterChain(sortedFilters);
      };
   }

   public List<PulseCompressionFilter> getFilters() {
      return filters;
   }

   public int getTotalDecimationFactor() {
      return totalDecimationFactor;
   }

   public ComplexArray apply(double[] signal) {
      ComplexArray result = ComplexArray.ofReal(signal);
      for (PulseCompressionFilter filter : filters) {
         result = filter.apply(result);
      }
      return result;
   }

   private static ComplexArray calculateB1(Fil0Datagram fil0Datagram) {
      ComplexArray filter = fil0Datagram.getFilter();
      double g = fil0Datagram.getG();
      ComplexArray b1 = ComplexArray.ofLength(2 * filter.length() - 1);
      int centerIndex = filter.length() - 1;
      for (int i = 0; i < filter.length(); i++) {
         b1.set(centerIndex - i, filter.re(i) / g, -filter.im(i) / g); // flipped and conjugated
      }
      for (int i = 1; i < filter.length(); i++) {
         b1.set(centerIndex + i, filter.re(i) / g, filter.im(i) / g);
      }
      return b1;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PulseCompressionFilterChain that
            && filters.equals(that.filters);
   }

   @Override
   public int hashCode() {
      return filters.hashCode();
   }
}
