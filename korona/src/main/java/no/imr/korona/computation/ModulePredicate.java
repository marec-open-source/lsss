package no.imr.korona.computation;

import java.util.function.Predicate;

@FunctionalInterface
public interface ModulePredicate extends Predicate<ModuleInfo> {
   @Override
   boolean test(ModuleInfo moduleInfo);

   static ModulePredicate alwaysTrue() {
      return _ -> true;
   }
}
