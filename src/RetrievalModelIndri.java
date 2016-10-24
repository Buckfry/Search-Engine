
public class RetrievalModelIndri extends RetrievalModel{

	private double lambda;
	private double mu;
	
	public RetrievalModelIndri(double lambda,double mu) {
		// TODO Auto-generated constructor stub
		this.lambda = lambda;
		this.mu = mu;
	}
	
	public double getLambda(){
		return lambda;
	}
	
	public double getMu(){
		return mu;
	}
	
	
	@Override
	public String defaultQrySopName() {
		// TODO Auto-generated method stub
		return "#and";
	}

}
