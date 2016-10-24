import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class QryIopWindow extends QryIop{
	private int distance;

	/**
	 * constructor
	 * 
	 * @param distance
	 */
	public QryIopWindow(int distance) {
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
			List<Integer> positions = new LinkedList<>();
			boolean eval = true;
			while (eval) {
				QryIop q0 = (QryIop) this.args.get(0);
				if(!q0.locIteratorHasMatch())
					break;
				int preLoc = q0.locIteratorGetMatch();
				int minLoc = preLoc;
				int maxLoc = preLoc;
				List<Integer> window = new LinkedList<>();
				window.add(preLoc);
				boolean match = true;
				for (int i = 1; i < this.args.size(); i++) {
					QryIop qi = (QryIop) this.args.get(i);
//					qi.locIteratorAdvancePast(preLoc);
					if (!qi.locIteratorHasMatch()) {
						eval = false;
						match = false;
						break;
					}
					int nowLoc = qi.locIteratorGetMatch(); 
					minLoc = Math.min(nowLoc, minLoc);
					maxLoc = Math.max(nowLoc,maxLoc);
					for(int j=0;j<window.size();j++){
						if (Math.abs(window.get(j) - nowLoc) >= this.distance) {
							match = false;
							break;
						}
					}
					window.add(nowLoc);
				}
				if (match) {
					positions.add(maxLoc);
					for (int i = 0; i < this.args.size(); i++) {
						((QryIop) this.args.get(i)).locIteratorAdvance();
					}
				}
				else{
					for (int i = 0; i < this.args.size(); i++) {
						((QryIop) this.args.get(i)).locIteratorAdvancePast(minLoc);
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
