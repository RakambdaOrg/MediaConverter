package fr.raksrinana.mediaconverter.utils;

import fr.raksrinana.utils.config.H2Manager;
import fr.raksrinana.utils.config.PreparedStatementFiller;
import fr.raksrinana.utils.config.SQLValue;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import static fr.raksrinana.utils.config.SQLValue.Type.STRING;

@Slf4j
public class Storage implements AutoCloseable{
	private final Collection<String> useless = new HashSet<>();
	private final Collection<String> newUseless = new HashSet<>();
	private final Path dbFile;
	
	public Storage(@NonNull final Path dbFile) throws IOException, SQLException{
		log.info("Loading useless files");
		this.dbFile = dbFile;
		try(var db = new H2Manager(dbFile)){
			db.sendUpdateRequest("CREATE TABLE IF NOT EXISTS Useless(Filee VARCHAR(512) NOT NULL, PRIMARY KEY(Filee));");
			
			useless.addAll(db.sendQueryRequest("SELECT * FROM Useless;", rs -> rs.getString("Filee")));
		}
	}
	
	@Override
	public void close() throws IOException, SQLException{
		log.info("Saving new useless files");
		try(var db = new H2Manager(dbFile)){
			var statementFillers = newUseless.stream()
					.map(path -> new PreparedStatementFiller(new SQLValue(STRING, path)))
					.collect(Collectors.toList());
			db.sendPreparedBatchUpdateRequest("MERGE INTO Useless(Filee) VALUES(?)", statementFillers);
		}
	}
	
	public boolean isUseless(@NonNull final Path path) throws SQLException{
		var value = path.toString().replace("\\", "/");
		return useless.contains(value);
	}
	
	public void setUseless(@NonNull final Path path){
		log.debug("Marking {} as useless", dbFile);
		var value = path.toString().replace("\\", "/");
		useless.add(value);
		newUseless.add(value);
	}
}
