package wtf.fentanyl.event.impl.system;

public class Stopwatch {

    private long time = -1L;

    public boolean elapsed(long delay) {
        return System.currentTimeMillis() - time >= delay;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - time;
    }

    public void reset() {
        time = System.currentTimeMillis();
    }

    public boolean isRunning() {
        return time != -1L;
    }

    public void stop() {
        time = -1L;
    }
}