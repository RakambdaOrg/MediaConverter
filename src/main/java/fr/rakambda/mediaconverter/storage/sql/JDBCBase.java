package fr.rakambda.mediaconverter.storage.sql;

import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class JDBCBase implements AutoCloseable{
	private final String NAME;
	private final ArrayList<CompletableFuture<?>> futures;
	
	/**
	 * Constructor.
	 *
	 * @param name Name of the database.
	 */
	protected JDBCBase(@NonNull String name){
		NAME = name;
		futures = new ArrayList<>();
	}
	
	/**
	 * Sends a query.
	 *
	 * @param query  The query.
	 * @param <T>    The returned type.
	 * @param parser The parser of the result.
	 *
	 * @return The result.
	 *
	 * @throws SQLException If the request couldn't be made.
	 */
	public <T> List<T> sendQueryRequest(@NonNull String query, @NonNull ResultConverter<T> parser) throws SQLException{
		ResultSet result;
		try(var connection = getDatasource().getConnection()){
			log.debug("Sending SQL request to {}: {}", NAME, query);
			try(var stmt = connection.createStatement()){
				result = stmt.executeQuery(query.replace(";", ""));
				var list = new LinkedList<T>();
				while(result.next()){
					list.add(parser.convert(result));
				}
				return list;
			}
		}
	}
	
	/**
	 * Initialize the database connection.
	 */
	protected abstract HikariDataSource getDatasource();
	
	/**
	 * Sends an update.
	 *
	 * @param query The query.
	 *
	 * @return The number of lines modified.
	 *
	 * @throws SQLException If the request couldn't be made.
	 */
	public int sendUpdateRequest(@NonNull String query) throws SQLException{
		int result = 0;
		try(var connection = getDatasource().getConnection()){
			log.debug("Sending SQL update to {}: {}", NAME, query);
			for(String req : query.split(";")){
				try(var stmt = connection.createStatement()){
					result += stmt.executeUpdate(req);
				}
			}
		}
		return result;
	}
	
	/**
	 * Close the connection.
	 */
	public void close(){
		for(CompletableFuture<?> future : futures){
			future.cancel(true);
		}
		getDatasource().close();
		log.info("SQL connection closed");
	}
	
	/**
	 * Sends a batch prepared update.
	 *
	 * @param request The prepared request.
	 * @param fillers The fillers of the requests.
	 *
	 * @return The number of lines modified.
	 *
	 * @throws SQLException If a request couldn't be made.
	 */
	public int sendPreparedBatchUpdateRequest(@NonNull String request, @NonNull Collection<PreparedStatementFiller> fillers) throws SQLException{
		int result = 0;
		try(var connection = getDatasource().getConnection()){
			log.debug("Sending SQL update to {}: {}\nWith fillers {}", NAME, request, fillers);
			try(var preparedStatement = connection.prepareStatement(request.replace(";", ""))){
				for(var filler : fillers){
					filler.fill(preparedStatement);
					preparedStatement.addBatch();
				}
				result += Arrays.stream(preparedStatement.executeBatch()).sum();
			}
		}
		return result;
	}
}
