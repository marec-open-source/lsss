package no.imr.tools.database.hibernate;

import java.io.Serializable;

/**
 * Base class for all database objects corresponding to a row in a database table.
 */
public interface BaseDatabaseObject extends Serializable {
   Object primaryKey();
}
