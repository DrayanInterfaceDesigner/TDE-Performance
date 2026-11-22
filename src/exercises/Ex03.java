package exercises;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class Ex03 {
    public ReentrantLock RESOURCE_A_LOCK = new ReentrantLock();
    public ReentrantLock RESOURCE_B_LOCK = new ReentrantLock();

    private final long LOCK_PAUSE_MS = 50;
    private final long MONITOR_INTERVAL_MS = 1000;
    private final long PG_START = System.nanoTime();

    public Ex03() {}

    private void run(String mode) throws InterruptedException {
        if (Objects.equals(mode, "lock")) {
            this.print("| Lock Mode |\n");
            this.runWithLock();
        } else {
            this.print("| Safe Mode |\n");
            this.runFixed();
        }
    }

    private void runWithLock() throws InterruptedException {
        Thread T1 = new Thread(() -> this.executeTask(
                "PROCESS 1",
                RESOURCE_A_LOCK, "RESOURCE A",
                RESOURCE_B_LOCK, "RESOURCE B"
        ), "T1");

        Thread T2 = new Thread(() -> this.executeTask(
                "PROCESS 2",
                RESOURCE_B_LOCK, "RESOURCE B",
                RESOURCE_A_LOCK, "RESOURCE A"
        ), "T2");

        Thread WATCHDOG = new Thread(() -> this.deadlockWatchdog(), "WATCHDOG");
        WATCHDOG.setDaemon(true);

        T1.start();
        T2.start();
        WATCHDOG.start();

        T1.join();
        T2.join();
        this.print("Finished without deadlock.");
    }

    private void runFixed() throws InterruptedException  {
        Thread T1 = new Thread(() -> this.executeTask(
                "PROCESS 1",
                RESOURCE_A_LOCK, "RESOURCE A",
                RESOURCE_B_LOCK, "RESOURCE B"
        ), "T1");

        Thread T2 = new Thread(() -> this.executeTask(
                "PROCESS 2",
                RESOURCE_A_LOCK, "RESOURCE A",
                RESOURCE_B_LOCK, "RESOURCE B"
        ), "T2");

        T1.start();
        T2.start();

        T1.join();
        T2.join();

        this.print("Finished without deadlock.");
    }

    private void executeTask(
            String THREAD_NAME,
            ReentrantLock RESOURCE_A_LOCK, String RESOURCE_A_NAME,
            ReentrantLock RESOURCE_B_LOCK, String RESOURCE_B_NAME
    )  {
        this.print(THREAD_NAME + " trying to get " + RESOURCE_A_NAME);
        RESOURCE_A_LOCK.lock();

        try {
            this.print(THREAD_NAME + " locked " + RESOURCE_B_NAME + " in.");
            this.sleep(this.LOCK_PAUSE_MS);

            this.print(THREAD_NAME + " trying to get " + RESOURCE_B_NAME + " while holding " + RESOURCE_A_NAME);
            RESOURCE_B_LOCK.lock();

        } finally {

        }
    }

    private void sleep(long TIME_MS) {
        try {
            Thread.sleep(TIME_MS);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
        }
    }

    private void deadlockWatchdog() {
        ThreadMXBean MX = ManagementFactory.getThreadMXBean();

        while (true) {
            this.sleep(this.MONITOR_INTERVAL_MS);

            long[] IDS = MX.findDeadlockedThreads();
            if (IDS == null) {
                continue;
            }

            this.print("##### DEADLOCK DETECTED: " + IDS.length + " threads #####");

            for (ThreadInfo INFO : MX.getThreadInfo(IDS, true, true)) {
                this.print(INFO.getThreadName()
                        + " is " + INFO.getThreadState()
                        + " | waiting for " + INFO.getLockName()
                        + " | owned by " + INFO.getLockOwnerName());
            }

            this.print("Exiting by timeout..");
            System.exit(1);
        }
    }

    private synchronized void print(String message) {
        long ms = (System.nanoTime() - this.PG_START / 1_000_000);
        System.out.printf(
                "[%5d ms] [%-7s] %s%n",
                ms,
                Thread.currentThread().getName(),
                message
        );
    }

    public void gostozo_i_amantes_da_phoda() {
        IO.println("Cotuendo amants de la phueda: OH madre MIA");
        for (int i = 1; i <= 5; i++) {
            IO.println("te culo contienes " + i + " amants de la phueda");
        }
    }
}
