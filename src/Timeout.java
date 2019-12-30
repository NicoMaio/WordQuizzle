public class Timeout implements Runnable {

    private long time;

    public Timeout(long tempo){
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
