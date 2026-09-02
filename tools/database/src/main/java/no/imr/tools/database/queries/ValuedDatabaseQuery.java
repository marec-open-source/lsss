package no.imr.tools.database.queries;

import org.hibernate.Session;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ValuedDatabaseQuery<T extends @Nullable Object> {
   T executeAndGetValue(Session session);
}
