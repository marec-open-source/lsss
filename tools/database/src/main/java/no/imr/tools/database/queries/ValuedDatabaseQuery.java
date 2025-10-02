package no.imr.tools.database.queries;

import org.hibernate.Session;

@FunctionalInterface
public interface ValuedDatabaseQuery<T> {
   T executeAndGetValue(Session session);
}
