package be.vib.imagej.registration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
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
				double[] isotonic = IsotonicRegression.Fit(zs);

				// The output of the Isotonic Regression however yields sequences of slices that
				// are taken to have identical z-values (this indeed minimizes the square error
				// in the regression). This however means that nearest neighbor sampling for z-values
				// just below and above a region of flat z's, will end up taking the same slice,
				// even though multiple slices have the same z, and it would make more sense to pick
				// the first and the last slice in this region.
				// We achieve this by modifying the output of isotonic regression so that the z-values
				// are modified from non-decreasing (but possibly equal) to *strictly* non-decreasing (never equal).				
				makeStrictlyMonotonic(isotonic);

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
	
	// Make the zs[i] slightly monotonically increasing, for i=i1 to i2.
	// On input this range of zs[i] is all identical values.
	private static void makeStrictlyMonotonic(double[] zs, int i1, int i2)  // assumption: zs are in micrometers
	{
		assert(i2 < zs.length);
		assert(i1 < i2);
		
		// The zs[i] for i1...i2 are all equal, but we want to make them monotonic.
		// We would like z's to increase by ideal_eps (but this may not be possible - see below - in that case we pick a smaller eps).
		 
		final double ideal_eps = 0.1 / 1000.0;   // eps = 0.1 nanometer
		 
		int n = i2 - i1;  // number of samples to make strictly monotonic (but now have all the same z)
		 
		double max_dz_left = (i1 == 0) ? Double.MAX_VALUE : zs[i1] - zs[i1 - 1];
		double max_dz_right = (i2 == zs.length-1) ? Double.MAX_VALUE : zs[i2 + 1] - zs[i2];
		 
		assert(max_dz_left  > 0);
		assert(max_dz_right > 0);
		 
		double max_dz = Math.min(max_dz_left, max_dz_right);  // we should not increase/decrease the z value at the end/begin of the interval by max_dz
		double max_eps = 2.0 * max_dz / n;
		 
		double eps = 0.95 * Math.min(ideal_eps, max_eps);  // the 0.95 is to ensure we do not end up with *exactly* equal z values at the interval boundaries 
		 
		// Ideally we want to modify the equals z's so that each z is eps bigger than the previous z.
		// But we need to be careful not to destroy the monotonicity of the samples outside the i1...i2 interval.
		 
		double i_mid = (i1 + i2) / 2.0;  // fractional index, only for interpolation
		 
		for (int i = i1; i <= i2; ++i)
		{
			 double dz = (i - i_mid) * eps;
		 	 zs[i] += dz;
		}
	}
	
	private static void makeStrictlyMonotonic(double[] zs)  // assumption: zs are in micrometers
	{
		if (zs.length == 0)
			return;
		
		double interval_z = zs[0];		
		int interval_begin = 0;
		int interval_end = 0;		
		
		for (int i = 1; i < zs.length; i++)
		{
				if (zs[i] == interval_z)
				{
					// extend interval
					interval_end = i;
					
					// correct the z value now (we're at the end of our i-loop and won't have the chance to do it later)
					if (interval_end == zs.length-1)
					{
						makeStrictlyMonotonic(zs, interval_begin, interval_end);
					}
				}
				else
				{
					if (interval_end != interval_begin)
					{
						makeStrictlyMonotonic(zs, interval_begin, interval_end);						
					}
					
					interval_begin = i;
					interval_end = i;
					interval_z = zs[i];
				}
		}
	}
	
	public static void main(String[] args)
	{
		double[] zs1 = { 1.0 };
		double[] zs2 = { 1.0, 1.0 };
		double[] zs3 = { 1.0, 1.0, 1.5, 2.0, 3.0, 3.0, 3.0, 4.0, 5.0, 6.0, 6.0 };  // flat zs at both ends
		double[] zs4 = { 1.0, 2.0, 2.00000001, 2.00000001, 2.00000001, 2.00000001, 4.0, 5.0, 6.0, 6.0 }; // difference between flat region and neighborhood is smaller than our ideal eps

		System.out.println(Arrays.toString(zs1));
		makeStrictlyMonotonic(zs1);
		System.out.println(Arrays.toString(zs1));
		
		System.out.println(Arrays.toString(zs2));
		makeStrictlyMonotonic(zs2);
		System.out.println(Arrays.toString(zs2));

		System.out.println(Arrays.toString(zs3));
		makeStrictlyMonotonic(zs3);
		System.out.println(Arrays.toString(zs3));

		System.out.println(Arrays.toString(zs4));
		makeStrictlyMonotonic(zs4);
		System.out.println(Arrays.toString(zs4));
	}
}
