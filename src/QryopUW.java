import java.io.IOException;

import java.util.ArrayList;
import java.util.Vector;


public class QryopUW extends Qryop{
	  public int near;
	  
	  public int getMinIndex()    //For DocID
	  {
	    int index = 0;  //Index needs to be something initially
	    //System.out.println(tracker_list.size());
	    for (int i = 0; i < tracker_list.size(); i++) 
	    {
	      if (tracker_list.get(i).dp.docid < tracker_list.get(index).dp.docid || i == 0)
	      {
	        index = i;
	      }
	    }
	    return index;
	  }
	  
	  public int getMaxIndex()
	  {   //Also for DocID
		    int index = 0;
		    for (int i = 0; i < tracker_list.size(); i++) {
		      if (tracker_list.get(i).dp.docid > tracker_list.get(index).dp.docid ||i==0)
		      {
		        index = i;
		      }
		    }
		    //System.out.println(index);
		    return index;
	  }


	  public int getMinPosition()
	  {    //For position within a posting
	    int index = 0;
	    for (int i = 0; i < tracker_list.size(); i++) {
	      if (getPosition(i) < getPosition(index)||i == 0) {
	        index = i;
	      }
	    }
	    return index;
	  }

	  public int getMaxPosition()
	  {           //For position within a posting
	    int index = 0;
	    for (int i = 0; i < tracker_list.size(); i++) 
	    {
	      if (getPosition(i) > getPosition(index)||(i==0))
	      {
	        index = i;
	      }
	    }
	    //System.out.println(index);
	    return index;
	  }

	  
	  public class Tracker    //To keep track of inverted lists of all query terms. This is where the checking happens.
	  {   
		  
	    InvList tracker_invlist;
	    InvList.DocPosting dp;
	    int docIndex;
	    int positionIndex;
	    
	    Tracker()
	    {
	    	tracker_invlist=null;
	    	dp=null;
	    	docIndex=0;
	    	positionIndex=0;
	    }
	  }

	  ArrayList<Tracker> tracker_list = null;   //To keep track of inverted lists

	  public QryopUW(int near)
	  {
	    this.near = near;
	  }

	 
	  public QryResult evaluate() throws IOException {

		boolean check;
		
	    QryResult result = new QryResult();
	    result.invertedList = new InvList();  //This is what we want - return a QryResult type.
	    tracker_list = new ArrayList<Tracker>();
	    //System.out.println(args.size());
	    for (int i=0;i<args.size(); i++)    //Initialization
	    {
	      //System.out.println(i);
	      Tracker t = new Tracker();
	      t.tracker_invlist = args.get(i).evaluate().invertedList;
	      if (i==0) 
	    	  result.invertedList.field=t.tracker_invlist.field;   //#UW terms must be in the same field.
	      t.positionIndex=0;
	      t.docIndex=0;
	      t.dp=t.tracker_invlist.postings.get(t.docIndex);
	      tracker_list.add(t);
	      //System.out.println(tracker_list.size());
	    }
	  
	    int i;
	    
	    
	    //Mess-up was here. These terms cannot come earlier, since the size of tracker_list is not yet known. Meh!
	    
	    int minIndex = getMinIndex();  //For DocID
		int maxIndex = getMaxIndex();  //Also for DocID
	  
	    while (tracker_list.get(maxIndex).docIndex < tracker_list.get(maxIndex).tracker_invlist.postings.size())
	    {
	      if (tracker_list.get(minIndex).dp.docid==tracker_list.get(maxIndex).dp.docid)   //Same document in both lists 
	      {
	    	check=true;
	    	  
	        int minPosition=getMinPosition();
	        int maxPosition=getMaxPosition();
	        
	       
	        
	        while (tracker_list.get(maxPosition).positionIndex < tracker_list.get(maxPosition).dp.positions.size())
	        {
	          if (getPosition(maxPosition)-getPosition(minPosition)+1 <= near)   //There is a match within the window size.
	          {
	            if (check)
	            {
	              result.invertedList.addPosting(tracker_list.get(minPosition).dp.docid, getPosition(minPosition));  //Add a posting into result's InvList.
	              check = false;
	            }
	            else 
	            {
	            //	System.out.println(getPosition(minPosition));
	              result.invertedList.insertInPosting(getPosition(minPosition));
	            }
	           
	            //System.out.println(tracker_list.size());
	            for (i = 0; i < tracker_list.size(); i++)
	            {
	              //System.out.println(i);
	              tracker_list.get(i).positionIndex++;
	              if (tracker_list.get(i).positionIndex >= tracker_list.get(i).dp.positions.size()) {
	                   break;
	              }
	            }
	            
	            if (i < tracker_list.size()) {
	              break;
	            }
	            
	            minPosition = getMinPosition();
	            maxPosition = getMaxPosition();
	          } 
	          
	          else
	          {
	            tracker_list.get(minPosition).positionIndex++;
	            if (tracker_list.get(minPosition).positionIndex >= tracker_list.get(minPosition).dp.positions.size()) {
	                break;
	            }
	            
	            if (getPosition(minPosition) > getPosition(maxPosition))
	            {
	              maxPosition = minPosition;
	            }
	            minPosition = getMinPosition();
	            //System.out.println(minPosition);
	          }
	        }
	        

	        for (i = 0; i < tracker_list.size(); i++)    //Index of the document needs to be updated here.
	        {
	      //  System.out.println(tracker_list.docIndex);
	          tracker_list.get(i).docIndex++;
	          tracker_list.get(i).dp = getPosting(i);
	          if (tracker_list.get(i).dp == null)     //Necessary to check for this condition.
	                break;
	        }
	        
	        if (i < tracker_list.size())
	        {   break;  }
	        
	        this.reset();
	       
	        minIndex = getMinIndex();
	        maxIndex = getMaxIndex();
	   
	       
	      } 
	      else
	      {
	    	  
	    	 // System.out.println(minIndex);
	    	tracker_list.get(minIndex).dp = getPosting(minIndex);
	        tracker_list.get(minIndex).docIndex++;
	        
	        if (tracker_list.get(minIndex).dp == null)
	              break;
	        tracker_list.get(minIndex).positionIndex = 0;
	        if (tracker_list.get(minIndex).dp.docid> tracker_list.get(maxIndex).dp.docid) 
	        {
	          maxIndex=minIndex;
	        }
	        minIndex = getMinIndex();
	        //System.out.println(minIndex);
	      }
	    }
	    return result;
	  }

	  
	  public void reset()            //This is what was needed!
	  {
		    for (int i = 0; i < tracker_list.size(); i++)
		    {
		      tracker_list.get(i).positionIndex=0;
		    }
	}

	  
	  public InvList.DocPosting getPosting(int i)   
	  {
	    Tracker t = tracker_list.get(i);
	    if (t.docIndex >= t.tracker_invlist.postings.size())
	        return null;
	    return t.tracker_invlist.postings.get(t.docIndex);
	  }

	  public int getPosition(int i) {    //Added this function because it is too cumbersome to calculate the value in every place
	    Tracker t = tracker_list.get(i);
	    int pos = t.dp.positions.get(t.positionIndex);
	    return pos;
	    //tracker_list.gett(i).dp.positions.get(tracker_list.get(i).positionIndex)
	  }
}
