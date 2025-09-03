package fr.rakambda.mediaconverter.itemprocessor.command;

import fr.rakambda.mediaconverter.itemprocessor.CommandConverter;
import fr.rakambda.mediaconverter.progress.ProgressBarSupplier;
import org.jspecify.annotations.NonNull;
import lombok.extern.log4j.Log4j2;
import java.nio.file.Path;

@Log4j2
public class AvifConverter extends CommandConverter{
	public AvifConverter(@NonNull Path input, @NonNull Path output, @NonNull Path temporary, boolean deleteInput, ProgressBarSupplier converterProgressBarSupplier){
		super(input, output, temporary, deleteInput, converterProgressBarSupplier);
	}
	
	@Override
	protected String[] getCommand(){
		return new String[]{
				"magick",
				getInput().toAbsolutePath().toString(),
				getTemporary().toString()
		};
	}
}
