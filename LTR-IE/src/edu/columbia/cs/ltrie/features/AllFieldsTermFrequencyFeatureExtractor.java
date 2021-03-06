package edu.columbia.cs.ltrie.features;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

import pt.utl.ist.online.learning.utils.Pair;

import edu.columbia.cs.ltrie.datamodel.DocumentWithFields;
import edu.columbia.cs.ltrie.indexing.IndexConnector;

public class AllFieldsTermFrequencyFeatureExtractor extends FeatureExtractor {
	private static final String LABEL = "TF";
	private IndexConnector conn;
	private String[] fields;
	private String label;
	private boolean computeAbsolute;
	private boolean computeRelative;
	private boolean computeBoolean;
	private final Double ONE = new Double(1.0);
	private Map<Integer,Double> frequencies = new HashMap<Integer,Double>();

	public AllFieldsTermFrequencyFeatureExtractor(IndexConnector conn, Set<String> fields){
		this(conn,fields,true,true,true);
	}

	public AllFieldsTermFrequencyFeatureExtractor(IndexConnector conn, Set<String> fields,
			boolean computeAbsolute, boolean computeRelative,
			boolean computeBoolean){
		this.conn=conn;
		this.fields=new String[fields.size()];
		int i=0;
		for(String field : fields){
			this.fields[i]=field;
			i++;
		}
		this.label=LABEL + "_ALL";
		this.computeAbsolute=computeAbsolute;
		this.computeRelative=computeRelative;
		this.computeBoolean=computeBoolean;
	}
	

	public Map<String,Double> extractFeatures(String doc){
		Map<String,Double> d = new HashMap<String, Double>();
		try {
			
			Map<String,Integer> frequencies = new HashMap<String, Integer>();
			for(String field : fields){
				Map<String, Integer> fieldFrequencies = conn.getTermFrequencies(doc, field);
				for(Entry<String,Integer> termFrequency : fieldFrequencies.entrySet()){
					Integer num = frequencies.get(termFrequency.getKey());
					if(num==null){
						num=0;
					}
					frequencies.put(termFrequency.getKey(), num+termFrequency.getValue());
				}
			}
			double sum = 0.0;
			if(computeRelative){
				for(Integer val : frequencies.values()){
					sum+=val;
				}
			}
			for(Entry<String,Integer> entry : frequencies.entrySet()){
				int value = entry.getValue();
				if(computeAbsolute){
					Double v = this.frequencies.get(value);
					if(v==null){
						v=(double) value;
						this.frequencies.put(value, v);
					}

					d.put(label + "_ABS_" + entry.getKey(), v);
				}
				if(computeRelative){
					d.put(label + "_REL_" + entry.getKey(), value/sum);
				}
				if(computeBoolean){
					d.put(label + "_BOO_" + entry.getKey(), ONE);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(1);
		}

		return d;
	}

	@Override
	public Pair<String,String> getTerm(String term) {
		String[] splitedKey = term.split("_");
		String field=null;
		String value=null;
		try{
			field = "*";
			value = splitedKey[3];
		}catch(ArrayIndexOutOfBoundsException aiobe){
			System.out.println("Problem:");
			System.out.println("Term: " + term);
			System.out.println("Splited key: " + Arrays.toString(splitedKey));
			System.exit(1);
		}
		//if(splitedKey[2].equals("BOO")){
		return new Pair<String,String>(field,value);
		//}
		//return null;
	}

	@Override
	public Query getQuery(String term) {
		String[] splitedKey = term.split("_");
		String field=null;
		String value=null;
		try{
			value = splitedKey[3];
			MultiFieldQueryParser queryParser = new MultiFieldQueryParser(
	                Version.LUCENE_41, 
	                fields,
	                new StandardAnalyzer(Version.LUCENE_41));
			Query query = queryParser.parse("+\"" + value + "\"");
			return query;
		}catch(ArrayIndexOutOfBoundsException aiobe){
			System.out.println("Problem:");
			System.out.println("Term: " + term);
			System.out.println("Splited key: " + Arrays.toString(splitedKey));
			System.exit(1);
		} catch (ParseException e) {
			
		}
		return null;
	}
}
