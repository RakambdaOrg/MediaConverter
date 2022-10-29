package fr.rakambda.mediaconverter.storage.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converter when fetching databases.
 *
 * @param <T> The returned type.
 */
@FunctionalInterface
public interface ResultConverter<T>{
	/**
	 * Convert a fetched result to the given type.
	 *
	 * @param resultSet The result set already fetched.
	 *
	 * @return The converted type.
	 *
	 * @throws SQLException If an error occured.
	 */
	T convert(ResultSet resultSet) throws SQLException;
}
