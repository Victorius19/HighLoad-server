package server;

import java.util.LinkedList;

class ThreadPool {
    private final int limit;
    private final LinkedList<Runnable> queue;

    ThreadPool(int count, int limit) {
        this.limit = limit;
        this.queue = new LinkedList<>();

        for (int i = 0; i < count; i++) {
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
