public class Timeout implements Runnable {

    private int time;

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
