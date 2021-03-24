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

        HashSet<Integer> MISset = LubyMIS.masterThread(input_num_of_processes,threads);

        //creating a Graph with the nodes of distributed MISSet result to verifiy the result from the sequential MISSet
        HashMap<Integer, List<Integer>> graph = new HashMap<Integer, List<Integer>>();
        for (int node:MISset) {
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
        int flag=0;
        for(int node:MISset){
            if(!sequentialMISSet.contains(node)){
                System.out.println("Verification Failed! Output did not match from sequential MIS!");
                flag=1;
                break;
            }
        }
        if (flag == 0){
            System.out.println("Verification Succeded! Output from DistributedMIS and SequentialMIS are Equal!");
        }
        
    }
}
