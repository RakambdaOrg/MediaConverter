package fr.rakambda.mediaconverter.storage.sql;

import org.jspecify.annotations.NonNull;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLValue{
	private final Object value;
	private final Type type;
	
	/**
	 * A type that the value can take.
	 */
	public enum Type{
		INT, STRING, LONG, DOUBLE
	}
	
	/**
	 * Constructor.
	 *
	 * @param type  The type of the value.
	 * @param value The value.
	 */
	public SQLValue(@NonNull Type type, @NonNull Object value){
		this.type = type;
		this.value = value;
	}
	
	/**
	 * Fill a prepared statement.
	 *
	 * @param index     The index of the value.
	 * @param statement The statement to fill.
	 *
	 * @throws SQLException If the statement couldn't be filled.
	 */
	public void fill(int index, @NonNull PreparedStatement statement) throws SQLException{
		switch(type){
			case INT -> statement.setInt(index, (Integer) value);
			case LONG -> statement.setLong(index, (Long) value);
			case DOUBLE -> statement.setDouble(index, (Double) value);
			default -> statement.setString(index, value.toString());
		}
	}
	
	@Override
	public String toString(){
		return type + " -> " + value;
	}
}
