/*
 * Project 1: Luby MIS
 *
 * @Authors
 * Biranchi Narayan Padhi  - bxp200001
 * Manasa M Bhat           - mmb190005
 * Siddarameshwar Kadagad  -  sxk190071
 *
 * Contribution---------------------------------:
 * @Manasa M Bhat
 * -Threads and Process initialization
 *
 * @Biranchi Narayan Padhi
 * -Input file extraction
 * -Add neighbors to processes from adjacency matrix
 *
 * @Siddarameshwar Kadagad
 * -Adjacency list creation for sequentialMIS
 * -Output verification result for sequentialMIS
 * -----------------------------------------------
 * */
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

public class MISMain {

    public static void main(String args[]) {

        int input_num_of_processes;
        Thread[] threads;
        LubyMIS.Process[] processes;
        int[][] adj_list;
        int[] uidArray;
        Scanner scanFile;

        if (args.length == 1) {
            // get input from a file
            try {
                scanFile = new Scanner(new File(args[0]));
            } catch (FileNotFoundException e) {
                System.out.println("Error while opening the input file : " + args[0]);
                return;
            }

        } else {
            scanFile = new Scanner(System.in);
        }

        input_num_of_processes = scanFile.nextInt();
        threads = new Thread[input_num_of_processes];
        processes = new  LubyMIS.Process[input_num_of_processes];

        // creating a UIDArray
        uidArray = new int[input_num_of_processes];

        // 2D array to store adjacency matrix represented by 1 or 0.
        adj_list = new int[input_num_of_processes][input_num_of_processes];

        for (int i = 0; i < input_num_of_processes; i += 1) {
            uidArray[i] = Integer.parseInt(scanFile.next());
        }

        // creating n no of process wand assigning each process an UID from UIDArray
        for (int i = 0; i < input_num_of_processes; i++) {
            processes[i] = new  LubyMIS.Process(uidArray[i], input_num_of_processes);
            threads[i] = new Thread(processes[i], i + "_thread");
        }

        // 2D array to store adjacency matrix represented by 1 or 0.
        for (int row = 0; row < input_num_of_processes; row++) {
            for (int col = 0; col < input_num_of_processes; col += 1) {
                adj_list[row][col] = Integer.parseInt(scanFile.next());
                if (adj_list[row][col] == 1) {
                    processes[row].setNeighbor(processes[col]);
                    processes[row].setNeighbor(threads[col]);
                }
            }
        }
        System.out.println("---------------------------------------------------------------------");
        System.out.println("Luby MIS - an algorithm to find MIS in Synchronous Distributed System");
        System.out.println("---------------------------------------------------------------------");
        HashSet<Integer> MISset = LubyMIS.masterThread(input_num_of_processes,threads);

        //index of MISSet processes in the uidArray
        List<Integer> indexes = new ArrayList<>();
        for (int node:MISset){

               for(int i=0; i<uidArray.length; i++){
                   if(node == uidArray[i]){
                       indexes.add(i);
                       break;
                   }
               }
        }

        //creating a Graph with the nodes of distributed MISSet result to verifiy the result from the sequential MISSet
        HashMap<Integer, List<Integer>> graph = new HashMap<Integer, List<Integer>>();
        for (int node:indexes) {
            List<Integer> neighbors = new ArrayList<Integer>();
            for (int col = 0; col < input_num_of_processes; col += 1) {
                if (adj_list[node][col] == 1) {
                    neighbors.add(col);
                }
            }
            graph.put(node, neighbors);
        }

        //calling the sequentialMISSet funtion in order to verify the result from Distributed MIS
        List<Integer> sequentialMISSet = sequentialMIS.sequentialMISSet(graph);
        List<Integer> sequentialMISOutput = new ArrayList<>();
        for(int s: sequentialMISSet){
            sequentialMISOutput.add(uidArray[s]);
        }
        System.out.println("Sequential MIS output:" + sequentialMISOutput);
        int flag=0;
        for(int index:indexes){
            if(!sequentialMISSet.contains(index)){
                System.out.println("Verification Failed! Output did not match from sequential MIS!");
                flag=1;
                break;
            }
        }
        if (flag == 0){
            System.out.println("Verification Succeeded! Output from DistributedMIS and SequentialMIS are Equal!");
        }
        System.out.println("----------------------------------------------------------------------------");
        
    }
}
