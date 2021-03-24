/*
 * Project 1: Luby MIS
 *
 * @Authors
 * Biranchi Narayan Padhi  - bxp200001
 * Manasa M Bhat           - mmb190005
 * Siddarameshwar Kadagad  -  sxk190071
 * */
import java.util.*;
public class sequentialMIS {

    public static List<Integer> sequentialMISSet(HashMap<Integer,List<Integer>> graph){

        if (graph.size() == 0){
            return Collections.emptyList();
        }

        if (graph.size() == 1){
            List<Integer> result= new ArrayList<Integer>();
            for(Integer key: graph.keySet()){
                result.add(key);
            }
            return result;
        }

        int currentVertex = (int) graph.keySet().toArray()[0];
        
        HashMap<Integer,List<Integer>> graph2 = (HashMap) graph.clone();
        graph2.remove(currentVertex);
        List<Integer> result1 = sequentialMISSet(graph2);

      
        for(Integer neighbor:graph.get(currentVertex)){
            if(graph2.get(neighbor) != null){
                graph2.remove(neighbor);
            }
        }
        
        List<Integer> result2 = new ArrayList<Integer>();
        result2.add(currentVertex);

        List<Integer> mymis = sequentialMISSet(graph2);
        result2.addAll(mymis);

        //System.out.println(result2);
      
        if(result1.size()>result2.size()){
            return result1;
        }
        return result2;
    }

}