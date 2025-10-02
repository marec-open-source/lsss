package no.imr.tools.database.queries;

import org.hibernate.StatelessSession;

@FunctionalInterface
public interface StatelessValuedDatabaseQuery<T> {
   T executeAndGetValue(StatelessSession session);
}
