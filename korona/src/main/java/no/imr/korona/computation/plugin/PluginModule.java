package no.imr.korona.computation.plugin;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.marec.api.korona.ModuleComputation;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class PluginModule extends GeneralPingModule {
   public final ObjectParameter<Optional<Example>> examples = new ObjectParameter<>(
         new Name("Example"),
         Optional.empty(), Stream.concat(Stream.of(Optional.<Example>empty()), Example.EXAMPLES.stream().map(Optional::of)).toList()) {
      @Override
      public String toString(Optional<Example> value) {
         return value.map(Example::toString).orElse("");
      }
   };

   public final TextParameter implementation = new TextParameter(
         new Name("Implementation"),
         "", PluginModule::getCompileError);

   public PluginModule() {
      examples.setPersistable(false);
      examples.subscribe(optExample -> {
         optExample.ifPresent(example -> {
            implementation.setValue(example.getImplementation());
         });
      });

      implementation.setProperty(BaseParameter.KEY_VERTICAL_FILL, true);
      implementation.setProperty(BaseParameter.KEY_MONOSPACED, true);
      implementation.subscribe(impl -> {
         examples.getValue().ifPresent(example -> {
            if (!example.getImplementation().equals(impl)) {
               examples.setValue(Optional.empty());
            }
         });
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            examples,
            implementation
      );
   }

   @Override
   public boolean handleUnknownXml(String name, Element subElement) {
      if (name.equals("Description")) {
         comment.setValue(subElement.getText());
         return true;
      }
      return false;
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) {
      return new PluginModuleComputation(this, computationContext, pingSource);
   }

   @Override
   public void customizeGUIConfig(GUIConfig guiConfig) {
      guiConfig.setHorizontalFill(true);
      guiConfig.setCombineInputAndDescription(true);
   }

   static ModuleComputation compile(String implementation) throws CompileException {
      if (implementation.isEmpty()) {
         return new EmptyPluginComputation();
      }
      Matcher packageMatcher = Pattern.compile("package\\s+((\\w|\\.|\\s)+)").matcher(implementation);
      String packageName = packageMatcher.find() ? packageMatcher.group(1).replaceAll("\\s", "") : "";
      Matcher classNameMatcher = Pattern.compile("class\\s+(\\w+).*(implements|extends)").matcher(implementation);
      if (!classNameMatcher.find()) {
         throw new CompileException("Could not find class name in implementation");
      }
      String className = packageName + (packageName.isEmpty() ? "" : ".") + classNameMatcher.group(1);
      return CompilerClassLoader.instantiate(ModuleComputation.class, className, implementation);
   }

   private static @Nullable String getCompileError(String implementation) {
      try {
         compile(implementation);
      } catch (CompileException e) {
         return e.getMessage();
      }
      return null;
   }

   @Override
   public void runSmokeTest() throws CompileException {
      new PluginModuleSmoke().run();
   }
}
