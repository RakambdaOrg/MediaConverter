package fr.rakambda.mediaconverter.storage.sql;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;

@Slf4j
public class PreparedStatementFiller{
	private final HashMap<Integer, SQLValue> values;
	
	/**
	 * Constructor.
	 *
	 * @param sqlValues The values to fill.
	 */
	public PreparedStatementFiller(@NonNull SQLValue... sqlValues){
		values = new HashMap<>();
		for(int i = 0; i < sqlValues.length; i++){
			values.put(i + 1, sqlValues[i]);
		}
	}
	
	/**
	 * Called to fill a prepared statement.
	 *
	 * @param statement The statement to fill.
	 */
	public void fill(@NonNull PreparedStatement statement){
		for(int i : values.keySet()){
			try{
				values.get(i).fill(i, statement);
			}
			catch(SQLException e){
				log.warn("Error filling prepared statement with parameter: {} -> {}", i, values.get(i));
			}
		}
	}
	
	@Override
	public String toString(){
		return values.toString();
	}
}
