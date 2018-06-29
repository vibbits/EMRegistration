package be.vib.imagej.registration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
	static public class Pair
	{
		public Path path;
		public double z;  // in micrometers
		
		Pair(Path path, double z)
		{
			this.path = path;
			this.z = z;
		}
	}
	
	public static ResampleInfo[] nearestNeighborResample(List<Path> inputFiles, double sliceThicknessNM, boolean preserveSliceOrder) // sliceThicknessNM in nanometers
	{
		// Extract the z-postion that is encoded in the filenames 
		ArrayList<Pair> slices = parseFilenames(inputFiles);
		// TODO: what if this fails? error thrown?
		
		// Slice positions do *not* always increase strictly monotonically.
		// Apparently the z-estimate of the microscope 
		boolean monotonic = slicePositionsMonotonicallyIncreasing(slices);
		if (!monotonic)
		{
			if (preserveSliceOrder)
			{
				System.out.println("Z-position of slices is *not* monotonically increasing! Performing isotonic regression.");
				
				System.out.println("Before isotonic regression:");
				for (int i = 0; i < slices.size(); i++)
					System.out.println(slices.get(i).z + " " + slices.get(i).path);

				// Extract zs
				int n = slices.size();
				double[] zs = new double[n];
				for (int i = 0; i < n; i++)
					zs[i] = slices.get(i).z;

				// Perform isotonic regression on the z positions
				// The result is z's that are as close to the original zs as possible,
				// but are non-decreasing.
				// FIXME: they do result in identical zs wherever there were decreasing zs in the original zs.
				//        we should probably replace these identical values with slightly increasing values, so that
				//        if we perform nearest neighbor sampling in that region we use these slices and in the right order.
				double[] isotonic = IsotonicRegression.Fit(zs);

				// Update the slice positions
				for (int i = 0; i < n; i++)
					slices.get(i).z = isotonic[i];

				System.out.println("After isotonic regression:");
				for (int i = 0; i < slices.size(); i++)
					System.out.println(slices.get(i).z + " " + slices.get(i).path);

			}
			else
			{
				System.out.println("Z-position of slices is *not* monotonically increasing! Re-ordering them.");
				slices = sortByMonotonicallyIncreasingZ(slices);
			}

		}
				
		ResampleInfo[] resampleInfo = doNearestNeighborResampling(slices, sliceThicknessNM);
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
	
	private static ResampleInfo[] doNearestNeighborResampling(ArrayList<Pair> slices, double sliceThicknessNM)  // sliceThicknessNM in nanometers
	{
		final double dz = sliceThicknessNM / 1000.0;  // z and dz are in microns
		final double zfirst = slices.get(0).z;
		final double zlast = slices.get(slices.size()-1).z;
		final int numResampledSlices = (int)((zlast - zfirst) / dz + dz / 2.0);
		final int numOriginalSlices = slices.size();		
		
		System.out.println("zfirst="+zfirst+" zlast=" + zlast + " numresampleslices=" + numResampledSlices + " numoriginalslices=" + numOriginalSlices);
		
		int indexNearestZ = 0;
		ResampleInfo[] resampleInfo = new ResampleInfo[numResampledSlices];
		
		for (int i = 0; i < numResampledSlices; i++)
		{
			// Our "resampled" slices are position equidistantly:
			// we want all our slices to be dz apart.
			double desiredZ = zfirst + i * dz;
			
			// Find the slice closest to our desired Z position.
			// Important note: the = in the >= comparison is mandatory because sometimes the microscope returns several slices with the same z value
			// (because its z-value estimates are not always accurate and sometimes go up and down). The = sign in the >= ensures that we skip over identical
			// z-values; without the = sign we would get stuck and stop increasing indexNearestZ.
			while ((Math.abs(slices.get(indexNearestZ).z - desiredZ) >= Math.abs(slices.get(indexNearestZ + 1).z - desiredZ)) && (indexNearestZ < numOriginalSlices - 1))
			{
				indexNearestZ++;
			}
			
			double nearestOriginalZ = slices.get(indexNearestZ).z;
			Path nearestOriginalPath = slices.get(indexNearestZ).path;
			
			// Remember the mapping between desired z position and nearest original slice.
			resampleInfo[i] = new ResampleInfo(desiredZ, nearestOriginalZ, nearestOriginalPath);

			System.out.println("Resampled slice " + i + " at desired pos " + desiredZ + " has closest original slice at " + nearestOriginalZ + " (index " + indexNearestZ + ") path=" + nearestOriginalPath);
		}
		return resampleInfo;
	}

	// Returns a dictionary sorted alphabetically on key.
	// The key is the original Path of the file; the value
	// is the z-position of the corresponding slice (in micrometers)
	// as encoded in the filename.
	private static ArrayList<Pair> parseFilenames(List<Path> inputFiles)
	{		
		// We're assuming that the filenames always have this pattern:
		//    prefix + "_" + slice number + "_" + "z=" + floating point number + "um" + ".tif"
		// Example filename: "slice_00025_z=0.2908um.tif"
		String regex = ".*_(\\d+)_z=(\\d*\\.?\\d*)um\\.[^\\.]+";
		Pattern pattern = Pattern.compile(regex);
		
		ArrayList<Pair> slices = new ArrayList<Pair>();
		for (Path path : inputFiles)
		{
			Matcher m = pattern.matcher(path.toString());
			boolean match = m.find();
			assert(match); // TODO: throw error if no match?
			
			String sliceNumber = m.group(1); // for debugging only
			String sliceZ = m.group(2);
			
			double z = Double.valueOf(sliceZ);
			Pair pair = new Pair(path, z);
			slices.add(pair);

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
	private static boolean slicePositionsMonotonicallyIncreasing(ArrayList<Pair> slices)
	{
		double zprev = -1e9; // -infinity
		for (Pair p : slices)
		{
			double z = p.z;
			if (zprev >= z)
				return false;
			zprev = z;
		}
		return true;
	}
	
	private static ArrayList<Pair> sortByMonotonicallyIncreasingZ(ArrayList<Pair> slices)
	{
		System.out.println("sortByMonotonicallyIncreasingZ");

		System.out.println("before sorting:");
		for (Pair p : slices)
		{
			System.out.println(p.z);
		}
		
		slices.sort((o1, o2) -> Double.compare(o1.z, o2.z));
		
		System.out.println("after sorting:");
		for (Pair p : slices)
		{
			System.out.println(p.z);
		}
		
		return slices;
	}
}
