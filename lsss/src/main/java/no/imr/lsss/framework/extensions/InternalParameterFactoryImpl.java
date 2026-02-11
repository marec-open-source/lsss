package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.internal.InternalParameterFactory;
import no.marec.lsss.api.util.parameters.ConfigParameter;
import no.marec.lsss.api.util.parameters.VoidConfigParameter;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class InternalParameterFactoryImpl implements InternalParameterFactory {
   static final InternalParameterFactoryImpl INSTANCE = new InternalParameterFactoryImpl();

   private InternalParameterFactoryImpl() {
   }

   @Override
   public ConfigParameter<Boolean> booleanParameter(String id, String label, boolean initialValue, String description) {
      return new BooleanParameter(new Name(id, label), initialValue, "");
   }

   @Override
   public ConfigParameter<Optional<Integer>> optionalIntParameter(String id, String label, Optional<Integer> initialValue, String unit, String description) {
      return new OptionalIntParameter(new Name(id, label), initialValue, new Unit(unit), description);
   }

   @Override
   public ConfigParameter<Integer> intParameter(String id, String label, int initialValue, String unit, String description) {
      return new IntParameter(new Name(id, label), initialValue, new Unit(unit), description);
   }

   @Override
   public ConfigParameter<Float> floatParameter(String id, String label, float initialValue, String unit, String description) {
      return new FloatParameter(new Name(id, label), initialValue, new Unit(unit), description);
   }

   @Override
   public ConfigParameter<Optional<Float>> optionalFloatParameter(String id, String label, Optional<Float> initialValue, String unit, String description) {
      return new OptionalFloatParameter(new Name(id, label), initialValue, new Unit(unit), description);
   }

   @Override
   public ConfigParameter<String> stringParameter(String id, String label, String initialValue, String description) {
      return new StringParameter(new Name(id, label), initialValue, description);
   }

   @Override
   public <T> ConfigParameter<T> selectionParameter(String id, String label, T initialValue, List<T> allowedValues, String description) {
      return new ObjectParameter<>(new Name(id, label), initialValue, allowedValues, description);
   }

   @Override
   public VoidConfigParameter button(String id, String label, String description) {
      return new ButtonParameter(new Name(id, label), description);
   }

   @Override
   public VoidConfigParameter header(String text) {
      return new HeaderParameter(text);
   }

   @Override
   public VoidConfigParameter lineSeparator() {
      return SeparatorParameter.line();
   }

   @Override
   public VoidConfigParameter spaceSeparator() {
      return SeparatorParameter.space();
   }

   @Override
   public ConfigParameter<Optional<Path>> surveyDirectoryParameter(LsssAccess lsssAccess, String dirId, String path) {
      LSSS lsss = ((LsssAccessImpl) lsssAccess).getLSSS();
      return new SurveyDirectoryParameter("Select directory for " + dirId,
            new SubDir(new Name("Main" + dirId), new Name(dirId), path), lsss);
   }
}
