package no.imr.korona.computation.expression;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Utils;
import no.imr.tools.compile.CompileException;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalStringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverters;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import org.dom4j.Element;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Creates a new channel defined as an expression of the existing channels.
 */
public final class ExpressionModule extends ConcurrentPingModule {
   private static final ValueConstraint<String> EXPRESSION_CONSTRAINT = value -> {
      try {
         new ExpressionApplier(value, Utils.EMPTY_INT_ARRAY, 1);
         return null;
      } catch (CompileException e) {
         return e.getMessage();
      }
   };

   public final DynamicListParameter<String> expressions = new DynamicListParameter<>(
         new Name("Expression"),
         List.of("C1"), Unit.NONE, EXPRESSION_CONSTRAINT, ValueConverters.STRING) {
      @Override
      public OptionalStringParameter createNewParameter(int index, String persistentName) {
         return new OptionalStringParameter(new Name(persistentName, "Expression"),
               Optional.empty(), EXPRESSION_CONSTRAINT,
               index == 0 ? "Use C1, C2, ..., or F18, F38, ..." : "Alternative expression");
      }
   };

   public ExpressionModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            expressions
      );
   }

   @Override
   public boolean handleUnknownXml(String name, Element subElement) {
      if (name.startsWith("Expression")) {
         expressions.setValue(ImmutableUtils.add(expressions.getValue(), subElement.getText()));
         return true;
      }
      return false;
   }

   @Override
   public ConcurrentPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new ExpressionModuleComputation(this, computationContext, pingSource);
   }

   @Override
   public void runSmokeTest() throws CompileException {
      new ExpressionApplierSmoke().run();
   }
}
