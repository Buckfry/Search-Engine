
/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 * The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

	/**
	 * Document-independent values that should be determined just once. Some
	 * retrieval models have these, some don't.
	 */

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r
	 *            The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch(RetrievalModel r) {
		return this.docIteratorHasMatchFirst(r);
	}

	/**
	 * Get a score for the document that docIteratorHasMatch matched.
	 * 
	 * @param r
	 *            The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	public double getScore(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return this.getScoreUnrankedBoolean(r);
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return this.getScoreRankedBoolean(r);
		} else if (r instanceof RetrievalModelBM25) {
			return this.getScoreBM25(r);
		} else if (r instanceof RetrievalModelIndri) {
			return this.getScoreIndri(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the SCORE operator.");
		}
	}

	/**
	 * getScore for the Unranked retrieval model.
	 * 
	 * @param r
	 *            The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	public double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
	}

	/**
	 * Initialize the query operator (and its arguments), including any internal
	 * iterators. If the query operator is of type QryIop, it is fully
	 * evaluated, and the results are stored in an internal inverted list that
	 * may be accessed via the internal iterator.
	 * 
	 * @param r
	 *            A retrieval model that guides initialization
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	public void initialize(RetrievalModel r) throws IOException {

		Qry q = this.args.get(0);
		q.initialize(r);
	}

	/**
	 * getScore for the RankedBoolean retrieval model.
	 * 
	 * @param r
	 *            The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return this.getTF();
		}
	}

	/**
	 * getScore for the BM25 retrieval model
	 * 
	 * @param r
	 *            the retrieval model that determines how scores are calculated.
	 * @return the document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	private double getScoreBM25(RetrievalModel r) throws IOException {
		
		RetrievalModelBM25 rb = (RetrievalModelBM25) r;
		double k1 = rb.getK1();
		double k3 = rb.getK3();
		double b = rb.getB();

		QryIop q = (QryIop) this.args.get(0);
		if(!q.docIteratorHasMatch(r))
			return 0;
		int docId = q.docIteratorGetMatch();
		double tf = q.docIteratorGetMatchPosting().tf;
		double df = q.getDf();

		double avgDocLen = Idx.getAvgDocLen(q.getField());
		double docLen = Idx.getFieldLength(q.getField(), docId);
		double tfWeight = tf / (tf + k1 * (1 - b + b * (docLen / avgDocLen)));
		double idfWeight = Math.log((Idx.getNumDocs() - df + 0.5) / (df + 0.5));// RSJ weigh													// weight
		if(idfWeight<0)
			idfWeight = 0;
		double qtf = 1.0;
		double userWeight = (k3 + 1) * qtf / (k3 + qtf);
		return idfWeight * tfWeight * userWeight;
	}

	private double getScoreIndri(RetrievalModel r) throws IOException {
		RetrievalModelIndri ri = (RetrievalModelIndri) r;
		double lambda = ri.getLambda();
		double mu = ri.getMu();
		QryIop q = (QryIop) this.args.get(0);
		String field = q.getField();
		double colLen = Idx.getSumOfFieldLengths(field);
		double tf = q.docIteratorGetMatchPosting().tf;
		double ctf = (double) q.getCtf();
		int docId = q.docIteratorGetMatch();
		double docLen = Idx.getFieldLength(field, docId);
		double score = (1 - lambda) * (tf + mu * ctf / colLen) / (docLen + mu) + lambda * ctf / colLen;
		return score;
	}

	@Override
	public double getDefaultScore(RetrievalModel r, int docId) throws IOException {
		// TODO Auto-generated method stub
		RetrievalModelIndri ri = (RetrievalModelIndri) r;
		double lambda = ri.getLambda();
		double mu = ri.getMu();
		QryIop q = (QryIop) this.args.get(0);
		double ctf = (double) q.getCtf();
		double colLen = Idx.getSumOfFieldLengths(q.getField());
		// int docId = q.docIteratorGetMatch();
		double docLen = Idx.getFieldLength(q.getField(), docId);
		double score = (1 - lambda) * (mu * ctf / colLen) / (docLen + mu) + lambda * ctf / colLen;
		return score;
	}
}
