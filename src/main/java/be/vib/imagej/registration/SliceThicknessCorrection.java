package be.vib.imagej.registration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SliceThicknessCorrection
{
	public static ResampleInfo[] nearestNeighborResample(List<Path> inputFiles, double dz) // dz in micrometers
	{
		// Extract the z-postion that is encoded in the filenames 
		SortedMap<Path, Double> slices = parseFilenames(inputFiles);
		// TODO: what if this fails? error thrown?
		
		// Slice positions do *not* always increase strictly monotonically.
		// Apparently the z-estimate of the microscope 
		boolean monotonic = slicePositionsMonotonicallyIncreasing(slices);
		if (!monotonic)
		{
			System.out.println("Z-position of slices is *not* monotonically increasing! Re-ordering them.");
			slices = sortByMonotonicallyIncreasingZ(slices);
		}
				
		ResampleInfo[] resampleInfo = doNearestNeighborResampling(slices, dz);
		return resampleInfo;
	}
	
	public static List<Path> getResampledFiles(ResampleInfo[] resampleInfo)
	{
		List<Path> paths = new ArrayList<Path>();
		for (ResampleInfo info : resampleInfo)
		{
			paths.add(info.originalFilename);
		}		
		return paths;
	}
	
	public static void printResampleInfo(ResampleInfo[] resampleInfo)
	{
		for (ResampleInfo info : resampleInfo)
		{
			System.out.println(info.desiredZ + " -> " + info.originalZ + " -> " + info.originalFilename);
		}
	}

	private static ResampleInfo[] doNearestNeighborResampling(SortedMap<Path, Double> slices, double dz)
	{
		final double zfirst = slices.get(slices.firstKey());
		final double zlast = slices.get(slices.lastKey());;
		final int numResampledSlices = (int)((zlast - zfirst) / dz + dz / 2.0);
		final int numOriginalSlices = slices.size();		
		final Path[] originalPaths = (Path[])slices.keySet().toArray();
		final Double[] originalZs = (Double[])slices.values().toArray();
		
		int indexNearestZ = 0;
		ResampleInfo[] resampleInfo = new ResampleInfo[numResampledSlices];
		
		for (int i = 0; i < numResampledSlices; i++)
		{
			// Our "resampled" slices are position equidistantly:
			// we want all our slices to be dz apart.
			double desiredZ = zfirst + i * dz;
			
			// Find the slice closest to our desired Z position.
			while ((Math.abs(originalZs[indexNearestZ + 1] - desiredZ) < Math.abs(originalZs[indexNearestZ] - desiredZ)) && (indexNearestZ < numOriginalSlices - 1))
			{
				indexNearestZ++;
			}
			
			double nearestOriginalZ = originalZs[indexNearestZ];
			
			// Remember the mapping between desired z position and nearest original slice.
			resampleInfo[i] = new ResampleInfo(desiredZ, nearestOriginalZ, originalPaths[indexNearestZ]);
		}
		return resampleInfo;
	}

	// Returns a dictionary sorted alphabetically on key.
	// The key is the original Path of the file; the value
	// is the z-position of the corresponding slice (in micrometers)
	// as encoded in the filename.
	private static SortedMap<Path, Double> parseFilenames(List<Path> inputFiles)
	{		
		// We're assuming that the filenames always have this pattern:
		//    prefix + "_" + slice number + "_" + "z=" + floating point number + "um" + ".tif"
		// Example filename: "slice_00025_z=0.2908um.tif"
		String regex = ".*_(\\d+)_z=(\\d*\\.?\\d*)um\\.[^\\.]+";
		Pattern pattern = Pattern.compile(regex);
		
		SortedMap<Path, Double> slices = new TreeMap<Path, Double>();
		for (Path path : inputFiles)
		{
			Matcher m = pattern.matcher(path.toString());
			boolean match = m.find();
			assert(match); // TODO: throw error if no match?
			
			String sliceNumber = m.group(1); // for debugging only
			String sliceZ = m.group(2);
			
			double z = Double.valueOf(sliceZ);
			slices.put(path, z);

			System.out.println(sliceNumber + " " + sliceZ + " " + path.toString());			
		}
		
		return slices;
	}
	
	// Returns whether the z positions of the slices are increasing
	// monotonically. This should be the case since they should be
	// successive slices of a 3D EM stack.
	// 
	// TODO: note, in practice they are not monotonic :-(
	//       see first few slices of F:\Datasets\EM Registration\Project_103_Patrizia\2018_03_16_P103_shPerk_bQ\Raw_data
	private static boolean slicePositionsMonotonicallyIncreasing(SortedMap<Path, Double> slices)
	{
		double zprev = -1e9; // -infinity
		for (Map.Entry<Path, Double> entry : slices.entrySet())
		{
			double z = entry.getValue();
			if (zprev >= z)
				return false;
			zprev = z;
		}
		return true;
	}
	
	private static SortedMap<Path, Double> sortByMonotonicallyIncreasingZ(SortedMap<Path, Double> slices)
	{
		assert(false); // CHECK CODE BELOW
		
		TreeMap<Path, Double> sorted = slices.entrySet().stream()
				                                        .sorted(Map.Entry.comparingByValue())
				                                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, TreeMap::new));		
		return sorted;
	}
}
