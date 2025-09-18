package fr.rakambda.mediaconverter.storage.data;

import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.sql.Timestamp;

@Data
public class UselessEntry implements Comparable<UselessEntry>{
	@NonNull
	private final String path;
	@Nullable
	private final Timestamp lastModified;
	
	@Override
	public int compareTo(UselessEntry o){
		return path.compareTo(o.path);
	}
}

