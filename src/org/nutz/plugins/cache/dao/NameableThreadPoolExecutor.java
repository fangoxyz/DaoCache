package org.nutz.plugins.cache.dao;

import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.nutz.json.Json;
import org.nutz.log.Log;
import org.nutz.log.Logs;

 

/**
 * 一个带名字的线程池，线程池中所有线程都有名字，线程在开始执行时将线程放入threads中.
 * Date: 2015-5-29
 */
public class NameableThreadPoolExecutor extends ThreadPoolExecutor {
	 
	private static final Object lock=new byte[0];
	private static final Log log = Logs.get();
	protected static final AtomicLong threadIDAtomLong = new AtomicLong(); 
    /*
     *  保存正在运行的线程的列表,名字是执行的sql，有唯一性要求，同一条sql只写入一次
     */
    protected static final Hashtable<String, FutureTaskExt> threads=new Hashtable<String,FutureTaskExt>();


	public NameableThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		// TODO Auto-generated constructor stub
	} 

	 
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		// TODO Auto-generated method stub
		if (r instanceof FutureTaskExt) { 
			FutureTaskExt task=(FutureTaskExt)r;
			log.debug(  "thead run  , treads name:" +task.getSql() ) ;
			 
    	}  
		
		super.beforeExecute(t, r);
	}


	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		// TODO 在这里开始行前将任务压入 threads表中
		if (r instanceof FutureTaskExt) { 
			FutureTaskExt task=(FutureTaskExt)r;
			removeThread(task.getSql());
			 
    	}  
		super.afterExecute(r, t);
	}
	
	@Override
    public void execute(Runnable command) {
		if (command instanceof FutureTaskExt) { 
        	FutureTaskExt t = (FutureTaskExt)command;
            String key = t.getSql();
            if (contains(t)){ 
            	blocking(key); 
            	t.callAtRepeat();
            	log.info(  "执行了有重复的线程时的分支程序"  ) ;
            	return;
            }
            	 
             log.debug(  "真正去执行了获取数据的程序"  ) ;
            
		}
		try{
			
			super.execute(command);
		}catch (RejectedExecutionException e){
			log.error("工作队列已满,队列未加入线程");
			e.printStackTrace();
		}catch (Exception e){ 
			e.printStackTrace();
        }
		
    }
	
	public   Integer getThreadsSize(){ 
			return threads.size();  
	}
 

    @Override
    public boolean remove(Runnable task) {
        if (task instanceof FutureTaskExt) {
        	FutureTaskExt t = (FutureTaskExt)task;
        	String key = t.getSql(); 
        	removeThread(key); 
        }
        return super.remove(task);
    }

    private boolean contains(FutureTaskExt task) {
    	boolean exist=false; 
    	String name=task.getSql();
    	synchronized(lock){
    		exist=threads.containsKey(name);
    		if (exist==false){
    			threads.put(name, task); 
    			log.debug("put " + name);  
    		} 
    		return exist;
    	}
    }

	
	private void blocking(String name){
		FutureTaskExt task = threads.get(name);
        
        if (task!=null){
        	log.debug("getThreadsSize:" +  getThreadsSize());
        	log.debug("blocked at here"   + name);
			try {
				 task.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        	
        }
		
	}
 
	
	private void removeThread(String name){  
		threads.remove(name); 
		log.debug("remove thread :" + name);
		return;
	}
	
      
}
 