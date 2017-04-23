/**
 * A class to test the performance of our Skip List against the one found in the Java libraries.
 */

import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;


import java.util.LinkedList;

import java.util.Random;
import java.util.Scanner;

public class FineGrainedPerformanceTests {
    private static final int NUM_THREADS = 20;
    int[] values;
   

    //MAIN
    public static void main(String[] args) {
    	System.out.println("Welcome");
    	System.out.println("Enter 'a'/'A' for Add test, 'c'/'C' for contain test, and 'r'/'R' for Remove Test:");
    	
    	Scanner sc = new Scanner(System.in);
    	String str = sc.next().toLowerCase();
    	
    	FineGrainedPerformanceTests test = new FineGrainedPerformanceTests(1000);
    	if(str.equals("a")){
    		System.out.println("=======================");
    		System.out.println("Our function add on avg: " + test.testSkipListAdd(false) + " ns");
            System.out.println("Java function add on avg: " + test.testSkipListAdd(true) + " ns");
            System.out.println("=======================");
    	}else if(str.equals("c")){
    		System.out.println("=======================");
    		System.out.println("Our function contains on avg: " + test.testSkipListContains(false) + " ns");
            System.out.println("Java function contains on avg: " + test.testSkipListContains(true) + " ns");
            System.out.println("=======================");
    	}else if(str.equals("r")){
    		System.out.println("=======================");
    		System.out.println("Our function remove on avg: " + test.testSkipListRemove(false) + " ns");
            System.out.println("Java function remove on avg: " + test.testSkipListRemove(true) + " ns");
            System.out.println("=======================");
    	}else{
    		System.out.println("Invalid Input, System exiting.");
    	}
    }


    public FineGrainedPerformanceTests(int size) {
        initArrayWithValues(size);
    }

    public long testSkipListAdd(boolean useJava) {
        ConcurrentSkipListMap<Integer,String> map = new ConcurrentSkipListMap<Integer, String>();
        FineGrainedSkipList<Integer> list = new FineGrainedSkipList<Integer>();
        ExecutorService es = Executors.newCachedThreadPool();
        LinkedList<Future<Long[]>> futures = new LinkedList<Future<Long[]>>();
        int start = 0;
        int valuesPerThread = values.length/NUM_THREADS;
        while (start < values.length) {
            int end = start + valuesPerThread;
            if (end > values.length - 1) {
                end = values.length - 1;
            }

            if(useJava) {
                futures.add(es.submit(new SkipListAddTimer(map, null, values, start, end)));
            } else {
                futures.add(es.submit(new SkipListAddTimer(null, list, values, start, end)));
            }

            start = end + 1;
        }

        long sum = 0;
        for (Future<Long[]> future : futures) {
            try {
                sum += sum(future.get());
            } catch (InterruptedException | ExecutionException e) {
            }
        }
        es.shutdown();
        return sum/ (long) values.length;
    }
    
    public long testSkipListRemove(boolean useJava){
    	ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<Integer, String>();
    	FineGrainedSkipList<Integer> list = new FineGrainedSkipList<Integer>();
    	for(int v : values){
    		map.put(v, "");
    		list.add(v);
    	}
    	ExecutorService es = Executors.newCachedThreadPool();
    	LinkedList<Future<Long[]>> futures = new LinkedList<Future<Long[]>>();
    	for(int i=0; i<NUM_THREADS; i++){
    		if(useJava){
    			futures.add(es.submit(new SkipListRemoveTimer(map, null, i+1)));
    		}else{
    			futures.add(es.submit(new SkipListRemoveTimer(null, list, i+1)));
    		}
    	}
    	
    	long sum=0;
    	for(Future<Long[]> future : futures){
    		try{
    			sum += sum(future.get());
    		}catch(InterruptedException | ExecutionException e){}
    	}
    	es.shutdown();
    	return sum/(long) values.length;
    }
    
    public long testSkipListContains(boolean useJava){
    	ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<Integer, String>();
    	FineGrainedSkipList<Integer> list = new FineGrainedSkipList<Integer>();
    	for(int v : values){
    		map.put(v, "");
    		list.add(v);
    	}
    	ExecutorService es = Executors.newCachedThreadPool();
    	LinkedList<Future<Long[]>> futures = new LinkedList<Future<Long[]>>();
    	for(int i=0; i<NUM_THREADS; i++){
    		if(useJava){
    			futures.add(es.submit(new SkipListContainsTimer(map, null, i+1)));
    		}else{
    			futures.add(es.submit(new SkipListContainsTimer(null, list, i+1)));
    		}
    	}
    	
    	long sum=0;
    	for(Future<Long[]> future : futures){
    		try{
    			sum += sum(future.get());
    		}catch(InterruptedException | ExecutionException e){}
    	}
    	es.shutdown();
    	return sum/(long) values.length;
    }

    /**
     * Internal/helper functions
     */

    private long sum(Long[] arr) {
        long sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum;
    }
    private void initArrayWithValues(int size) {
        values = new int[size];
        Random r = new Random();
        for (int i = 0; i < values.length; i++) {
            values[i] = r.nextInt();
        }
    }



    /**
     * Inner runnable classes used to access skip lists concurrently.
     */

    class SkipListAddTimer implements Callable<Long[]> {
        ConcurrentSkipListMap<Integer, String> javaSkipList;
        FineGrainedSkipList<Integer> ourSkipList;
        
        int[] values;
        int start;
        int end;

        SkipListAddTimer(ConcurrentSkipListMap<Integer, String> list1, FineGrainedSkipList<Integer> list2, 
                int[] values, int start, int end) {
            javaSkipList = list1;
            ourSkipList = list2;
            this.values = values;
            this.start = start;
            this.end = end;
        }

        public Long[] call() { 
            Long[] results = new Long[end - start + 1];
            int index = 0; 
            long mstart, mend;
            for (int i = start; i <= end; i++) {
                if(javaSkipList != null) {
                	mstart = System.nanoTime();
                    javaSkipList.put(values[i], "");
                    mend = System.nanoTime();
                } else {
                	mstart = System.nanoTime();
                    ourSkipList.add(values[i]);
                    mend = System.nanoTime();
                }
                results[index++] = mend - mstart;
            }
            return results;
        }
    }
    
    class SkipListContainsTimer implements Callable<Long[]>{
    	ConcurrentSkipListMap<Integer, String> javaSkipList;
    	FineGrainedSkipList<Integer> ourSkipList;
    	
    	int multiplier;
    	
    	SkipListContainsTimer(ConcurrentSkipListMap<Integer, String> list1, FineGrainedSkipList<Integer> list2, int m){
    		javaSkipList = list1;
    		ourSkipList = list2;
    		multiplier = m;
    	}
    
		public Long[] call() throws Exception {
			Long[] results = new Long[values.length];
			long start, end;
			for(int i=0; i<values.length; i++){
				if(javaSkipList!=null){
					start = System.nanoTime();
					javaSkipList.containsValue(i*multiplier);
					end = System.nanoTime();
				}else{
					start = System.nanoTime();
					ourSkipList.contains(i*multiplier);
					end = System.nanoTime();
				}
				results[i] = end-start;
			}
			return results;
		}
	}
    
    class SkipListRemoveTimer implements Callable<Long[]>{
    	ConcurrentSkipListMap<Integer, String> javaSkipList;
    	FineGrainedSkipList<Integer> ourSkipList;
    	
    	int multiplier;
    	
    	SkipListRemoveTimer(ConcurrentSkipListMap<Integer, String> list1, FineGrainedSkipList<Integer> list2, int m){
    		javaSkipList = list1;
    		ourSkipList = list2;
    		multiplier = m;
    	}
    
		public Long[] call() throws Exception {
			Long[] results = new Long[values.length];
			long start, end;
			for(int i=0; i<values.length; i++){
				if(javaSkipList!=null){
					start = System.nanoTime();
					javaSkipList.remove(i*multiplier);
					end = System.nanoTime();
				}else{
					start = System.nanoTime();
					ourSkipList.remove(i*multiplier);
					end = System.nanoTime();
				}
				results[i] = end-start;
			}
			return results;
		}
	}
}
