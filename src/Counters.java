/**
 * @author Nicolò Maio
 *
 * Classe Counters usata per gestire i conteggi delle sfida fra i clienti.
 * */

import java.util.HashMap;

public class Counters {

    private final HashMap<String,int[]> table;
    // HashMap table usata per gestire i conteggi delle sfide.

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

    /**
     * @param user utente da aggiungere a table.
     */
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

    /**
     * @param user utente che ha terminato di inviare risposte.
     */
    public void setEndChallenge(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[4] = 1;
            table.replace(user, punteggio);
        }
    }


    /**
     * @param user utente del quale si vuol sapere se ha terminato la sfida.
     * @return il punteggio finale di quel cliente.
     */
    public int getEndChallenge(String user){
        int[] punteggio;
        synchronized (table) {
            punteggio = table.get(user);
        }
        return punteggio[4];
    }

    /**
     * @param user utente al quale si sta aggiungendo una risposta corretta consegnata.
     */
    public void setCorrect(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[0] = punteggio[0] + 1;
            punteggio[3] = punteggio[3] + 2;
            table.replace(user, punteggio);
        }
    }

    /**
     * @param user utente al quale si sta aggiungendo una risposta non corretta consegnata.
     */
    public void setUnCorrect(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[1] = punteggio[1] + 1;
            punteggio[3] = punteggio[3] - 1;
            table.replace(user, punteggio);
        }
    }

    /**
     * @param user utente al quale si sta aggiungendo una risposta lasciata vuota consegnata.
     */
    public void setAbsent(String user){
        synchronized (table) {
            int[] punteggio = table.get(user);
            punteggio[2] = punteggio[2] + 1;
            table.replace(user, punteggio);
        }
    }

    /**
     * @param user utente del quale si vuole ottenere i vari conteggi della sfida.
     * @return array punteggio di quel utente del quale si richiedono i risultati della sfida.
     */
    public int[] getResults(String user){
        int[] punteggio;
        synchronized (table){
            punteggio = table.get(user);
        }
        return punteggio;
    }

    /**
     * @param user utente del quale si sta resettando i conteggi.
     */
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
