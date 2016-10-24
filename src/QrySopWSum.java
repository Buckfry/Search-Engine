import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QrySopWSum extends QrySop{
	private List<Double> weights = new ArrayList<>();
	
	public void addWeight(double weight){
		weights.add(weight);
	}
	
	public double weightSum(){
		double sum = 0;
		for(double weight:weights){
			sum += weight;
		}
		return sum;
	}
	
	@Override
	public double getScore(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		if(!docIteratorHasMatchCache())
			return 0;
		double score = 0;
		int docId = docIteratorGetMatch();
//		double weightSum = weightSum();
		for(int i=0;i<args.size();i++){
			QrySop q = (QrySop) args.get(i);
			double weight = weights.get(i);
//			double tempScore;
			if(q.docIteratorHasMatch(r) && q.docIteratorGetMatch()==docId){
				score += weight*q.getScore(r);
//				tempScore = weight*q.getScore(r);
			}
			else{
				score += weight*q.getDefaultScore(r, docId);
//				tempScore = weight*q.getDefaultScore(r, docId);
			}
//			score += tempScore/weightSum;
		}
		return score/weightSum();
//		return score;
	}

	@Override
	public double getDefaultScore(RetrievalModel r, int docId) throws IOException {
		// TODO Auto-generated method stub
		if(!docIteratorHasMatchCache())
			return 0;
		double score = 0;
//		double weightSum = weightSum();
		for(int i=0;i<args.size();i++){
			QrySop q = (QrySop)args.get(i);
			double weight = weights.get(i);
			score += weight*q.getDefaultScore(r, docId);
//			score += weight/weightSum*q.getDefaultScore(r, docId);
		}
		return score/weightSum();
//		return score;
	}

	@Override
	public boolean docIteratorHasMatch(RetrievalModel r) {
		// TODO Auto-generated method stub
		return docIteratorHasMatchMin(r);
	}

}
