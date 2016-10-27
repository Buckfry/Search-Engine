
/*
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.2.
 */
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * This software illustrates the architecture for the portion of a search engine
 * that evaluates queries. It is a guide for class homework assignments, so it
 * emphasizes simplicity over efficiency. It implements an unranked Boolean
 * retrieval model, however it is easily extended to other retrieval models. For
 * more information, see the ReadMe.txt file.
 */
public class QryEval {

	// --------------- Constants and variables ---------------------

	private static final String USAGE = "Usage:  java QryEval paramFile\n\n";

	private static final String[] TEXT_FIELDS = { "body", "title", "url", "inlink" };

	// --------------- Methods ---------------------------------------

	/**
	 * @param args
	 *            The only argument is the parameter file name.
	 * @throws Exception
	 *             Error accessing the Lucene index.
	 */
	public static void main(String[] args) throws Exception {
		
		int length = 6;
		String[] exp = new String[length];
		int i=0;
		for(i=0;i<6;i++){
			exp[i] = "exp4_"+i;
		}
//		for(;i<12;i++){
//			exp[i] = "exp3_"+(i-6);
//		}
//		for(;i<18;i++)
//			exp[i] = "exp4_"+(i-12);
		BufferedWriter bw = new BufferedWriter(new FileWriter("Time"));
		for(i=0;i<exp.length;i++){
			Timer timer = new Timer();
			timer.start();
			Map<String, String> parameters = readParameterFile(exp[i]);
			Idx.open(parameters.get("indexPath"));
			RetrievalModel model = initializeRetrievalModel(parameters);
			processQueryFile(parameters, model);
			timer.stop();
			bw.write(exp[i]+" Time:  " + timer+"\n");
		}
		bw.close();
		
		/*
		
		// This is a timer that you may find useful. It is used here to
		// time how long the entire program takes, but you can move it
		// around to time specific parts of your code.

		Timer timer = new Timer();
		timer.start();

		// Check that a parameter file is included, and that the required
		// parameters are present. Just store the parameters. They get
		// processed later during initialization of different system
		// components.

		if (args.length < 1) {
			throw new IllegalArgumentException(USAGE);
		}

		Map<String, String> parameters = readParameterFile(args[0]);

		// Open the index and initialize the retrieval model.

		Idx.open(parameters.get("indexPath"));
		RetrievalModel model = initializeRetrievalModel(parameters);

		// Perform experiments.

		processQueryFile(parameters, model);

		// Clean up.

		timer.stop();
		System.out.println("Time:  " + timer);
		
		*/
		
	}

	/**
	 * Allocate the retrieval model and initialize it using parameters from the
	 * parameter file.
	 * 
	 * @return The initialized retrieval model
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	private static RetrievalModel initializeRetrievalModel(Map<String, String> parameters) throws IOException {

		RetrievalModel model = null;
		String modelString = parameters.get("retrievalAlgorithm").toLowerCase();

		if (modelString.equals("unrankedboolean")) {
			model = new RetrievalModelUnrankedBoolean();
		} else if (modelString.equals("rankedboolean")) {
			model = new RetrievalModelRankedBoolean();
		} else if (modelString.equals("bm25")) {
			double k1 = Double.valueOf(parameters.get("BM25:k_1"));
			double k3 = Double.valueOf(parameters.get("BM25:k_3"));
			double b = Double.valueOf(parameters.get("BM25:b"));
			model = new RetrievalModelBM25(k1, k3, b);
		} else if (modelString.equals("indri")) {
			double lambda = Double.parseDouble(parameters.get("Indri:lambda"));
			double mu = Double.parseDouble(parameters.get("Indri:mu"));
			model = new RetrievalModelIndri(lambda, mu);
		} else {
			throw new IllegalArgumentException("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
		}
		return model;
	}

	/**
	 * Print a message indicating the amount of memory used. The caller can
	 * indicate whether garbage collection should be performed, which slows the
	 * program but reduces memory usage.
	 * 
	 * @param gc
	 *            If true, run the garbage collector before reporting.
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc)
			runtime.gc();

		System.out
				.println("Memory used:  " + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
	}

	/**
	 * Process one query.
	 * 
	 * @param qString
	 *            A string that contains a query.
	 * @param model
	 *            The retrieval model determines how matching and scoring is
	 *            done.
	 * @return Search results
	 * @throws IOException
	 *             Error accessing the index
	 */
	static ScoreList processQuery(String qString, RetrievalModel model) throws IOException {

		String defaultOp = model.defaultQrySopName();
		qString = defaultOp + "(" + qString + ")";
		Qry q = QryParser.getQuery(qString);

		// Show the query that is evaluated

		System.out.println("    --> " + q);

		if (q != null) {

			ScoreList r = new ScoreList();

			if (q.args.size() > 0) { // Ignore empty queries

				q.initialize(model);

				while (q.docIteratorHasMatch(model)) {
					int docid = q.docIteratorGetMatch();
					double score = ((QrySop) q).getScore(model);
					r.add(docid, score);
					q.docIteratorAdvancePast(docid);
				}
			}
			r.sort();
			return r;
		} else
			return null;
	}

	/**
	 * Process the query file.
	 * 
	 * @param queryFilePath
	 * @param model
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	static void processQueryFile(Map<String, String> parameters, RetrievalModel model) throws IOException {
		String queryFilePath = parameters.get("queryFilePath");
		SEWriter.intialize(parameters.get("trecEvalOutputPath"));
		BufferedReader input = null;
		try {
			String qLine = null;
			input = new BufferedReader(new FileReader(queryFilePath));

			// Each pass of the loop processes one query.
			ScoreList r = null;
			String fb = parameters.get("fb");

			// query expansion
			if (fb != null && fb.toLowerCase().equals("true")) {
				FileWriter fw = new FileWriter(parameters.get("fbExpansionQueryFile"));
				String fbInitialRankingFile = parameters.get("fbInitialRankingFile");
				int fbDocs = Integer.parseInt(parameters.get("fbDocs"));
				int fbTerms = Integer.parseInt(parameters.get("fbTerms"));
				double fbMu = Double.parseDouble(parameters.get("fbMu"));
				double fbOrigWeight = Double.parseDouble(parameters.get("fbOrigWeight"));
				BufferedReader expandedQueryFile = new BufferedReader(new FileReader("hw3/Exp3-Indri-expansion_5.qryOut"));
				BufferedReader rankingInput = null;
				if (fbInitialRankingFile != null && fbInitialRankingFile.length() > 0) {
					// read a document ranking in trec_eval input format
					// from the fbInitialRankingFile
					rankingInput = new BufferedReader(new FileReader(fbInitialRankingFile));
				}
				while ((qLine = input.readLine()) != null) {
					int d = qLine.indexOf(':');
					if (d < 0) {
						throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
					}
					printMemoryUsage(false);
					String qid = qLine.substring(0, d);
					String query = qLine.substring(d + 1);
					System.out.println("Query " + qLine);
					String defaultOp = model.defaultQrySopName();
					query = defaultOp + "(" + query + ")";
					String expanded = expandedQueryFile.readLine();
					int idx = expanded.indexOf(':');
					
					query = "#wand(" + fbOrigWeight + " " + query + " " + (1 - fbOrigWeight) + " "
							+ expanded.substring(idx+1) + ")";
					 System.out.println(query);
					r = processQuery(query, model);
					// write results to file in trec_eval format
					writeResults(qid, r);
				}
				fw.close();
				expandedQueryFile.close();
				if (fbInitialRankingFile != null && fbInitialRankingFile.length() > 0)
					rankingInput.close();
			} else {
				while ((qLine = input.readLine()) != null) {
					int d = qLine.indexOf(':');

					if (d < 0) {
						throw new IllegalArgumentException("Syntax error:  Missing ':' in query line.");
					}

					printMemoryUsage(false);

					String qid = qLine.substring(0, d);
					String query = qLine.substring(d + 1);

					System.out.println("Query " + qLine);

					r = processQuery(query, model);
					// write results to file in trec_eval format
					writeResults(qid, r);

					// if (r != null) {
					// printResults(qid, r);
					// System.out.println();
					// }
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			input.close();
		}
		SEWriter.close();
	}

	/**
	 * Write the results in trev_val format
	 * 
	 * @param qid
	 *            query id
	 * @param r
	 *            scoreList of query
	 * @param path
	 *            file's path
	 * 
	 */
	static void writeResults(String qid, ScoreList r) throws IOException {
		StringBuilder sb = new StringBuilder();

		if (r == null) {
			sb.append(qid).append("\t").append("Q0\t").append("dummy\t").append(1).append("\t").append(0).append("\t")
					.append("run-1\n");
		} else {
			int size = Math.min(100, r.size());
			for (int i = 0; i < size; i++) {
				sb.append(qid).append("\t").append("Q0\t").append(r.getExternalDocid(i)).append("\t").append(i + 1)
						.append("\t").append(r.getDocidScore(i)).append("\t").append("run-1\n");
			}
		}
		// System.out.println(sb.toString());
		SEWriter.write(sb.toString());

	}

	/**
	 * Print the query results.
	 * 
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryName
	 *            Original query.
	 * @param result
	 *            A list of document ids and scores
	 * @throws IOException
	 *             Error accessing the Lucene index.
	 */
	static void printResults(String queryName, ScoreList result) throws IOException {

		System.out.println(queryName + ":  ");
		if (result.size() < 1) {
			System.out.println("\tNo results.");
		} else {
			for (int i = 0; i < result.size(); i++) {
				System.out.println(
						"\t" + i + ":  " + Idx.getExternalDocid(result.getDocid(i)) + ", " + result.getDocidScore(i));
			}
		}
	}

	/**
	 * Read the specified parameter file, and confirm that the required
	 * parameters are present. The parameters are returned in a HashMap. The
	 * caller (or its minions) are responsible for processing them.
	 * 
	 * @return The parameters, in <key, value> format.
	 */
	private static Map<String, String> readParameterFile(String parameterFileName) throws IOException {

		Map<String, String> parameters = new HashMap<String, String>();

		File parameterFile = new File(parameterFileName);

		if (!parameterFile.canRead()) {
			throw new IllegalArgumentException("Can't read " + parameterFileName);
		}

		Scanner scan = new Scanner(parameterFile);
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split("=");
			parameters.put(pair[0].trim(), pair[1].trim());
		} while (scan.hasNext());

		scan.close();

		if (!(parameters.containsKey("indexPath") && parameters.containsKey("queryFilePath")
				&& parameters.containsKey("trecEvalOutputPath") && parameters.containsKey("retrievalAlgorithm"))) {
			throw new IllegalArgumentException("Required parameters were missing from the parameter file.");
		}

		return parameters;
	}

}
