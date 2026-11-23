package exercises;

import java.util.Objects;
import java.util.concurrent.Semaphore;

public class Ex02 {
    private static final int T = 8;          // numero de threads
    private static final int M = 200000;     // incrementos por thread
    private static final int RUNS = 3;       // execucoes de cada versao

    private final Semaphore SEMAPHORE = new Semaphore(1, true);
    private final long PG_START = System.nanoTime();
    private volatile long count = 0;

    public Ex02() {}

    // modo: "unsafe" = sem sincronizacao, "semaphore" = com semaforo binario
    public void run(String mode) throws InterruptedException {
        if (Objects.equals(mode, "unsafe")) {
            this.print("| Unsafe Mode (sem sincronizacao) |\n");
            this.runBenchmark(false);
        } else if (Objects.equals(mode, "semaphore")) {
            this.print("| Semaphore Mode (semaforo binario justo) |\n");
            this.runBenchmark(true);
        }
    }

    private void runBenchmark(boolean useSemaphore) throws InterruptedException {
        long expected = (long) T * M;

        System.out.println("| Execucao | Esperado  | Obtido    | Perdidos  | Tempo (ms) |");
        System.out.println("|----------|-----------|-----------|-----------|------------|");

        for (int run = 1; run <= RUNS; run++) {
            this.count = 0;

            long start = System.nanoTime();
            this.startAndJoin(useSemaphore);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            System.out.printf("| %-8d | %-9d | %-9d | %-9d | %-10d |%n",
                    run, expected, this.count, expected - this.count, elapsedMs);
        }
        System.out.println();
    }

    private void startAndJoin(boolean useSemaphore) throws InterruptedException {
        Thread[] workers = new Thread[T];

        for (int t = 0; t < T; t++) {
            workers[t] = new Thread(
                    useSemaphore ? this::executeSafeTask : this::executeUnsafeTask,
                    "T" + (t + 1)
            );
        }

        for (Thread worker : workers) worker.start();
        for (Thread worker : workers) worker.join();
    }

    private void executeUnsafeTask() {
        for (int i = 0; i < M; i++) {
            this.count = this.count + 1;
        }
    }

    private void executeSafeTask() {
        for (int i = 0; i < M; i++) {
            try {
                this.SEMAPHORE.acquire();
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                return;
            }

            try {
                this.count = this.count + 1;
            } finally {
                this.SEMAPHORE.release();
            }
        }
    }

    private synchronized void print(String message) {
        long ms = (System.nanoTime() - this.PG_START) / 1_000_000;
        System.out.printf(
                "[%5d ms] [%-7s] %s%n",
                ms,
                Thread.currentThread().getName(),
                message
        );
    }
}
