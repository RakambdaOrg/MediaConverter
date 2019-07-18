package fr.mrcraftcod.videonormalizer.utils;

import fr.mrcraftcod.utils.config.PreparedStatementFiller;
import fr.mrcraftcod.utils.config.SQLValue;
import fr.mrcraftcod.utils.config.SQLiteManager;
import fr.mrcraftcod.videonormalizer.batch.BatchCreator;
import fr.mrcraftcod.videonormalizer.batch.PS1BatchCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Thomas Couchoud (MrCraftCod - zerderr@gmail.com) on 09/12/2017.
 *
 * @author Thomas Couchoud
 * @since 2017-12-09
 */
public class Configuration extends SQLiteManager{
	private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
	private final Map<String, Boolean> useless = new ConcurrentHashMap<>();
	
	public Configuration(final File dbFile) throws ClassNotFoundException, InterruptedException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Useless(Filee VARCHAR(512) NOT NULL, PRIMARY KEY(Filee));").waitSafely();
	}
	
	public boolean isUseless(final Path path) throws InterruptedException{
		if(useless.isEmpty()){
			sendQueryRequest("SELECT * FROM Useless;").done(resultSet -> {
				try{
					while(resultSet.next()){
						useless.put(resultSet.getString("Filee"), true);
					}
				}
				catch(SQLException e){
					LOGGER.error("Error getting useless status", e);
				}
			}).waitSafely();
		}
		return useless.containsKey(path.toString().replace("\\", "/"));
	}
	
	@Override
	public void close(){
		LOGGER.info("Closing SQL Connection...");
		super.close();
	}
	
	public BatchCreator getBatchCreator(){
		return new PS1BatchCreator();
	}
	
	public void setUseless(final Path path) throws InterruptedException{
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Useless(Filee) VALUES(?);", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString().replace("\\", "/")))).waitSafely();
	}
	
	void setUseless(final Collection<Path> paths) throws InterruptedException{
		final var placeHolders = IntStream.range(0, paths.size()).mapToObj(o -> "(?)").collect(Collectors.joining(","));
		final var values = paths.stream().map(path -> path.toString().replace("\\", "/")).flatMap(path -> List.of(new SQLValue(SQLValue.Type.STRING, path)).stream()).toArray(SQLValue[]::new);
		sendPreparedUpdateRequest("INSERT OR IGNORE INTO Useless(Filee) VALUES " + placeHolders + ";", new PreparedStatementFiller(values)).waitSafely();
		LOGGER.debug("Set downloaded status for {} items", paths.size());
	}
}
