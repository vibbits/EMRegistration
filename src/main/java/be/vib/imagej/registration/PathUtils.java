package be.vib.imagej.registration;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils
{
	public static String getPathString(Path path)
	{
		if (path == null)
		{
			return "";
		}
		else
		{
			return path.toString();
		}
	}
	
	public static Path pathFromString(String folder)
	{
		try
		{
			return Paths.get(folder);
		}
		catch (InvalidPathException e)
		{
			return null;
		}
	}
}
