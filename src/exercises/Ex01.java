package exercises;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class Ex01 {
    private static final int N = 5;

    private final long FORK_PAUSE_MS = 100;        // pausa entre o 1° e o 2º garfo (alarga a janela do impasse)
    private final long MONITOR_INTERVAL_MS = 500;
    private final long NAIVE_TIMEOUT_MS = 30000;  // tempo maximo esperando o deadlock aparecer
    private final long FIXED_DURATION_MS = 10000; // duração da simulação corrigida
    private final long PG_START = System.nanoTime();

    private final String[] state = new String[N];

    public Ex01() {}

    // modo: "naive" = ingenuo, qualquer outro valor = corrigido
    public void run(String mode) {
        if ("naive".equals(mode)) {
            this.runNaive();
        } else if ("fixed".equals(mode)) {
            this.runFixed();
        }
    }

    // versao ingenua

    public void runNaive() {
        this.print("Versao INGENUA\n");

        ReentrantLock[] forks = this.createForks(false);
        AtomicBoolean running = new AtomicBoolean(true);
        int[] meals = new int[N];
        Thread[] philosophers = this.startPhilosophers(forks, false, running, meals, true);

        long deadline = System.currentTimeMillis() + NAIVE_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            this.sleep(MONITOR_INTERVAL_MS);

            List<ThreadInfo> deadlocked = this.findDeadlocked(philosophers);
            if (!deadlocked.isEmpty()) {
                this.reportDeadlock(deadlocked);
                return; // os filosofos ficam travados para sempre
            }
        }

        running.set(false);
        for (Thread t : philosophers) this.join(t, 1000);
        this.print("Nenhum deadlock em " + NAIVE_TIMEOUT_MS / 1000 + " s (é probabilistico, rode de novo).");
    }

    private void reportDeadlock(List<ThreadInfo> deadlocked) {
        synchronized (this) {
            System.out.println("\n################ DEADLOCK DETECTADO ################");
            for (ThreadInfo info : deadlocked) {
                System.out.printf("%s esta %s esperando um garfo que esta com %s%n",
                        info.getThreadName(), info.getThreadState(), info.getLockOwnerName());
            }
            System.out.println("\nEstados no momento do impasse:");
            for (int p = 0; p < N; p++) {
                System.out.printf("  Filosofo %d: %-9s (segura garfo %d, espera garfo %d)%n",
                        p, state[p], leftFork(p), rightFork(p));
            }
            System.out.println("\nCada um segura o garfo da esquerda e espera o da direita:");
            System.out.println("espera circular -> ninguem progride (deadlock).\n");
        }
    }

    // versao corrigida

    public void runFixed() {
        this.print("Versao CORRIGIDA: hierarquia de recursos (menor garfo -> maior garfo) |\n");

        ReentrantLock[] forks = this.createForks(true);
        AtomicBoolean running = new AtomicBoolean(true);
        int[] meals = new int[N];
        Thread[] philosophers = this.startPhilosophers(forks, true, running, meals, false);

        boolean deadlock = false;
        long deadline = System.currentTimeMillis() + FIXED_DURATION_MS;
        while (System.currentTimeMillis() < deadline) {
            this.sleep(MONITOR_INTERVAL_MS);
            List<ThreadInfo> deadlocked = this.findDeadlocked(philosophers);
            if (!deadlocked.isEmpty()) {
                this.reportDeadlock(deadlocked);
                deadlock = true;
                break;
            }
        }

        running.set(false);
        if (!deadlock) {
            for (Thread t : philosophers) this.join(t, 0);
        }
        this.reportStats(meals, deadlock);
    }

    private void reportStats(int[] meals, boolean deadlock) {
        synchronized (this) {
            System.out.println("\n==================== RESULTADO ====================");
            System.out.println(deadlock
                    ? "DEADLOCK DETECTADO (nao deveria ocorrer!)"
                    : "Nenhum deadlock: todos os filosofos terminaram normalmente.");
            System.out.println("\nRefeicoes por filosofo:");
            int min = Integer.MAX_VALUE, max = 0;
            for (int p = 0; p < N; p++) {
                System.out.printf("  Filosofo %d: %d%n", p, meals[p]);
                min = Math.min(min, meals[p]);
                max = Math.max(max, meals[p]);
            }
            System.out.printf("%nMenor / maior nº de refeicoes: %d / %d -> todos comeram (sem inanicao).%n%n",
                    min, max);
        }
    }

    // filosofo

    private void philosopher(int id, ReentrantLock[] forks, boolean ordered,
                             AtomicBoolean running, int[] meals) {
        int left = leftFork(id);
        int right = rightFork(id);
        int first = ordered ? Math.min(left, right) : left;
        int second = ordered ? Math.max(left, right) : right;

        while (running.get()) {
            this.setState(id, "pensando");
            this.randomSleep(10, 50);

            this.setState(id, "com fome");

            forks[first].lock();
            try {
                this.print("pegou garfo " + first + ", aguardando garfo " + second);
                this.sleep(FORK_PAUSE_MS);

                forks[second].lock();
                try {
                    this.print("pegou garfo " + second);
                    meals[id]++;

                    this.setState(id, "comendo");
                    this.randomSleep(10, 50);
                } finally {
                    forks[second].unlock();
                }
            } finally {
                forks[first].unlock();
            }
            this.print("devolveu garfos " + second + " e " + first);
        }
    }

    // utilitarios

    private static int leftFork(int p)  { return p; }
    private static int rightFork(int p) { return (p - 1 + N) % N; }

    private ReentrantLock[] createForks(boolean fair) {
        ReentrantLock[] forks = new ReentrantLock[N];
        for (int i = 0; i < N; i++) {
            forks[i] = new ReentrantLock(fair);
            state[i] = "pensando";
        }
        return forks;
    }

    private Thread[] startPhilosophers(ReentrantLock[] forks, boolean ordered,
                                       AtomicBoolean running, int[] meals, boolean daemon) {
        Thread[] threads = new Thread[N];
        for (int p = 0; p < N; p++) {
            final int id = p;
            threads[p] = new Thread(() -> this.philosopher(id, forks, ordered, running, meals), "F" + p);
            threads[p].setDaemon(daemon);
        }
        for (Thread t : threads) t.start();
        return threads;
    }

    // Retorna as threads que a JVM reporta em deadlock
    private List<ThreadInfo> findDeadlocked(Thread[] philosophers) {
        ThreadMXBean MX = ManagementFactory.getThreadMXBean();
        List<ThreadInfo> result = new ArrayList<>();
        long[] ids = MX.findDeadlockedThreads();
        if (ids == null) return result;

        for (ThreadInfo info : MX.getThreadInfo(ids)) {
            if (info == null) continue;
            for (Thread t : philosophers) {
                if (t.threadId() == info.getThreadId()) result.add(info);
            }
        }
        return result;
    }

    private void setState(int id, String newState) {
        state[id] = newState;
        this.print(newState.toUpperCase());
    }

    private void randomSleep(int minMs, int maxMs) {
        this.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
    }

    private void sleep(long timeMs) {
        try {
            Thread.sleep(timeMs);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    // Espera a thread terminar (timeoutMs = 0 espera sem limite), tratando a interrupção aqui
    private void join(Thread thread, long timeoutMs) {
        try {
            thread.join(timeoutMs);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    private synchronized void print(String message) {
        long ms = (System.nanoTime() - this.PG_START) / 1000000;
        System.out.printf("[%6d ms] [%-4s] %s%n", ms, Thread.currentThread().getName(), message);
    }
}
