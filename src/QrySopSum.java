import java.io.IOException;

public class QrySopSum extends QrySop{

	@Override
	public double getScore(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		double score = 0;
		if(this.docIteratorHasMatch(r)){
			int docId = this.docIteratorGetMatch();
			for(Qry q: this.args){
				if(q.docIteratorHasMatch(r)&&docId == q.docIteratorGetMatch())
					score += ((QrySop)q).getScore(r);
			}
			return score;
		}
		return 0;
	}

	@Override
	public boolean docIteratorHasMatch(RetrievalModel r) {
		// TODO Auto-generated method stub
		return this.docIteratorHasMatchMin(r);
	}

	@Override
	public double getDefaultScore(RetrievalModel r, int docId) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
