package memory_demo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demonstrate How JVM garbage collects large objects and output memory usage to
 * a CSV file
 * @author Geoff Williams <geoff@geoffwilliams.me.uk>
 */
public class MemoryDemo {

    private static final int MB_SIZE = 2048;

    private static final int UPPER_BOUND = 90;
    private static final int LOWER_BOUND = 65;
    private static final int MB = 1048576;

    private static final Random random = new Random();

    private ArrayList<String> myBlob = new ArrayList<>();
    private ArrayList<String> myActivityBlob = new ArrayList<>();

    public String generateString(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = (char) (random.nextInt(UPPER_BOUND - LOWER_BOUND) + LOWER_BOUND);
            sb.append(c);
        }

        return sb.toString();
    }

    public static long getPID() {
        String processName
                = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(processName.split("@")[0]);
    }

    public void demo(String args[], boolean gcOnce, boolean manualGc, boolean objectActivity) throws InterruptedException, IOException {
        Thread t = new Thread(new MemoryPrinter(args));
        t.start();

        for (int i = 0; i < MB_SIZE; i++) {
            myBlob.add(generateString(MB));
        }

        if (manualGc) {
            System.out.println("Launching manual GC thread");
            Thread gc = new Thread(new MemoryReclaimer());
            gc.start();
        }
        System.out.println("Done:  allocated " + MB_SIZE + "MB");

        myBlob = null;
        
        if (gcOnce) {
            System.out.println("Doing ONE manual GC");
            System.gc();
        }

        // 50MB of base usage in all cases...
        myActivityBlob.clear();
        for (int i = 0; i < 50; i++) {
            myActivityBlob.add(generateString(MB));
        }
        
        if (objectActivity) {
            System.out.println("Starting background object activity");
        }
        while (true) {
            Thread.sleep(100);

            
            // this line actually makes a huge difference to garbage collection 
            // try commenting it out and see what happens... basically if there
            // is no underlying activity in the VM then the garbage collector 
            // doesn't bother to do anything - even if its sitting on a ton of
            // memory.  This goes for G1GC too - it's a very self-centered view
            // of the world ;-)
            String blah = myActivityBlob.get(random.nextInt(myActivityBlob.size() - 1));
            //        + myActivityBlob.get(random.nextInt(myActivityBlob.size() - 1));

            if (objectActivity) {
                myActivityBlob.clear();
                for (int i = 0; i < 512; i++) {
                    myActivityBlob.add(generateString(MB));
                }
            }
        }

    }

    public static void main(String[] args) throws InterruptedException, IOException {
        boolean manualGc = false;
        boolean objectActivity = false;
        boolean gcOnce = false;
        for (String arg : args) {
            switch (arg) {
                case "--manual_gc":
                    manualGc = true;
                    break;
                case "--object_activity":
                    objectActivity = true;
                    break;
                case "--gc_once":
                    gcOnce = true;
                    break;
            }
        }
        MemoryDemo md = new MemoryDemo();
        md.demo(args, gcOnce, manualGc, objectActivity);
    }

    class MemoryPrinter implements Runnable {

        private boolean running = true;
        private int interval = 1;
        private PrintWriter pw;
        private String[] args;
        
        private MemoryPrinter() {}
        public MemoryPrinter(String[] args) {
            this.args = args;
        }
        
        private void header() {
            RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
            List<String> arguments = runtimeMxBean.getInputArguments();
            pw.print("# Command line arguments:  ");
            for (String arg : arguments) {
                pw.print(arg);
                pw.print(" ");
            }
            if (args.length > 0) {
                pw.print(" Program arguments:  ");
                for (String arg: args) {
                    pw.print(arg);
                    pw.print(" ");
                }
            }
            pw.println();
            pw.println();
            pw.println("used memory, total memory, maximum memory");
        }
        
        public void setArgs(String[] args) {
            this.args = args;
        }

        @Override
        public void run() {
            try {
                pw = new PrintWriter(getPID() + "_memlog.txt");
                header();
                while (running) {
                    long usedMem = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MB;
                    long freeMem = Runtime.getRuntime().freeMemory() / MB;
                    long totalMem = Runtime.getRuntime().totalMemory() / MB;
                    long maxMemory = Runtime.getRuntime().maxMemory() / MB;
                    String csv = usedMem + "," + totalMem + "," + maxMemory;
                    pw.println(csv);
                    pw.flush();
                    Thread.sleep(interval * 1000);
                }
            } catch (FileNotFoundException | InterruptedException ex) {
                Logger.getLogger(MemoryDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    class MemoryReclaimer implements Runnable {

        private boolean running = true;
        private boolean first = true;

        @Override
        public void run() {
            while (running) {
                long totalMem = Runtime.getRuntime().totalMemory() / MB;
                long freeMem = Runtime.getRuntime().freeMemory() / MB;
                double percentFree = ((double) freeMem / totalMem) * 100;

                System.out.println("Free memory %: " + percentFree);
                if (first || percentFree > 60) {
                    try {
                        System.out.println("*** REQUESTED FULL GC ***");
                        System.gc();
                        first = false;
                        Thread.sleep(60000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MemoryDemo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    System.out.println("Free memory % <= 60 - terminating cleanup thread");
                    running = false;
                }
            }
        }

    }
}
