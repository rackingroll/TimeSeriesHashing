package edu.cornell.recordLinkage;

import java.io.IOException;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/*
 * @author Anshumali Shrivastava
 * Code assumes that we have hashes example minhashes
 */
public class HashBucketsEfficient {

	int _K;
	int _L;
	int _range = (int) Math.pow(2, 24);
	long[][] _As;
	long[][] _Bs;

	List<HashMap<Integer, Set<Integer>>> _Tables = new ArrayList<HashMap<Integer, Set<Integer>>>();

	/*
	 * Initializes L Hashtables with index coming from K different hashes.
	 */

	public HashBucketsEfficient(int K, int L) {
		Random rn = new Random(System.currentTimeMillis());
		_K = K;
		_L = L;
		_As = new long[_L][];
		_Bs = new long[_L][];
		for (int i = 0; i < _L; i++) {
			_Tables.add(new HashMap<Integer, Set<Integer>>());
			_As[i] = new long[_K];
			_Bs[i] = new long[_K];
			for (int j = 0; j < _K; j++) {
				_As[i][j] = rn.nextLong();
				if (_As[i][j] % 2 == 0)
					_As[i][j] = _As[i][j] + 1;
				_Bs[i][j] = rn.nextLong();
			}
		}
	}

	/*
	 * Use this as an example on how to call LSH add and LSH query
	 */

	public String[] getAllRetriecedRecord(List<int[]> HashMatrix) throws IOException {
		// HashMap<Integer, Set<Integer>> allRetrieved = new HashMap<Integer,
		// Set<Integer>>();

		HashMap<String, Boolean> seenPair = new HashMap<String, Boolean>();
		for (int i = 0; i < HashMatrix.size(); i++)
			LSHAdd(i, HashMatrix.get(i));

		RecordLinkage.log("Done Adding all Records to Buckets");

		for (int i = 0; i < HashMatrix.size(); i++) {
			Set<Integer> retrieved = LSHRetrieve(HashMatrix.get(i));
			// retrieved.remove(i);
			for (Iterator<Integer> setIterator = retrieved.iterator(); setIterator.hasNext();) {
				Integer matchedRecord = setIterator.next();
				if (matchedRecord == i)
					continue;
				if (matchedRecord > i) {
					String recordPair = i + "$" + matchedRecord;
					if (seenPair.containsKey(recordPair))
						continue;
					else
						seenPair.put(recordPair, true);
				} else {
					String recordPair = matchedRecord + "$" + i;
					if (seenPair.containsKey(recordPair))
						continue;
					else
						seenPair.put(recordPair, true);
				}
			}
		}

		return seenPair.keySet().toArray(new String[0]);
	}

	/*
	 * Add an element, we only need K *L different hash values (example
	 * MinHashes) for that record along with its index number or identifier.
	 */

	public void LSHAdd(int recIndex, int[] hashes) throws UnexpectedException {

		if (hashes.length < _K * _L)
			throw new UnexpectedException("no of minhased less than " + _K * _L + " the size is " + hashes.length);

		for (int i = 0; i < _L; i++) {
			long hashedbucket = 0;
			for (int j = 0; j < _K; j++)
				hashedbucket += _As[i][j] * hashes[i * _K + j] + _Bs[i][j];
			int hashvalue = (int) ((hashedbucket << 10) >>> 40);
			if (!_Tables.get(i).containsKey(hashvalue)) {
				Set<Integer> set = new TreeSet<Integer>();
				set.add(recIndex);
				_Tables.get(i).put(hashvalue, set);
			} else
				_Tables.get(i).get(hashvalue).add(recIndex);
		}

	}

	/*
	 * Retrieve potential near neighbors from the L tables for a given record.
	 * The argument is K*L Hashes of the record. (example MiHashes) Returns a
	 * set of record indices as potential near neighbor.
	 */

	public Set<Integer> LSHRetrieve(int[] hashes) throws UnexpectedException {

		Set<Integer> retrieved = new TreeSet<Integer>();

		if (hashes.length < _K * _L)
			throw new UnexpectedException("no of minhased less than K*L");

		for (int i = 0; i < _L; i++) {
			long hashedbucket = 0;
			for (int j = 0; j < _K; j++)
				hashedbucket += _As[i][j] * hashes[i * _K + j] + _Bs[i][j];
			int hashvalue = (int) ((hashedbucket << 10) >>> 40);
			if (!_Tables.get(i).containsKey(hashvalue))
				continue;
			else
				retrieved.addAll(_Tables.get(i).get(hashvalue));
		}

		return retrieved;
	}

}
