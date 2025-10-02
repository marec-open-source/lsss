package no.marec.lsss.api.internal;

import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.util.parameters.ConfigParameter;
import no.marec.lsss.api.util.parameters.VoidConfigParameter;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * This is an implementation detail and is not part of the LSSS API.
 */
public interface InternalParameterFactory {

   ConfigParameter<Boolean> booleanParameter(String id, String label, boolean initialValue, String description);

   ConfigParameter<Integer> intParameter(String id, String label, int initialValue, String unit, String description);

   ConfigParameter<Optional<Integer>> optionalIntParameter(String id, String label, Optional<Integer> initialValue, String unit, String description);

   ConfigParameter<Float> floatParameter(String id, String label, float initialValue, String unit, String description);

   ConfigParameter<Optional<Float>> optionalFloatParameter(String id, String label, Optional<Float> initialValue, String unit, String description);

   ConfigParameter<String> stringParameter(String id, String label, String initialValue, String description);

   <T> ConfigParameter<T> selectionParameter(String id, String label, T initialValue, List<T> allowedValues, String description);

   VoidConfigParameter button(String id, String label, String description);

   VoidConfigParameter header(String text);

   VoidConfigParameter lineSeparator();

   VoidConfigParameter spaceSeparator();

   ConfigParameter<Optional<Path>> surveyDirectoryParameter(LsssAccess lsssAccess, String dirId, String path);
}
