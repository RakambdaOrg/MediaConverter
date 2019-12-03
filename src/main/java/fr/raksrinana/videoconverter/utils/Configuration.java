package fr.raksrinana.videoconverter.utils;

import fr.raksrinana.utils.config.PreparedStatementFiller;
import fr.raksrinana.utils.config.SQLValue;
import fr.raksrinana.utils.config.SQLiteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Configuration extends SQLiteManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	private final Map<String, Boolean> useless = new ConcurrentHashMap<>();
	
	public Configuration(final Path dbFile) throws ClassNotFoundException, InterruptedException, ExecutionException, TimeoutException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Useless(Filee VARCHAR(512) NOT NULL, PRIMARY KEY(Filee));").get(30, TimeUnit.SECONDS);
	}
	
	public boolean isUseless(final Path path) throws TimeoutException, ExecutionException, InterruptedException{
		if(useless.isEmpty()){
			sendQueryRequest("SELECT * FROM Useless;").thenAccept(resultSet -> resultSet.ifPresent(result -> {
				try{
					while(result.next()){
						useless.put(result.getString("Filee"), true);
					}
				}
				catch(SQLException e){
					LOGGER.error("Error getting useless status", e);
				}
			})).get(30, TimeUnit.SECONDS);
		}
		return useless.containsKey(path.toString().replace("\\", "/"));
	}
	
	@Override
	public void close(){
		LOGGER.info("Closing SQL Connection...");
		super.close();
	}
	
	public void setUseless(final Path path) throws InterruptedException, TimeoutException, ExecutionException{
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Useless(Filee) VALUES(?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString().replace("\\", "/")))).get(30, TimeUnit.SECONDS);
	}
	
	void setUseless(final Collection<Path> paths) throws InterruptedException, TimeoutException, ExecutionException{
		final var placeHolders = IntStream.range(0, paths.size()).mapToObj(o -> "(?)").collect(Collectors.joining(","));
		final var values = paths.stream().map(path -> path.toString().replace("\\", "/")).flatMap(path -> List.of(new SQLValue(SQLValue.Type.STRING, path)).stream()).toArray(SQLValue[]::new);
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Useless(Filee) VALUES " + placeHolders + ";", new PreparedStatementFiller(values)).get(30, TimeUnit.SECONDS);
		LOGGER.debug("Set downloaded status for {} items", paths.size());
	}
}
