package fr.raksrinana.mediaconverter.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

public interface IStorage extends AutoCloseable{
	boolean isUseless(Path path) throws SQLException;
	
	void setUseless(Path path);
	
	void save() throws SQLException, IOException;
}
