package fr.rakambda.mediaconverter.ffmpeg;

import com.sun.jna.platform.win32.Kernel32;

interface AffinityKernel extends Kernel32{
	boolean SetProcessAffinityMask(HANDLE hProcess, int dwProcessAffinityMask);
}
