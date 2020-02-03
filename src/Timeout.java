/***
 * @author Nicolò Maio
 *
 * Task del thread che gestisce un timeout
 */

public class Timeout implements Runnable {

    private int time;
    // tempo di durata del timeout

    public Timeout(int tempo){
        time = tempo;
    }

    public void run(){
        try {
            Thread.sleep(time);
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
