
/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 * The AND operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r
	 *            The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch(RetrievalModel r) {
		if (r instanceof RetrievalModelIndri)
			return this.docIteratorHasMatchMin(r);
		else
			return this.docIteratorHasMatchAll(r);
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
		} else if (r instanceof RetrievalModelIndri) {
			return this.getScoreIndri(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the OR operator.");
		}
	}

	/**
	 * getScore for the UnrankedBoolean retrieval model.
	 * 
	 * @param r
	 *            The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException
	 *             Error accessing the Lucene index
	 */
	private double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
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

	private double getScoreIndri(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache() || args.size() == 0)
			return 0;
		double score = 1;
//		double divisor = 1.0 / args.size();
		int docId = docIteratorGetMatch();
		for (int i = 0; i < args.size(); i++) {
			QrySop q = (QrySop) args.get(i);
//			double tempScore;
			if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == docId) {
				score *= q.getScore(r);
				// tempScore = q.getScore(r);
			} else {
				score *= q.getDefaultScore(r, docId);
				// tempScore = q.getDefaultScore(r, docId);
			}
			// score *= Math.pow(tempScore, divisor);
		}
		return Math.pow(score, 1.0 / args.size());
		// return score;
	}

	@Override
	public double getDefaultScore(RetrievalModel r, int docId) throws IOException {
		// TODO Auto-generated method stub
		if (!docIteratorHasMatchCache() || args.size() == 0)
			return 0;
		double score = 1;
//		double divisor = 1.0 / args.size();
		for (int i = 0; i < args.size(); i++) {
			QrySop q = (QrySop) args.get(i);
			score *= q.getDefaultScore(r, docId);
			// score *= Math.pow(q.getDefaultScore(r, docId), divisor);

		}
		return Math.pow(score, 1.0 / args.size());
		// return score;
	}

}
