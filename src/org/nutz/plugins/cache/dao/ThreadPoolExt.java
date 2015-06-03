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
    private    NameableThreadPoolExecutor executor = new NameableThreadPoolExecutor(5,50, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
  
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
    
     
}
