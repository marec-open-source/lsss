package no.imr.lsss.framework.backup;

import no.imr.tools.parameter.Name;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record BackupExclusionOption(
      Name name,
      Supplier<Predicate<Path>> excludePredicateSupplier
) {
}
