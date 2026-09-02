package no.imr.korona.computation.misc;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Module for generating combination echograms.
 */
public final class CombinationModule extends ConcurrentPingModule {
   final IntParameter firstOperandKHz = new IntParameter(
         new Name("FirstOperandKhz", "First operand kHz"),
         0, Unit.KHZ, ValueConstraints.gte(0),
         "Frequency of the first operand");

   final IntParameter secondOperandKHz = new IntParameter(
         new Name("SecondOperandKhz", "Second operand kHz"),
         0, Unit.KHZ, ValueConstraints.gte(0),
         "Frequency of the second operand");

   final IntParameter firstOperandChannel = new IntParameter(
         new Name("FirstOperandChannel", "First operand channel"),
         1, Unit.NONE, ValueConstraints.gte(1),
         "Channel of the first operand (used if First operand kHz = 0)");

   final IntParameter secondOperandChannel = new IntParameter(
         new Name("SecondOperandChannel", "Second operand channel"),
         2, Unit.NONE, ValueConstraints.gte(1),
         "Channel of the second operand (used if Second operand kHz = 0)");

   final BooleanParameter logarithmicOperands = new BooleanParameter(
         new Name("LogarithmicOperands", "Logarithmic operands"),
         false,
         "If checked use log(sv), otherwise use (linear) sv");

   public enum Operation {
      SUBTRACT("-", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = x.getValueForReferenceIndex(i) - y.getValueForReferenceIndex(i);
         }
      }),
      ADD("+", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = x.getValueForReferenceIndex(i) + y.getValueForReferenceIndex(i);
         }
      }),
      MULTIPLY("*", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = x.getValueForReferenceIndex(i) * y.getValueForReferenceIndex(i);
         }
      }),
      DIVIDE("/", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            float yValue = y.getValueForReferenceIndex(i);
            if (yValue == 0) {
               Log.global.warning("CombinationModule : Divide by zero in processing. Result set to 0.");
               result[i] = 0;
            } else {
               result[i] = x.getValueForReferenceIndex(i) / yValue;
            }
         }
      }),
      MEAN("mean", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = (x.getValueForReferenceIndex(i) + y.getValueForReferenceIndex(i)) / 2;
         }
      }),
      HARMONIC_MEAN("harmonic mean", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = 2 / (1 / x.getValueForReferenceIndex(i) + 1 / y.getValueForReferenceIndex(i));
         }
      }),
      MIN("min", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = Math.min(x.getValueForReferenceIndex(i), y.getValueForReferenceIndex(i));
         }
      }),
      MAX("max", (x, y, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = Math.max(x.getValueForReferenceIndex(i), y.getValueForReferenceIndex(i));
         }
      }),
      ZERO("zero", (_, _, result) -> {
         Arrays.fill(result, 0);
      }),
      COPY_1ST_OPERAND("copy1stOperand", (x, _, result) -> {
         for (int i = 0; i < result.length; i++) {
            result[i] = x.getValueForReferenceIndex(i);
         }
      });

      private final String id;
      private final Evaluator evaluator;

      Operation(String id, Evaluator evaluator) {
         this.id = id;
         this.evaluator = evaluator;
      }

      @Override
      public String toString() {
         return id;
      }

      public void eval(ResampledFloatArray x, ResampledFloatArray y, float[] result) {
         evaluator.eval(x, y, result);
      }

      @FunctionalInterface
      private interface Evaluator {
         void eval(ResampledFloatArray x, ResampledFloatArray y, float[] result);
      }
   }

   public final ObjectParameter<Operation> operation = new ObjectParameter<>(
         new Name("Operation"),
         Operation.MEAN, Operation.values(),
         "Specifies how the operands are combined");

   public CombinationModule() {
      firstOperandKHz.addListenerAndNotify(kHz -> firstOperandChannel.setEnabled(kHz == 0));
      secondOperandKHz.addListenerAndNotify(kHz -> secondOperandChannel.setEnabled(kHz == 0));
      operation.addListenerAndNotify(op -> {
         boolean canUseSv = op != Operation.SUBTRACT;
         if (!canUseSv) {
            logarithmicOperands.setBooleanValue(true);
         }
         logarithmicOperands.setEnabled(canUseSv);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            firstOperandKHz,
            secondOperandKHz,
            firstOperandChannel,
            secondOperandChannel,
            logarithmicOperands,
            operation
      );
   }

   private void updateChannelParameters(RawFileConfiguration rawFileConfiguration) {
      List<Integer> kHzs = Stream.concat(
                  Stream.of(0),
                  rawFileConfiguration.getTransducers().stream()
                        .map(RawFileTransducer::getKHz)
            )
            .toList();
      firstOperandKHz.setSuggestedValues(kHzs);
      secondOperandKHz.setSuggestedValues(kHzs);

      List<Integer> channels = IntStream.rangeClosed(1, rawFileConfiguration.getTransducerCount())
            .boxed()
            .toList();
      firstOperandChannel.setSuggestedValues(channels);
      secondOperandChannel.setSuggestedValues(channels);
   }

   int getChannel(IntParameter kHzParameter, IntParameter channelParameter, RawFileConfiguration rawFileConfiguration) throws ModuleConfigurationException {
      int channel;
      int kHz = kHzParameter.getIntValue();
      if (kHz > 0) {
         channel = rawFileConfiguration.lastChannelWithKHz(kHz);
         if (channel <= 0) {
            throw new ModuleConfigurationException(this, "Frequency " + kHz + " kHz is not found in the data");
         }
      } else {
         channel = channelParameter.getIntValue();
         if (channel > rawFileConfiguration.getTransducerCount()) {
            throw new ModuleConfigurationException(this, "Channel " + channel + " is not found in the data");
         }
      }
      return channel;
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws ModuleConfigurationException {
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      updateChannelParameters(pingConfiguration.getRawFileConfiguration());
      return new CombinationModuleComputation(this, computationContext, pingSource);
   }
}
