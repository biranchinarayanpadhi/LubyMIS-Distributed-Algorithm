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
 * -LubyMIS basic class structure
 * -Synchronization between rounds
 * -Send Done message(i.e. 'I am part of MIS' message to neighbour)
 * -Increment Phase (one phase has 3 rounds) and print Rounds
 *
 * @Biranchi Narayan Padhi
 * -Generation of random IDs
 * -Send IDs to neighbors
 * -Comparison of IDs to find max ID
 *
 * @Siddarameshwar Kadagad
 * -Add process to MISSet
 * -CountDownLatch in master thread
 * -----------------------------------------------
 * */
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

    private static Semaphore phaseSem = new Semaphore(1);
    private static Semaphore sendIdToNeighborsSem = new Semaphore(1);
    private static Semaphore compareRecievedIdsFromNeighborsSem = new Semaphore(1);
    private static AtomicBoolean printRound = new AtomicBoolean(true);
    private static CountDownLatch latch;
    public static final AtomicInteger waitCounter = new AtomicInteger(0);
    public static final AtomicInteger activeProcesses = new AtomicInteger(0);
    private static HashSet<Integer> MISSet;
    private static final AtomicInteger totalPhases = new AtomicInteger(0);
    private static final AtomicBoolean incrementPhase = new AtomicBoolean(false);

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

        public void waitForAllProcesses(int round) {
            waitCounter.getAndIncrement();
            while (waitCounter.get() < activeProcesses.get()) ;
            try {
                Thread.sleep(1000);
                //reset the waitCounter to n
                boolean updatedFlag = waitCounter.compareAndSet(activeProcesses.get(), 0);
                if(updatedFlag && round > 0){
                    System.out.println("---------------------");
                    System.out.println("Round "+round+"...");
                    System.out.println("---------------------");
                }
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

        public void resetPhaseFlag() {
            try {

                phaseSem.acquire();
            } catch (InterruptedException e) {
                //
            }
            if (!incrementPhase.get()) {
                incrementPhase.getAndSet(true);
                System.out.println("---------------------------------------------");
                System.out.println("Phase " + (totalPhases.get() + 1) + " started.");
                System.out.println("---------------------------------------------");
                System.out.println("Round 1...");
                System.out.println("---------------------");
            }
            phaseSem.release();
        }

        public void incrementPhase() {
            try {

                phaseSem.acquire();
            } catch (InterruptedException e) {
                //
            }
            if (incrementPhase.get()) {
                incrementPhase.getAndSet(false);
                totalPhases.getAndIncrement();
            }

            phaseSem.release();
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
            System.out.println(processId + " is a part of MIS");
            MISSet.add(processId);
        }

        public void run() {
            //wait for all threads to start before executing
            waitForAllProcesses(0);
            System.out.println(processId + " Started executing...");

            while (true) {

                //-----------------------Round 1-------------------------------
                //Generate random ID and send to all threads, wait for all to finish

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                resetPhaseFlag();
                sendIdToNeighbors();
                //waiting for all processes to send random Ids to the neighbors
                waitForAllProcesses(2);
                //-----------------------Round 2-------------------------------

                //find Maximum Random Id
                //compare received random ID and send Done, wait for all to finish
                int maxId = compareRecievedIdsFromNeighbors();
                System.out.println(processId + "'s max id is " + maxId);

                if (randomId == maxId) {
                    //add the process id to MISSet
                    addMIS(processId);
                    //send Done message to neighbors
                    sendMessageToNeighbor();
                    //wait for all processes to receive Done message
                    waitForAllProcesses(3);

                    //increment phase before terminating
                    incrementPhase();
                    activeProcesses.getAndDecrement();
                    latch.countDown();
                    System.out.println("ProcessId " + processId + " has won and is going to terminate!");
                    break;
                }
                //wait for all processes to receive Done message
                waitForAllProcesses(3);
                //-----------------------Round 3-------------------------------
                //if the neighbor is in MISSet, neighborInMIS will be true
                if (neighborInMIS) {
                    //terminate
                    //wait for all to process the Done message
                    waitForAllProcesses(0);
                    incrementPhase();
                    activeProcesses.getAndDecrement();
                    latch.countDown();
                    System.out.println("ProcessId " + processId + " has lost and is going to terminate!");
                    break;
                }
                //wait for all to process the Done message
                waitForAllProcesses(0);
                incrementPhase();
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
        //wait for all threads to print the terminating messages
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //printing total number of rounds
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("With three rounds in each phase, Total rounds: " + totalPhases.get() * 3);

        //Set of processes in MIS
        System.out.println("----------------------------------------------------------------------------");
        System.out.println("Final Output from LubyMIS Algorithm is:");
        System.out.println("Processes in MIS:" + MISSet);
        System.out.println("----------------------------------------------------------------------------");
        
      
        //returning the MISSet to the caller
        return MISSet;
      
    }
}