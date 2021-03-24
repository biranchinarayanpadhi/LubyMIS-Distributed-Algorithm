
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class LubyMIS {

    private static Semaphore roundSem = new Semaphore(1);
    private static Semaphore sendIdToNeighborsSem = new Semaphore(1);
    private static Semaphore compareRecievedIdsFromNeighborsSem = new Semaphore(1);
    private static CountDownLatch latch;
    public static final AtomicInteger waitCounter = new AtomicInteger(0);
    public static final AtomicInteger activeProcesses = new AtomicInteger(0);
    private static HashSet<Integer> MISSet;
    private static final AtomicInteger totalRounds = new AtomicInteger(0);
    private static final AtomicBoolean incrementRound = new AtomicBoolean(false);

    public static class Process implements Runnable {
        private int processId;
        private int n;
        private ArrayList<Process> neighbors;
        private ArrayList<Thread> neighborThreads;
        private int randomId;
        private boolean neighborInMIS = false;
        private ArrayList<Integer> recivedIdsforProcess;
        Random random = new Random();

        public Process(int id, int num) {
            processId = id;
            n = num;
            neighbors = new ArrayList<>();
            neighborThreads = new ArrayList<>();
            recivedIdsforProcess = new ArrayList<>();
        }

        public ArrayList<Process> getNeighbors() {
            return neighbors;
        }

        public void setNeighbor(Process neighbor) {
            neighbors.add(neighbor);
        }

        public void setNeighbor(Thread neighborThread) {
            neighborThreads.add(neighborThread);
        }

        public ArrayList<Integer> getRecievedIdsForProcess() {
            return recivedIdsforProcess;
        }

        public void setRecievedIdsForProcess(int id) {
            recivedIdsforProcess.add(id);
        }

        public void setEmpty() {
            getRecievedIdsForProcess().clear();
        }

        public void setNeighborInMIS() {
            if (!neighborInMIS) {
                // System.out.println("ProcessId "+processId+" Received Done message.");
                neighborInMIS = true;
            }
        }

        public void sendMessageToNeighbor() {

            for (int i = 0; i < neighborThreads.size(); i++) {
                if (neighborThreads.get(i).isAlive()) {
                    // System.out.println("Sending Done message to neighbour: "+ neighborThreads.get(i).getName());
                    neighbors.get(i).setNeighborInMIS();
                }
            }
        }

        public void waitForAllProcesses() {
            waitCounter.getAndIncrement();
            while (waitCounter.get() < activeProcesses.get()) ;
            try {
                Thread.sleep(1000);
                //reset the waitCounter to n
                waitCounter.compareAndSet(activeProcesses.get(), 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void sendIdToNeighbors() {
            try {
                sendIdToNeighborsSem.acquire();
            } catch (InterruptedException e) {
                //
            }
            randomId = random.nextInt((int) Math.pow(n, 4)) + 1;
            
            System.out.println(processId + "'s random Id is " + randomId);
            for (int i = 0; i < neighbors.size(); i += 1) {
                if (neighborThreads.get(i).isAlive()) {
                    neighbors.get(i).setRecievedIdsForProcess(randomId);
                    System.out.println(processId + " sent it's random Id to " + neighbors.get(i).processId);
                }
            }
            sendIdToNeighborsSem.release();
        }

        public void resetRoundFlag() {
            try {

                roundSem.acquire();
            } catch (InterruptedException e) {
                //
            }
            if (!incrementRound.get()) {
                incrementRound.getAndSet(true);
                System.out.println("---------------------------------------------");
                System.out.println("Round " + (totalRounds.get() + 1) + " started.");
                System.out.println("---------------------------------------------");
                
            }
            roundSem.release();
        }

        public void incrementRound() {
            try {

                roundSem.acquire();
            } catch (InterruptedException e) {
                //
            }
            if (incrementRound.get()) {
                incrementRound.getAndSet(false);
                totalRounds.getAndIncrement();
            }

            roundSem.release();
        }

        public int compareRecievedIdsFromNeighbors() {
            try {
                compareRecievedIdsFromNeighborsSem.acquire();
            } catch (InterruptedException e) {
                //
            }
            List<Integer> result = getRecievedIdsForProcess();
            System.out.println(processId + "'s randomId from neighbors are " + result);
            if (result.size() == 0) {
                compareRecievedIdsFromNeighborsSem.release();
                return randomId;
            }
            result.add(randomId);
            Integer max = Collections.max(result);

            //check for duplicate max value, if yes, return -1
            if (Collections.frequency(result, max) > 1) {
                System.out.println("More than one process have max id");
                max = -1;
            }
            //reset array
            setEmpty();
            compareRecievedIdsFromNeighborsSem.release();
            return (int) max;
        }

        public void addMIS(int processId) {
            System.out.println(processId + "is a part of MIS");
            MISSet.add(processId);
        }

        public void run() {
            //wait for all threads to start before executing
            waitForAllProcesses();
            System.out.println(processId + " Started executing...");

            while (true) {

                //Generate random ID and send to all threads, wait for all to finish
                //compare received random ID and send Done, wait for all to finish

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                resetRoundFlag();
                sendIdToNeighbors();
                //waiting for all processes to send random Ids to the neighbors
                waitForAllProcesses();

                //find Maximum Random Id
                int maxId = compareRecievedIdsFromNeighbors();
                System.out.println(processId + "'s max id is " + maxId);

                if (randomId == maxId) {
                    //add the process id to MISSet
                    addMIS(processId);
                    //send Done message to neighbors
                    sendMessageToNeighbor();
                    //wait for all processes to receive Done message
                    waitForAllProcesses();

                    //increment round before terminating
                    incrementRound();
                    activeProcesses.getAndDecrement();
                    latch.countDown();
                    System.out.println("ProcessId " + processId + " is going to terminate!");
                    break;
                }
                //wait for all processes to receive Done message
                waitForAllProcesses();

                //if the neighbor is in MISSet, neighborInMIS will be true
                if (neighborInMIS) {
                    //terminate
                    //wait for all to process the Done message
                    waitForAllProcesses();
                    incrementRound();
                    activeProcesses.getAndDecrement();
                    latch.countDown();
                    System.out.println("ProcessId " + processId + " is going to terminate!");
                    break;
                }
                //wait for all to process the Done message
                waitForAllProcesses();
                incrementRound();

            }
        }
    }

    public static HashSet<Integer> masterThread(int num_of_process,Thread[] threads) {
        
        //initially activeProcesses will be initiated with number of processes. 
        activeProcesses.set(num_of_process);

        //creating a global MIS
        MISSet = new HashSet<>();
        
        latch = new CountDownLatch(num_of_process);

        //start threads
        for (int i = 0; i < num_of_process; i++)
            threads[i].start();


        //The master thread waits for all the threads to terminate
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //printing total number of rounds
        System.out.println("------------------------------------");
        System.out.println("Total rounds: " + totalRounds.get());

        //Set of processes in MIS
        System.out.println("------------------------------------");
        System.out.println("Final Output from LubyMIS Algorithm is:");
        System.out.println("Processes in MIS:" + MISSet);
        System.out.println("------------------------------------");
        
      
        //returning the MISSet to the caller
        return MISSet;
      
    }
}