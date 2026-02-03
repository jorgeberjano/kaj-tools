package es.jbp.kajtools.batch;

public interface BatchContext {

    void stop();
    boolean isRunning();
    boolean isStopping();
}
