package be.vib.imagej.registration;

import java.nio.file.Path;

public class ResampleInfo
{
	public double desiredZ;
	public double originalZ;
	public Path originalFilename;
	
	public ResampleInfo(double desiredZ, double originalZ, Path originalFilename)
	{
		this.desiredZ = desiredZ;
		this.originalZ = originalZ;
		this.originalFilename = originalFilename;
	}
}