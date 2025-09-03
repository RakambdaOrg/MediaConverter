package fr.rakambda.mediaconverter.storage;

import fr.rakambda.mediaconverter.storage.sql.H2Manager;
import fr.rakambda.mediaconverter.storage.sql.PreparedStatementFiller;
import fr.rakambda.mediaconverter.storage.sql.SQLValue;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReentrantLock;

@Log4j2
public class H2Storage implements IStorage{
	private final Collection<String> useless = new ConcurrentSkipListSet<>();
	private final Queue<String> newUseless = new ConcurrentLinkedQueue<>();
	private final Path dbFile;
	private final ReentrantLock lock;
	
	public H2Storage(@NonNull Path dbFile) throws IOException, SQLException{
		log.info("Loading useless files");
		this.dbFile = dbFile;
		this.lock = new ReentrantLock();
		try(var db = new H2Manager(dbFile)){
			db.sendUpdateRequest("CREATE TABLE IF NOT EXISTS Useless(Filee VARCHAR(512) NOT NULL, PRIMARY KEY(Filee));");
			
			useless.addAll(db.sendQueryRequest("SELECT * FROM Useless;", rs -> rs.getString("Filee")));
		}
	}
	
	@Override
	public void close() throws IOException, SQLException{
		save();
	}
	
	public boolean isUseless(@NonNull Path path){
		var value = path.toString().replace("\\", "/");
		return useless.contains(value);
	}
	
	public void setUseless(@NonNull Path path){
		log.debug("Marking {} as useless", path);
		var value = path.toString().replace("\\", "/");
		useless.add(value);
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
				String path;
				while((path = newUseless.poll()) != null){
					statementFillers.add(new PreparedStatementFiller(new SQLValue(SQLValue.Type.STRING, path)));
				}
				var result = db.sendPreparedBatchUpdateRequest("MERGE INTO Useless(Filee) VALUES(?)", statementFillers);
				log.info("Saved {}/{} useless files", result, newUseless.size());
			}
		}
		finally{
			lock.unlock();
		}
	}
}
