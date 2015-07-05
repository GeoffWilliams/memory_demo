package memory_demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.IntStream;

/**
 *
 * @author geoff
 */
public class Memory_demo {

  private static final int MB_SIZE = 2048;

  private static final int UPPER_BOUND=90;
  private static final int LOWER_BOUND=65;
  private static final int STRING_LENGTH=1048576;


  private static final Random random = new Random();

  private static ArrayList<String> myBlob = new ArrayList<>();
 
 
  public static String generateString(int len) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0 ; i < len ; i++) {
        char c = (char) (random.nextInt(UPPER_BOUND - LOWER_BOUND) + LOWER_BOUND);
        sb.append(c);
    }
    
    return sb.toString();
  }


    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("About to consume " + MB_SIZE + "MB.  Press enter to start");
        System.in.read();
        for (int i = 0 ; i < MB_SIZE ; i++) {
            myBlob.add(generateString(STRING_LENGTH));
            System.out.println("allocated: " + (i+1) + "MB");
        }
        
        System.out.println("Finished!  I've allocated " + MB_SIZE + "MB.  Now I'm going to wait for 30 seconds...");
        Thread.sleep(30000);
        
        System.out.println("Ok, now I'm going to free the memory and will then enter a loop that runs forever.  Your free to watch the memory usage and kill at any time :)");
        myBlob = null;
        
        while (true) {
            Thread.yield();
            Thread.sleep(100);
            
            // make some random activity to poke the GC...
            String blah = generateString(MB_SIZE);
        }
     }
    
}
