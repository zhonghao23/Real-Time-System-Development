package RTS_Assignment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;

public class LiveScore {
    static ArrayList<String> teams1 = new ArrayList<String>();
    static ArrayList<String> teams2 = new ArrayList<String>();
    
    public static class isExit {
        public static boolean exit = false;
    }
    
    public static void main(String args[]){
        //read teams from txt file
        long startTime = new Timestamp(System.currentTimeMillis()).getTime();
        int teamAmount = 0;
        try{
            File openFile = new File("firstTeam.txt");
            Scanner reader = new Scanner(openFile);    
            while (reader.hasNextLine()){
                teams1.add(reader.nextLine());
                teamAmount++;
            }
            reader.close();
            File openFile2 = new File("secondTeam.txt");
            Scanner reader2 = new Scanner(openFile2);
            teamAmount = 0;
            while (reader2.hasNextLine()) {
                teams2.add(reader2.nextLine());
                teamAmount++;
            }
            reader2.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }
        
        ConcurrentLinkedQueue<Match> platform = new ConcurrentLinkedQueue<>();
        ArrayList<Match> matches = new ArrayList<Match>(); //allocate match
        for(int i=0;i<teamAmount;i++){
            Match m = new Match(i, teams1.get(i), 0, teams2.get(i), 0);
            matches.add(m);
        }
        
        ExecutorService fixed_threads = Executors.newFixedThreadPool(5);
        for (int i=0;i<5;i++){ //create 5 threads
            fixed_threads.execute(new scoreUpdater(platform,teamAmount));
        }
        fixed_threads.shutdown();
        ScheduledExecutorService s = Executors.newScheduledThreadPool(1); //more threads = more parallel for each task
        s.scheduleAtFixedRate(new scoreDisplayer(platform, matches), 1, 1, TimeUnit.SECONDS); //delay 1sec, every 1 sec
        while (!fixed_threads.isTerminated()) {
        }
        while(true){
            if(platform.isEmpty()){
                s.shutdown();
                break;
            }            
        }
        while(true){
            if(s.isTerminated()){
                System.out.println("-------------NO MORE RESULTS\n-------------ALL MATCHES ARE ENDED");
                long endTime = new Timestamp(System.currentTimeMillis()).getTime();
                System.out.println("Start Time: " + startTime + "\nEnd Time: " + endTime + "\nTime Elapsed: " + (endTime-startTime));
                System.exit(0);
            } 
        }
    }
    
    static class Match {
        int id;
        String team1;
        int score1;
        String team2;
        int score2;

        public Match(int id, String team1, int score1, String team2, int score2) {
            this.id = id;
            this.team1 = team1;
            this.score1 = score1;
            this.team2 = team2;
            this.score2 = score2;
        }

        public int getScore1() {
            return score1;
        }

        public void setScore1(int score1) {
            this.score1 = score1;
        }

        public int getScore2() {
            return score2;
        }

        public void setScore2(int score2) {
            this.score2 = score2;
        }
    }
    
    static class scoreUpdater implements Runnable {
        Random rand = new Random();
        ConcurrentLinkedQueue<Match> platform;
        int match_num;
        public scoreUpdater(ConcurrentLinkedQueue<Match> platform, int match_num){
            this.platform = platform;
            this.match_num = match_num;
        }

        public void run() {          
            int work_count = 0;
            while(work_count < 10){
                int match_id = rand.nextInt(match_num); //maybe can enhance to one thread handle one portion
                try {
                    RandomAccessFile openFile = new RandomAccessFile("randomScores.txt", "r");
                    int file_length = (int)openFile.length();
                    int randomPosition1 = rand.nextInt(file_length-1);
                    int randomPosition2 = rand.nextInt(file_length-1);
                    openFile.seek(randomPosition1); 
                    openFile.readLine();    //get random score from txt file
                    int score1 = Integer.parseInt(openFile.readLine());
                    openFile.seek(randomPosition2);
                    openFile.readLine();    //get random score from txt file
                    int score2 = Integer.parseInt(openFile.readLine());
                    openFile.close();
                    Match m = new Match(match_id, teams1.get(match_id), score1, teams2.get(match_id), score2);
                    //System.out.println("Match: " + match_id + " Score1: " + score1 + " Score2: " + score2);
                    platform.offer(m);
                    work_count++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try{
                    Thread.sleep((30+rand.nextInt(10)) * 100); //sleep for 1000ms - 1900ms
                }catch(InterruptedException e) {
                    System.out.println("error");
                }
            }
        }
    }

    static class scoreDisplayer implements Runnable {
        ConcurrentLinkedQueue<Match> platform;
        ArrayList<Match> matches;
        public scoreDisplayer(ConcurrentLinkedQueue<Match> platform, ArrayList<Match> matches){
            this.platform = platform;
            this.matches = matches;
        }
        
        public void run() {
            if (platform.peek() != null) { //periodic
                for (Match result : platform){
                    //get current scores
                    Match m = matches.get(result.id);
                    int new_score1 = m.getScore1() + result.score1;
                    int new_score2 = m.getScore2() + result.score2;
                    m.setScore1(new_score1);
                    m.setScore2(new_score2);
                    matches.set(result.id, m);
                    platform.remove(result);
                }
                System.out.println("Live Scores: (Periodic)");
                for (Match match : matches){
                    System.out.println(match.team1 + "\t" + match.score1 + "\t:\t" + match.score2 + "\t" + match.team2);
                }
                long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
                System.out.println("-----Periodic Time:"+ timestamp);
            }
            else { //aperiodic
                System.out.println("\n\t---No new results in 1 second... Waiting...");
                ExecutorService ex = Executors.newCachedThreadPool();
                Future<ArrayList<Match>> latest_matches = ex.submit(new getLatestScores(platform,matches));
                try{
                    System.out.println("\tLive Scores: (Aperiodic)");
                    for (Match match : latest_matches.get(5, TimeUnit.SECONDS)){
                        System.out.println("\t" + match.team1 + "\t" + match.score1 + "\t:\t" + match.score2 + "\t" + match.team2);
                    }
                    long timestamp = new Timestamp(System.currentTimeMillis()).getTime();
                    System.out.println("\t-----Aperiodic Time:"+ timestamp+"\n");
                }catch(InterruptedException | CancellationException e){
                    e.printStackTrace();
                }catch(ExecutionException e){
                    throw new RuntimeException(e);
                }catch(TimeoutException e){ 
                    ex.shutdownNow();
                }
            }
        }
    }
   
    static class getLatestScores implements Callable<ArrayList<Match>> {
        ConcurrentLinkedQueue<Match> platform;
        ArrayList<Match> matches;
        
        public getLatestScores(ConcurrentLinkedQueue<Match> platform, ArrayList<Match> matches) {
            this.platform = platform;
            this.matches = matches;
        }
        
        public ArrayList<Match> call() {
            while(true){
                if(platform.peek() != null){
                    for (Match result : platform){
                        //get current scores
                        Match m = matches.get(result.id);
                        int new_score1 = m.getScore1() + result.score1;
                        int new_score2 = m.getScore2() + result.score2;
                        m.setScore1(new_score1);
                        m.setScore2(new_score2);
                        matches.set(result.id, m);
                        platform.remove(result);
                    }
                    break;
                }
            }
            return matches;
        }
    }
}