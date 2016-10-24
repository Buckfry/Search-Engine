
/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 * The NEAR operator for all retrieval models. Return a document if all of the
 * query arguments occur in the document, in order, with no more than n-1 terms
 * separating two adjacent terms. For example, #NEAR/2(a b c) matches "a b c",
 * "a x b c", "a b x c", and "a x b x c", but not "a x x b c". The document's
 * score will be the number of times the NEAR/n operator matched the document
 * (i.e., its frequency).
 *
 */
public class QryIopNear extends QryIop {
	private int distance;

	/**
	 * constructor
	 * 
	 * @param distance
	 */
	public QryIopNear(int distance) {
		this.distance = distance;
	}

	/**
	 * Evaluate the query operator; the result is an internal inverted list that
	 * may be accessed via the internal iterators.
	 * 
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	protected void evaluate() throws IOException {
		this.invertedList = new InvList(this.field);
		if (args == null || args.size() == 0 || !this.docIteratorHasMatchAll(null))
			return;
		while (this.docIteratorHasMatchAll(null)) {
			int docid = this.getCachedDoc();
//			System.out.println("+++++++++++++++++++++++++++++++"+docid);
			List<Integer> positions = new LinkedList<>();
			boolean eval = true;

			while (eval) {
				QryIop q0 = (QryIop) this.args.get(0);
				if(!q0.locIteratorHasMatch())
					break;
				int preLoc = q0.locIteratorGetMatch();
				boolean match = true;
				for (int i = 1; i < this.args.size(); i++) {
					QryIop qi = (QryIop) this.args.get(i);
					qi.locIteratorAdvancePast(preLoc);
					if (!qi.locIteratorHasMatch()) {
						eval = false;
						match = false;
						break;
					}
					int nowLoc = qi.locIteratorGetMatch();//nowLoc must be greater than preLov 
					if (nowLoc - preLoc <= this.distance) {
						preLoc = nowLoc;
					} else {
						match = false;
						break;
					}
				}
				q0.locIteratorAdvance();
				if (match) {
					positions.add(preLoc);
					for (int i = 1; i < this.args.size(); i++) {
						((QryIop) this.args.get(i)).locIteratorAdvance();
					}
				}
			}
			if(positions.size()!=0)
				this.invertedList.appendPosting(docid, positions);
			for(Qry q:this.args){
				q.docIteratorAdvancePast(docid);
			}
		}
	}

	/**
	 * Get a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {
		return (this.distance + "." + this.field);
	}
}
