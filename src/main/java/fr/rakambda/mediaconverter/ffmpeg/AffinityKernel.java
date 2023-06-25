package fr.rakambda.mediaconverter.ffmpeg;

import com.sun.jna.platform.win32.Kernel32;
import lombok.NonNull;

interface AffinityKernel extends Kernel32{
	boolean SetProcessAffinityMask(@NonNull HANDLE hProcess, int dwProcessAffinityMask);
}
