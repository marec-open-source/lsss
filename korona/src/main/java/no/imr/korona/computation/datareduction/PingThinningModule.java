package no.imr.korona.computation.datareduction;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public final class PingThinningModule extends GeneralPingModule {
   public final IntParameter pingsToSkipInitially = new IntParameter(
         new Name("PingsToSkipInitially", "Initial skip"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings to skip initially");

   public final IntParameter pingsToSkipPeriodically = new IntParameter(
         new Name("PingsToSkip", "Periodical skip"),
         0, Unit.COUNT, ValueConstraints.gte(0),
         "Number of pings to skip periodically");

   public PingThinningModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            pingsToSkipInitially,
            pingsToSkipPeriodically
      );
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new PingThinningModuleComputation(this, computationContext, pingSource);
   }

   private static final class PingThinningModuleComputation extends GeneralPingModuleComputation {
      private final PingThinningModule module;

      // Start all files with ping number 1 to avoid black missing segments when viewing multiple files in LSSS.
      private long pingNumber = 1;
      private boolean firstTime = true;

      private PingThinningModuleComputation(PingThinningModule module, ComputationContext computationContext, PingSource pingSource) {
         super(module, computationContext, pingSource);

         this.module = module;
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         if (firstTime) {
            firstTime = false;
            return skipAndGetOnePing(module.pingsToSkipInitially.getIntValue());
         }
         return skipAndGetOnePing(module.pingsToSkipPeriodically.getIntValue());
      }

      private @Nullable Ping skipAndGetOnePing(int pingsToSkip) throws IOException {
         for (int i = 0; i < pingsToSkip; i++) {
            Ping skippedPing = inputPing();
            if (skippedPing == null) {
               return null;
            }
         }

         Ping ping = inputPing();
         if (ping == null) {
            return null;
         }

         DefaultPingIndex newPingIndex = new DefaultPingIndex(ping.getPingIndex());
         newPingIndex.setPingNumber(pingNumber++);
         DefaultPing newPing = new DefaultPing(ping.getPingConfiguration(), newPingIndex, ping.getBot0Datagram());
         newPing.addAll(ping.getPingItems());
         return newPing;
      }
   }
}
