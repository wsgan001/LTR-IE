package pt.utl.ist.online.learning.engines;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.columbia.cs.ltrie.features.FeaturesCoordinator;

import pt.utl.ist.online.learning.exceptions.InvalidVectorIndexException;
import pt.utl.ist.online.learning.utils.DataObject;
import pt.utl.ist.online.learning.utils.Pair;


public class ElasticNetLinearPegasosSpecialEngine<E> implements LinearOnlineEngine<E> {

	private Map<E,Double> weights;
	private double b=0.0;
	private double hingeParameter;
	private double lambda;
	private double importanceL2;
	private double t=0;
	private double normSq=0.0;
	private boolean useB;
	private double previousMult=1.0;
	
	public ElasticNetLinearPegasosSpecialEngine() {
	}

	public ElasticNetLinearPegasosSpecialEngine(double lambda, double importanceL2, double hingeParameter) {
		this(lambda,importanceL2,hingeParameter,true);
	}

	public ElasticNetLinearPegasosSpecialEngine(double lambda, double importanceL2, double hingeParameter, boolean useB) {
		this.lambda=lambda;
		this.importanceL2=importanceL2;
		this.hingeParameter=hingeParameter;
		weights = new HashMap<E, Double>();
		this.useB=useB;
	}

	public boolean updateModel(DataObject<Map<E, Double>> inputVector, boolean desiredOutput){
		t++;
		double y=1.0;
		if(!desiredOutput){
			y=-1.0;
		}
		double inner = innerProduct(weights, inputVector.getData())*previousMult;
		
		double learning = 1.0/(t*lambda);
		double weightVectorconstant = (1-learning*importanceL2*lambda);
		
		for(Entry<E,Double> entryX1 : weights.entrySet()){
			entryX1.setValue(entryX1.getValue()*weightVectorconstant*previousMult);
		}
		b*=previousMult;
		normSq*=weightVectorconstant*weightVectorconstant;
		updateWeights(inputVector,learning,(1-importanceL2)*lambda,y,inner);
		double multConstant=Math.min(1.0,1.0/(Math.sqrt(normSq*lambda)));
		if(normSq==0){
			multConstant=0;
		}
		previousMult=multConstant;
		normSq*=multConstant*multConstant;
		
		return true;
	}

	private void updateWeights(DataObject<Map<E, Double>> inputVector, double learning, double lambda, double y, double inner) {
		double inputVectorConstant= learning*y;
		boolean toUseInputVector = y*(inner-b)<=hingeParameter;
		for(Entry<E,Double> entry : inputVector.getData().entrySet()){
			double xVal = entry.getValue();
			Double newWi = weights.get(entry.getKey());
			boolean isNewValue=false;
			if(newWi==null){
				newWi=0.0;
				isNewValue=true;
			}
			double originalValue=newWi;
			if(toUseInputVector){
				newWi+=inputVectorConstant*xVal;
			}
			
			applyPenalty(entry.getKey(),newWi,isNewValue,originalValue, learning*lambda);
		}
		if(toUseInputVector && useB){
			b-=y*learning;
		}
	}

	private void applyPenalty(E key, double newWi,boolean isNewValue, double originalValue, double penalty) {
		double z = newWi;
		if(newWi>0){
			newWi=Math.max(0, newWi-penalty);
		}else if(newWi<0){
			newWi=Math.min(0, newWi+penalty);
		}
		
		double diff=newWi-originalValue;
		if(newWi!=0){
			if(diff!=0){
				weights.put(key, newWi);
			}
		}else if(!isNewValue){
			weights.remove(key);
		}
		normSq+=2*diff*originalValue+diff*diff;
		if(normSq<0){
			normSq=0;
		}
	}

	public boolean getResult(DataObject<Map<E, Double>> vector){
		double currentResult = previousMult*innerProduct(weights, vector.getData())-previousMult*b;
		if(currentResult>0){
			return true;
		}else{
			return false;
		}
	}

	@Override
	public double getScore(DataObject<Map<E, Double>> vector) {
		double inner = previousMult*innerProduct(weights, vector.getData());
		return inner-previousMult*b;
	}

	private double innerProduct(Map<E,Double> x1,Map<E,Double> x2){
		int x1Size = x1.size();
		int x2Size = x2.size();
		if(x2Size<x1Size){
			Map<E,Double> temp=x1;
			x1=x2;
			x2=temp;
		}

		double result = 0;
		for(Entry<E,Double> entryX1 : x1.entrySet()){
			Double valX2 = x2.get(entryX1.getKey());
			if(valX2!=null){
				result+=entryX1.getValue()*valX2;
			}
		}
		return result;
	}

	public DataObject<Map<E, Double>> convertVector(DataObject<Map<E, Double>> x) throws InvalidVectorIndexException{
		return x;
	}

	@Override
	public void compileModel() {
		double totalSum=0.0;
		int numFeatures=weights.size();
		double maxValue=Double.MIN_VALUE;
		double minValue=Double.MAX_VALUE;
		double absoluteMin=Double.MAX_VALUE;
		for(Entry<E,Double> entry : weights.entrySet()){
			totalSum+=entry.getValue();
			maxValue=Math.max(maxValue, entry.getValue());
			minValue=Math.min(minValue, entry.getValue());
			absoluteMin=Math.min(absoluteMin, Math.abs(entry.getValue()));
		}
		System.out.println("Total Sum = " + totalSum);
		System.out.println("Average = " + totalSum/numFeatures);
		System.out.println("Num Features = " + numFeatures);
		System.out.println("Max value = " + maxValue);
		System.out.println("Min value = " + minValue);
		System.out.println("Absolute min = " + absoluteMin);
		//System.out.println(qi);
	}

	@Override
	public void start() {
	}

	@Override
	public void objectiveReport(Map<Integer, DataObject<Map<E, Double>>> trainingData, Map<Integer, Boolean> desiredLabels){
		// TODO Auto-generated method stub

	}

	@Override
	public Map<E, Double> getWeightVector() {
		if(previousMult!=1){
			for(Entry<E,Double> entry : weights.entrySet()){
				entry.setValue(entry.getValue()*previousMult);
			}
			b*=previousMult;
			previousMult=1;
		}
		return weights;
	}

	@Override
	public double getRho() {
		return b;
	}
	
	@Override
	public LinearOnlineEngine<E> copy() {
		ElasticNetLinearPegasosSpecialEngine<E> newEngine = new ElasticNetLinearPegasosSpecialEngine<E>();
		newEngine.weights=new HashMap<E, Double>(weights);
		newEngine.b=b;
		newEngine.hingeParameter=hingeParameter;
		newEngine.lambda=lambda;
		newEngine.importanceL2=importanceL2;
		newEngine.t=t;
		newEngine.normSq=normSq;
		newEngine.useB=useB;
		newEngine.previousMult=previousMult;
				
		return newEngine;
	}

}
