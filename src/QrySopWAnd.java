import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class QrySopWAnd extends QrySop{

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
		if(!this.docIteratorHasMatchCache()||args.size()==0)
			return 0;
		int docId = this.docIteratorGetMatch();
		double score = 1;
		for(int i=0;i<args.size();i++){
			QrySop q = (QrySop) args.get(i);
			double weight = weights.get(i);
			if(q.docIteratorHasMatch(r)&&q.docIteratorGetMatch()==docId){
				score *= Math.pow(q.getScore(r), weight);
			}
			else{
				score *= Math.pow(q.getDefaultScore(r, docId), weight);
			}
			
		}
		score = Math.pow(score, 1/weightSum());
		return score;
	}

	@Override
	public boolean docIteratorHasMatch(RetrievalModel r) {
		// TODO Auto-generated method stub
		return this.docIteratorHasMatchMin(r);
	}
	
	@Override
	public double getDefaultScore(RetrievalModel r, int docId) throws IOException{
		if(!docIteratorHasMatchCache())
			return 0;
		double score = 1;
		for(int i=0;i<args.size();i++){
			QrySop q = (QrySop)args.get(i);
			double weight = weights.get(i);
			score *= Math.pow(q.getDefaultScore(r, docId), weight);
		}
		score = Math.pow(score, 1/weightSum());
		return score;
		
	}
}
