package fr.raksrinana.videoconverter.utils;

import fr.raksrinana.utils.config.H2Manager;
import fr.raksrinana.utils.config.PreparedStatementFiller;
import fr.raksrinana.utils.config.SQLValue;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class Configuration extends H2Manager{
	private final Collection<String> useless = new ConcurrentSkipListSet<>();
	
	public Configuration(@NonNull final Path dbFile) throws IOException, SQLException{
		super(dbFile);
		sendUpdateRequest("CREATE TABLE IF NOT EXISTS Useless(Filee VARCHAR(512) NOT NULL, PRIMARY KEY(Filee));");
	}
	
	public boolean isUseless(@NonNull final Path path) throws SQLException{
		if(useless.isEmpty()){
			useless.addAll(sendQueryRequest("SELECT * FROM Useless;", rs -> rs.getString("Filee")));
		}
		return useless.contains(path.toString().replace("\\", "/"));
	}
	
	@Override
	public void close(){
		log.info("Closing SQL Connection...");
		super.close();
	}
	
	public void setUseless(@NonNull final Path path){
		sendCompletablePreparedUpdateRequest("MERGE INTO Useless(Filee) VALUES(?)", new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path.toString().replace("\\", "/"))));
	}
}
