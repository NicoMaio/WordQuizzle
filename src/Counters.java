import java.util.HashMap;

public class Counters {

    private HashMap<String,int[]> table;

    /*
    * int[0] = parole corrette;
    * int[1] = parole sbagliate;
    * int[2] = parole lasciate vuote;
    * int[3] = punteggio attuale;
    *
    * */

    public Counters(){
        table = new HashMap<>();
    }

    public void addUser(String user){

        synchronized (table) {
            if (!table.containsKey(user)) {
                int[] punteggio = new int[5];
                for (int i = 0; i < 5; i++) {
                    punteggio[i] = 0;
                }
                table.put(user, punteggio);
            }
        }
    }

    public void setEndChallenge(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[4] = 1;
            table.replace(user, punteggio);
        }
    }

    public int getEndChallenge(String user){
        int[] punteggio;
        synchronized (table) {
            punteggio = table.get(user);
        }
        return punteggio[4];
    }
    public void setCorrect(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[0] = punteggio[0] + 1;
            punteggio[3] = punteggio[3] + 2;
            table.replace(user, punteggio);
        }
    }

    public void setUnCorrect(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[1] = punteggio[1] + 1;
            punteggio[3] = punteggio[3] - 1;
            table.replace(user, punteggio);
        }
    }

    public void setAbsent(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[2] = punteggio[2] + 1;
            table.replace(user, punteggio);
        }
    }

    public int[] getResults(String user){
        int[] punteggio;
        synchronized (table){
            punteggio = table.get(user);
        }
        return punteggio;
    }

    public void resetPoints(String user) {
        synchronized (table) {
            int[] punteggio = table.get(user);
            for (int i = 0; i < 5; i++) {
                punteggio[i] = 0;
            }
            table.replace(user, punteggio);
        }

    }



}
