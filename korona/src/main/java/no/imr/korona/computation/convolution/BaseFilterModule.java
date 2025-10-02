package no.imr.korona.computation.convolution;

import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.List;

/**
 * Base class for filtering and convolution modules.
 */
public abstract class BaseFilterModule extends GeneralPingModule {
   public final BooleanParameter onlyLastChannel = new BooleanParameter(
         new Name("OnlyLastChannel", "Only last channel"),
         false,
         "If selected only the last channel is processed, otherwise all channels are processed");

   public final BooleanParameter maskPelagic = new BooleanParameter(
         new Name("MaskPelagic", "Mask pelagic"),
         false,
         "Exclude samples above bottom");

   public final BooleanParameter maskBottom = new BooleanParameter(
         new Name("MaskBottom", "Mask bottom"),
         true,
         "Exclude samples below bottom");

   public final BooleanParameter maskSecondBottom = new BooleanParameter(
         new Name("MaskSecondBottom", "Mask second bottom"),
         true,
         "Exclude samples below 10m above second bottom echo");

   public final BooleanParameter maskNoise = new BooleanParameter(
         new Name("MaskNoise", "Mask noise"),
         false,
         "Exclude samples below noise threshold");

   public enum RegionMasking {
      none, inside, outside
   }

   public final ObjectParameter<RegionMasking> maskRegion = new ObjectParameter<>(
         new Name("MaskRegion", "Mask region"),
         RegionMasking.none, RegionMasking.values(),
         "Exclude samples in connection with regions");

   public final ObjectParameter<RegionMasking> maskTrack = new ObjectParameter<>(
         new Name("MaskTrack", "Mask track"),
         RegionMasking.none, RegionMasking.values(),
         "Exclude samples in connection with tracks");

   public final IntParameter minPing = new IntParameter(
         new Name("MinPing", "Min ping"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings used ≥ 2 × Min Ping + 1");

   public final IntParameter maxPing = new IntParameter(
         new Name("MaxPing", "Max ping"),
         10, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings used ≤ 2 × Max Ping + 1");

   BaseFilterModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            onlyLastChannel,
            maskPelagic,
            maskBottom,
            maskSecondBottom,
            maskNoise,
            maskRegion,
            maskTrack,
            minPing,
            maxPing
      );
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(TransducerRangesFileService.NAME);
   }
}
