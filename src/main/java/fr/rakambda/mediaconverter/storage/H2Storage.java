package fr.rakambda.mediaconverter.storage;

import fr.rakambda.mediaconverter.storage.data.UselessEntry;
import fr.rakambda.mediaconverter.storage.sql.H2Manager;
import fr.rakambda.mediaconverter.storage.sql.PreparedStatementFiller;
import fr.rakambda.mediaconverter.storage.sql.SQLValue;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class H2Storage implements IStorage{
	private final Map<String, UselessEntry> useless = new ConcurrentHashMap<>();
	private final Queue<UselessEntry> newUseless = new ConcurrentLinkedQueue<>();
	private final Path dbFile;
	private final ReentrantLock lock;
	
	public H2Storage(@NonNull Path dbFile) throws IOException, SQLException{
		log.info("Loading useless files");
		this.dbFile = dbFile;
		this.lock = new ReentrantLock();
		try(var db = new H2Manager(dbFile)){
			db.sendUpdateRequest("""
					CREATE TABLE IF NOT EXISTS Useless(
						Filee VARCHAR(512) NOT NULL,
						LastModified TIMESTAMP,
						PRIMARY KEY(Filee)
					 );""");
			db.sendUpdateRequest("ALTER TABLE Useless  ADD COLUMN IF NOT EXISTS LastModified TIMESTAMP;");
			
			useless.putAll(db.sendQueryRequest("SELECT * FROM Useless;", rs -> new UselessEntry(rs.getString("Filee"), rs.getTimestamp("LastModified")))
					.stream().collect(Collectors.toMap(UselessEntry::getPath, Function.identity())));
		}
	}
	
	@Override
	public void close() throws IOException, SQLException{
		save();
	}
	
	public boolean isUseless(@NonNull Path path, @Nullable Timestamp lastModified){
		if(Objects.isNull(lastModified)){
			return true;
		}
		var value = path.toString().replace("\\", "/");
		var entry = useless.get(value);
		if(Objects.isNull(entry)){
			return false;
		}
		
		var entryLastModified = entry.getLastModified();
		if(Objects.isNull(entryLastModified)){
			return true;
		}
		return entryLastModified.compareTo(lastModified) >= 0;
	}
	
	public void setUseless(@NonNull Path path, @Nullable Timestamp lastModified){
		log.debug("Marking {} as useless", path);
		var value = new UselessEntry(path.toString().replace("\\", "/"), lastModified);
		useless.put(value.getPath(), value);
		newUseless.add(value);
	}
	
	public void save() throws SQLException, IOException{
		lock.lock();
		try{
			if(newUseless.isEmpty()){
				return;
			}
			log.info("Saving new useless files");
			try(var db = new H2Manager(dbFile)){
				var statementFillers = new LinkedList<PreparedStatementFiller>();
				UselessEntry entry;
				while((entry = newUseless.poll()) != null){
					statementFillers.add(new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, entry.getPath()), new SQLValue(SQLValue.Type.TIMESTAMP, entry.getLastModified())));
				}
				var result = db.sendPreparedBatchUpdateRequest("MERGE INTO Useless(Filee, LastModified) VALUES(?,?)", statementFillers);
				log.info("Saved {}/{} useless files", result, newUseless.size());
			}
		}
		finally{
			lock.unlock();
		}
	}
}
