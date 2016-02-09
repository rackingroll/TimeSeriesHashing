package edu.cornell.recordLinkage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/*
 * @author Anshumali Shrivastava
 */
public class RecordLinkageUtils {

	public static void main(String[] args) {

		RecordLinkageUtils rcutil = new RecordLinkageUtils();
		if (args.length < 5 || args == null) {
			System.out.println(
					"command: java -jar RecordLinkageFastMinhash.jar csvinputfilepath outputfilepath numofhashes noshingles ismergeshingles");
			return;
		}

		rcutil.generateMinhashfromCSV(args[0], args[1], Integer.valueOf(args[2]), Integer.valueOf(args[3]),
				Boolean.valueOf(args[4]));
	}

	public String[] allngramsSubstring(String str, int length) {
		int resultCount = 0;
		for (int i = 1; i <= length; i++)
			resultCount += str.length() - i + 1;

		String[] result = new String[resultCount];
		int count = 0;
		for (int j = 1; j <= length; j++)
			for (int i = 0; i < str.length() - j + 1; i++) {
				result[count] = str.substring(i, i + j);
				count++;
			}
		return result;
	}

	public void generateMinhashfromCSV(String inFilename, String outFilename, int noofHashes, int noOfshingles,
			boolean ismerge) {

		int chunksize = 60;

		long a[] = new long[(int) Math.ceil(noofHashes / chunksize)];
		long b[] = new long[(int) Math.ceil(noofHashes / chunksize)];

		Random rn = new Random(System.currentTimeMillis());

		byte[] randbits = new byte[(int) (Math.ceil(noofHashes / chunksize) * chunksize)];
		new Random().nextBytes(randbits);

		for (int j = 0; j < (int) Math.ceil(noofHashes / chunksize); j++) {

			a[j] = rn.nextLong();
			if (a[j] % 2 == 0)
				a[j] = a[j] + 1;
			b[j] = rn.nextLong();
		}

		BufferedReader br = null;
		BufferedWriter writer = null;
		String line = "";

		try {
			writer = new BufferedWriter(new FileWriter(outFilename));
			br = new BufferedReader(new FileReader(inFilename));
			line = br.readLine(); // skip header
			while ((line = br.readLine()) != null) {
				line.replace(",", "$");
				line.replace("  ", " ");
				String[] shingles;
				if (!ismerge)
					shingles = ngramsSubstring(line, noOfshingles);
				else
					shingles = allngramsSubstring(line, noOfshingles);

				// System.out.println(Arrays.toString(shingles));

				for (int j = 0; j < (int) Math.ceil(noofHashes / chunksize); j++) {
					int[] minhashes = getFastMinHash(shingles, chunksize, a[j], b[j], randbits, j * chunksize);
					writer.write(Arrays.toString(minhashes).replace("[", "").replace("]", ","));
				}

				writer.write("\n");
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null)
				try {
					br.close();
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		System.out.println("Done");

	}

	public List<int[]> generateMinhashMatfromCSV(String inFilename, int noofHashes, int noOfshingles, boolean ismerge,
			boolean isweighted) {

		double chunksize = 60;

		long a[] = new long[(int) Math.ceil(noofHashes / chunksize)];
		long b[] = new long[(int) Math.ceil(noofHashes / chunksize)];

		Random rn = new Random(System.currentTimeMillis());

		byte[] randbits = new byte[(int) (Math.ceil(noofHashes / chunksize) * chunksize)];
		new Random().nextBytes(randbits);

		for (int j = 0; j < (int) Math.ceil(noofHashes / chunksize); j++) {

			a[j] = rn.nextLong();
			if (a[j] % 2 == 0)
				a[j] = a[j] + 1;
			b[j] = rn.nextLong();
		}

		List<int[]> allminhashes = new ArrayList<int[]>();
		BufferedReader br = null;
		String line = "";

		try {
			br = new BufferedReader(new FileReader(inFilename));
			line = br.readLine(); // skip header
			while ((line = br.readLine()) != null) {
				if (line.trim().length() < 2)
					continue;
				line.replace(",", "$");
				line.replaceAll("( )+", " ");
				String[] shingles;
				if (!ismerge)
					if (!isweighted)
						shingles = ngramsSubstring(line, noOfshingles);
					else
						shingles = weightedNgramsSubstring(line, noOfshingles);
				else if (!isweighted)
					shingles = allngramsSubstring(line, noOfshingles);
				else
					shingles = weightedAllngramsSubstring(line, noOfshingles);

				// System.out.println(Arrays.toString(shingles));
				int[] allhashes = new int[(int) ((int) Math.ceil(noofHashes / chunksize) * chunksize)];
				for (int j = 0; j < (int) Math.ceil(noofHashes / chunksize); j++)
					writeFastMinHash(shingles, (int) chunksize, a[j], b[j], randbits, (int) (j * chunksize), allhashes);
				allminhashes.add(allhashes);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null)
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		System.out.println("Done Minhashing");

		return allminhashes;
	}

	/*
	*
	* Use This ANSHU
	*/
	public int[] getFastMinHash(Set<Integers> nonZeroIndices, int numofHashes, long a, long b, byte[] randbits, int index) {

		int[] minhashes = new int[numofHashes];

		for (int i = 0; i < numofHashes; i++)
			minhashes[i] = Integer.MAX_VALUE;

		double binlength = Math.ceil(Math.pow(2, 20) / (numofHashes + 0.0));
		// int[] hashedgrams = new int[ngrams.length];
		for (Iterator<Integer> setIterator = nonZeroIndices.iterator(); nonZeroIndices.hasNext();){

			long hashedlong = ((a * setIterator.next() + b) << 3) >>> 44;
			// System.out.println(hashedlong);
			int hashedgrams = (int) hashedlong;
			// System.out.println(hashedgrams);
			int quo = (int) Math.floor(hashedgrams / binlength);
			if (minhashes[quo] > hashedgrams - quo * binlength)
				minhashes[quo] = hashedgrams - quo * (int) binlength;
		}

		// now rotate
		int[] minhashesappended = new int[2 * numofHashes];
		for (int i = 0; i < numofHashes; i++) {
			minhashesappended[i] = minhashes[i];
			minhashesappended[numofHashes + i] = minhashes[i];
		}
		int[] minhashesappended2 = Arrays.copyOf(minhashesappended, minhashesappended.length);

		for (int i = 2 * numofHashes - 2; i >= 0; i--)
			if (minhashesappended[i] == Integer.MAX_VALUE)
				minhashesappended[i] = minhashesappended[i + 1] + (int) binlength;

		for (int i = 1; i < 2 * numofHashes; i++)
			if (minhashesappended2[i] == Integer.MAX_VALUE)
				minhashesappended2[i] = minhashesappended2[i - 1] + (int) binlength;

		for (int i = 0; i < numofHashes; i++)
			if (minhashes[i] == Integer.MAX_VALUE)
				if (randbits[index + i] == 1)
					minhashes[i] = minhashesappended[i];
				else
					minhashes[i] = minhashesappended2[numofHashes + i];

		return minhashes;
	}

	public String[] ngramsSubstring(String str, int length) {
		final int resultCount = str.length() - length + 1;
		String[] result = new String[resultCount];
		for (int i = 0; i < resultCount; i++)
			result[i] = str.substring(i, i + length);
		return result;
	}

	public String[] weightedAllngramsSubstring(String str, int length) {
		int resultCount = 0;
		for (int i = 1; i <= length; i++)
			resultCount += str.length() - i + 1;
		HashMap<String, Integer> ngrams = new HashMap<String, Integer>();
		List<String> weightedshingles = new ArrayList<String>();
		// String[] result = new String[resultCount];
		for (int j = 1; j <= length; j++)
			for (int i = 0; i < str.length() - j + 1; i++) {
				String currgram = str.substring(i, i + j);
				if (ngrams.containsKey(currgram)) {
					ngrams.put(currgram, ngrams.get(currgram) + 1);
					weightedshingles.add(currgram + String.valueOf(ngrams.get(currgram) + 1));
				} else {
					ngrams.put(currgram, 1);
					weightedshingles.add(currgram + "1");
				}
			}

		return weightedshingles.toArray(new String[weightedshingles.size()]);
		// (String[]) weightedshingles.toArray();
	}

	public String[] weightedNgramsSubstring(String str, int length) {
		final int resultCount = str.length() - length + 1;
		HashMap<String, Integer> ngrams = new HashMap<String, Integer>();
		List<String> weightedshingles = new ArrayList<String>();
		for (int i = 0; i < resultCount; i++) {
			String currgram = str.substring(i, i + length);
			if (ngrams.containsKey(currgram)) {
				ngrams.put(currgram, ngrams.get(currgram) + 1);
				weightedshingles.add(currgram + String.valueOf(ngrams.get(currgram) + 1));
			} else {
				ngrams.put(currgram, 1);
				weightedshingles.add(currgram + "1");
			}
		}

		return weightedshingles.toArray(new String[weightedshingles.size()]);
	}

	public void writeFastMinHash(String[] ngrams, int numofHashes, long a, long b, byte[] randbits, int index,
			int[] allhashes) {

		int[] minhashes = new int[numofHashes];

		for (int i = 0; i < numofHashes; i++)
			minhashes[i] = Integer.MAX_VALUE;

		double binlength = Math.ceil(Math.pow(2, 20) / (numofHashes + 0.0));
		// int[] hashedgrams = new int[ngrams.length];
		for (int i = 0; i < ngrams.length; i++) {

			long hashedlong = ((a * ngrams[i].hashCode() + b) << 3) >>> 44;
			// System.out.println(hashedlong);
			int hashedgrams = (int) hashedlong;
			// System.out.println(hashedgrams);
			int quo = (int) Math.floor(hashedgrams / binlength);
			if (minhashes[quo] > hashedgrams - quo * binlength)
				minhashes[quo] = hashedgrams - quo * (int) binlength;
		}

		// now rotate
		int[] minhashesappended = new int[2 * numofHashes];
		for (int i = 0; i < numofHashes; i++) {
			minhashesappended[i] = minhashes[i];
			minhashesappended[numofHashes + i] = minhashes[i];
		}
		int[] minhashesappended2 = Arrays.copyOf(minhashesappended, minhashesappended.length);

		for (int i = 2 * numofHashes - 2; i >= 0; i--)
			if (minhashesappended[i] == Integer.MAX_VALUE)
				minhashesappended[i] = minhashesappended[i + 1] + (int) binlength;

		for (int i = 1; i < 2 * numofHashes; i++)
			if (minhashesappended2[i] == Integer.MAX_VALUE)
				minhashesappended2[i] = minhashesappended2[i - 1] + (int) binlength;

		for (int i = 0; i < numofHashes; i++)
			if (minhashes[i] == Integer.MAX_VALUE)
				if (randbits[index + i] == 1)
					allhashes[index + i] = minhashesappended[i];
				else
					allhashes[index + i] = minhashesappended2[numofHashes + i];
			else
				allhashes[index + i] = minhashes[i];

	}

}
