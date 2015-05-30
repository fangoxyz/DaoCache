package org.nutz.plugins.cache.dao;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
//http://nutz.cn/V6
//http://nutz.cn/V5
/**
 * Created by apple on 15/5/26.
 */
public class ThreadPoolExt {
	 
    // 构造一个线程
    private    NameableThreadPoolExecutor executor = new NameableThreadPoolExecutor(5,100, 20L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
  
    public Integer getThreadsSize() {
        return  executor.getThreadsSize();
    } 
    /**
     * @param task
     * @return
     */
    public Object addTaskAndWaitResult(final CallableExt task) {
    	if (task==null) return null;
        FutureTaskExt futureTask = new FutureTaskExt(task){

			@Override
			public void callAtRepeat() {
				task.callAtRepeat();
				return;
			}
        	
        }; 
        executor.execute(futureTask); 
        try {
        	Object result = futureTask.get(); 
       	 	//executor.remove(futureTask.getName());
           return result;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    } 

    public synchronized void stop(){
        try {
            executor.shutdownNow();
            executor.awaitTermination(1L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }  
    } 
    public void purgeCancelTask() {
        executor.purge();
        //executor.getQueue();
       
    }

    /**
     * @param args
     */
   /* public static void main(String[] args) {

    	 ThreadPoolExt exec = new ThreadPoolExt();
    	try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        ArrayList<String> keyList = new ArrayList<String>();
        ArrayList<String> removeKeyList = new ArrayList<String>();
        ArrayList<String> cancelKeyList = new ArrayList<String>();

        for (int i = 0; i < 5; i++) {
        	  
// 产生一个任务，并将其加入到线程池
            String task = "task@ " + exec.getNewThreadId();
           // System.out.println("put " + task);
             
            //keyList.add(exec.addDBTask(new CallableExt(task)));
            //System.out.println(exec.getActiveCount());
           // System.out.println(exec.getThreadsSize());
           // System.out.println(exec.getExecutorList().size());
        }
        
        
        
        BlockingQueue<Runnable> run=exec.getExecutorList();
        System.out.println("运行列表:");
        System.out.println(JSON.toJSON(run));
        System.out.println(exec.getActiveCount());
        System.out.println(exec.getThreadsKeys().size());
        System.out.println(exec.getExecutorList().size());
        System.out.println("运行结束:"); 
        
      
        ThreadPoolExt exec2 = new ThreadPoolExt();
        for (int i = 0; i < 10; i++) {
       	 
        	// 产生一个任务，并将其加入到线程池
            String task = "task@ " + exec2.getNewThreadId();
            System.out.println("put " + task);

            keyList.add(exec2.addDBTask(new CallableExt(task)));
            System.out.println(exec2.getThreadsKeys().size());
            System.out.println(exec2.getExecutorList().size());
        }
        
       
       
        
        try {
            Thread.sleep(1L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        long startTime=System.currentTimeMillis();
        
        exec.getTaskResultAsyn(keyList.get(0));
        System.out.println(exec.getThreadsKeys().size());
        System.out.println(exec.getExecutorList().size());
        long endTime=System.currentTimeMillis();
        System.out.println("执行时间："+(endTime-startTime));
        for(String str:exec.getThreadsKeys()){
            System.out.println(str);
        }
        startTime=System.currentTimeMillis();
        exec.getTaskResultAsyn(keyList.get(1));
        System.out.println(exec.getThreadsKeys().size());
        System.out.println(exec.getExecutorList().size());
        endTime=System.currentTimeMillis();
        System.out.println("执行时间："+(endTime-startTime));
        for(String str:exec.getThreadsKeys()){
            System.out.println(str);
        }  
        
        Integer cntThread=exec.getThreds().size();
        
        while (cntThread>0) {
        	 System.out.println(    "start====================");
        	// System.out.println(Json.toJson(exec.getThreds()));
        	for(int i=0 ;i<keyList.size();i++){
	        	 if (!exec.taskIsDone(keyList.get(i))) { 
	        		 System.out.println("runing task: " + (i + 1));
	        	 }
        	}
        	System.out.println("activeCount:" + exec.getActiveCount()); 
        	System.out.println("block queue:" + exec.getExecutorList().size());
        	cntThread=exec.getThreds().size();
        	System.out.println("threads count:" + cntThread + "====================");
        	try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	 
        }
        
     //  Object rtn= exec.getTaskResultAsyn(keyList.get(0));
    //   System.out.println(Json.toJson(rtn));
     //   System.out.println(    "end====================");
//
//        for (int i = 0; i < 10; i++) {
//            if (exec.taskIsDone(keyList.get(i))) {
//                System.out.println(exec.getTaskResult(keyList.get(i)));
//                exec.removeTask(keyList.get(i));
//
//                removeKeyList.add(keyList.get(i));
//            } else {
//                exec.cancelTask(keyList.get(i));
//                System.out.println("Cancel task: " + (i + 1));
//                exec.removeTask(keyList.get(i));
//
//                cancelKeyList.add(keyList.get(i));
//            }
//        }
        exec.purgeCancelTask();
//
        exec.stop();
        return;
//        try {
//            Thread.sleep(6000L);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        for (String key : cancelKeyList) {
//            if (exec.taskIsCancelled(key)) {
//                System.out.println("Cancel: " + key);
//            }
//        }
//        for (int i = 0; i < 10; i++) {
//            keyList.get(i);
//        }
    }*/
}
