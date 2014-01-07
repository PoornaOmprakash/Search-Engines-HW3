/*
 *  Copyright (c) 2013, Carnegie Mellon University.  All Rights Reserved.
 */



import java.io.IOException;

public class QryopScore extends Qryop {

  /**
   * The SCORE operator accepts just one argument.
   */
  public QryopScore(Qryop q) {
    this.args.add(q);
  }

  /**
   * Evaluate the query operator.
   */
  public QryResult evaluate() throws IOException {

    // Evaluate the query argument.
    QryResult result = args.get(0).evaluate();

    // Each pass of the loop computes a score for one document. Note: If the evaluate operation
    // above returned a score list (which is very possible), this loop gets skipped.
    if (result.docScores.scores.size() > 0)
      return result;
  //For BM25
    int df = result.invertedList.df;
    int N = QryEval.READER.getDocCount(result.invertedList.field);
    double idf = Math.log((N - df + 0.5d) / (df + 0.5d));
    // TODO
    double userW = 1.0d;
    
    //For Indri
    int ctf = result.invertedList.ctf;
    long lenC = QryEval.READER.getSumTotalTermFreq(result.invertedList.field);
    long qFreC = QryEval.READER.getDocCount(result.invertedList.field);//.getSumDocFreq(result.invertedList.field);
    double qc = (double)ctf/(double)lenC;
    if (QryEval.retrievalAlgortihm.equals("Indri"))
    {
      if (QryEval.smoothing.equals("df"))
      {
        qc = (double)df/(double)qFreC;
      }
    }
    for (int i = 0; i < result.invertedList.df; i++) {
      
      // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
      if (QryEval.retrievalAlgortihm.equals("UnrankedBoolean")) {
        // Unranked Boolean. All matching documents get a score of 1.0.
        result.docScores.add(result.invertedList.postings.get(i).docid, (float) 1.0);
      } else if (QryEval.retrievalAlgortihm.equals("BM25")) {
        //BM25 use idf*tf*useW formula
        int tf = result.invertedList.postings.get(i).tf;
        long docLen = QryEval.dls.getDocLength(result.invertedList.field, result.invertedList.postings.get(i).docid);
        double avgLen = (double)QryEval.READER.getSumTotalTermFreq(result.invertedList.field)/(double)QryEval.READER.getDocCount(result.invertedList.field);
        double tfW = ((double)tf)/(tf+QryEval.k1*(1-QryEval.b+QryEval.b*docLen/avgLen));
        result.docScores.add(result.invertedList.postings.get(i).docid, (float)(idf*tfW*userW));
      } else if (QryEval.retrievalAlgortihm.equals("Indri")) {
        //Indri
        double first = QryEval.lambda;
        first *= result.invertedList.postings.get(i).tf + QryEval.mu*qc;
        first /= QryEval.dls.getDocLength(result.invertedList.field, result.invertedList.postings.get(i).docid) + QryEval.mu;
        double second = (1-QryEval.lambda)*qc;
        double score = Math.log(first+second);
        result.docScores.add(result.invertedList.postings.get(i).docid, (float)score);
      } else {
        // Ranked boolean, use tf as score.
        result.docScores.add(result.invertedList.postings.get(i).docid,
                result.invertedList.postings.get(i).tf);
      }
    }

    String field = result.invertedList.field;
    double avgLen = (double) QryEval.READER.getSumTotalTermFreq(field)
            / (double) QryEval.READER.getDocCount(field);
    double defaultScore = 0;
    if (QryEval.retrievalAlgortihm.equals("Indri"))
    {
      defaultScore = (QryEval.lambda * QryEval.mu * qc);
      defaultScore /= avgLen + QryEval.mu;
      defaultScore += (1 - (QryEval.lambda) * qc);
      defaultScore = Math.log(defaultScore);
    }
    result.docScores.default_score = (float) defaultScore;
    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.
    if (result.invertedList.df > 0)
    {
      result.invertedList = new InvList();
      result.invertedList.field = field;
    }

    return result;
  }
}


/*import java.io.IOException;

public class QryopScore extends Qryop {
  /**
   * The SCORE operator accepts just one argument.
   *
  public QryopScore(Qryop q) {
    this.args.add(q);
  }

  /**
   * Evaluate the query operator.
   *
  public QryResult evaluate() throws IOException {

    // Evaluate the query argument.
    QryResult result = args.get(0).evaluate();
    
    int df = result.invertedList.df;
	int ctf = result.invertedList.ctf;
    long length_c = QryEval.READER.getSumTotalTermFreq(result.invertedList.field);
    long qryFreqCorpus = QryEval.READER.getDocCount(result.invertedList.field);
    double qc = (double)ctf/(double)length_c;
    if(QryEval.retrievalAlgortihm.equals("Indri")) 
    {
    if (QryEval.smoothing.equals("df"))
      {
        qc = (double)df/(double)qryFreqCorpus;
      }
    }

    for (int i = 0; i < result.invertedList.df; i++) {

    	if(QryEval.retrievalAlgortihm.equals("RankedBoolean"))
    	 result.docScores.add(result.invertedList.postings.get(i).docid,result.invertedList.postings.get(i).tf);
    	else if(QryEval.retrievalAlgortihm.equals("RankedBoolean"))
    	 result.docScores.add(result.invertedList.postings.get(i).docid, (float) 1.0);
    	
    	else if(QryEval.retrievalAlgortihm.equals("BM25"))
        {
        //System.out.println(i);
        float score;
        float total_doclen=0;
        float doclen=QryEval.dls.getDocLength("body", result.invertedList.postings.get(i).docid);
        total_doclen=QryEval.READER.getSumTotalTermFreq("body");
        float avg_doclen=total_doclen/QryEval.READER.getDocCount("body");
        score = (float)(Math.log((QryEval.N-result.invertedList.df+0.5)/(result.invertedList.df+0.5)) * (result.invertedList.postings.get(i).tf/(result.invertedList.postings.get(i).tf+QryEval.k1*((1-QryEval.b)+QryEval.b*(doclen/avg_doclen))))*((QryEval.k3+1)*1)/(QryEval.k3+1));
        //System.out.println(score);
        result.docScores.add(result.invertedList.postings.get(i).docid, score);
        }
        
        else if(QryEval.retrievalAlgortihm.equals("Indri"))
        {
        	double score=0;
        	int tf=result.invertedList.postings.get(i).tf;
        	double length_d=QryEval.dls.getDocLength(result.invertedList.field,result.invertedList.postings.get(i).docid);
        	score=(QryEval.lambda*((tf+(QryEval.mu*(ctf/length_c)))/(length_d+QryEval.mu)));
            score+=((1-QryEval.lambda)*(ctf/length_c));
            score=Math.log(score);
            result.docScores.add(result.invertedList.postings.get(i).docid,(float)score);
        }	
    }
    
   
    double default_score = 0;
    if (QryEval.retrievalAlgortihm.equals("Indri"))
    {
      int tf=0;
      double length_d = (double)QryEval.READER.getSumTotalTermFreq(result.invertedList.field)/(double)QryEval.READER.getDocCount(result.invertedList.field);
      //System.out.println(result.invertedList.field);
      default_score=(QryEval.lambda*((tf+(QryEval.mu*(ctf/length_c)))/(length_d+QryEval.mu)));
      default_score+=((1-QryEval.lambda)*(ctf/length_c));
      default_score=Math.log(default_score);
    }
    //result.docScores.default_score=(float)default_score;
    result.docScores.default_score=(float)default_score;
    

    // BEGINNING OF NEW CODE
    // The SCORE operator should not return a populated inverted list.
    // If there is one, replace it with an empty inverted list.
    if (result.invertedList.df > 0)
    result.invertedList = new InvList();
    // END OF NEW CODE
    return result;

   }

} */
