package server;

import java.util.LinkedList;

class ThreadPool {
    private volatile boolean isStopped = false;
    private final int limit;
    private final LinkedList<Runnable> queue;

    ThreadPool(int threadsAmount, int tasksAmount) {
        this.limit = tasksAmount;
        this.queue = new LinkedList<>();

        for (int i = 0; i < threadsAmount; i++) {
            new Thread(new ServerThread()).start();
        }
    }

    public synchronized void add(Runnable newTask) {
        while (queue.size() == limit) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        queue.addLast(newTask);
        notify();
    }

    private synchronized Runnable remove() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Runnable handler = queue.removeFirst();
        notify();
        return handler;
    }

    private final class ServerThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    remove().run();
                } catch (Exception e) {
                    break;
                }
            }
        }
    }
}
