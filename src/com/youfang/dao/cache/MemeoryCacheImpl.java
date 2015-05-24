package com.youfang.dao.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.plugins.cache.dao.CacheResult;

/**
 * 
* @ClassName: MemeoryCacheImpl    
* @Description:实现ehcache缓存数据
* @author 小俊不乖  
* @date 2012-7-6 上午11:25:07
* 		modified by 面  on 2015-3-5
 */
public class MemeoryCacheImpl implements CacheController {
	/**
	 * ehcache缓存的数据对象，所有数据都缓存在这个对象中
	 */
	public static final CacheManager  cacheManager  = new CacheManager(); 
	public static final String cacheNameFlushSql="flushSql";
	protected  static long flushTimes=0L;
	protected  static long getTimes=0L;
	protected  static long putTimes=0L;
	
	private static final Log log = Logs.get();
	private static final AtomicLong flushAtomLong = new AtomicLong();
	private static final AtomicLong getAtomLong = new AtomicLong();
	private static final AtomicLong putAtomLong = new AtomicLong(); 
	private static  byte[] lock = new byte[0]; 
	public static final byte[] NULL_OBJ = new byte[0];
	
	@SuppressWarnings("unchecked")
	public List<String> getCacheLink(String tableName){
		try{
			return (List<String>) getObject(new CacheModel("cachelinks"),tableName);
		}catch  (Exception e) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Boolean existsCacheLink(String tableName,String linkName){
		try{
			List<String> cacheLink=new ArrayList<String>();
			cacheLink= (List<String>) getObject(new CacheModel("cachelinks"),tableName);
			return cacheLink.contains(linkName); 
		}catch  (Exception e) {
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addCacheLink(String tableName,String linkName){
		try{
			if (linkName==null) return;
			if (tableName==null) return;
			if (tableName.equals(linkName)) return;
			//使用同步的方式读取缓存域中的数据，对数据操作就直接修改了数据
			List<String> cacheLink= (List<String>)  getObjectSyn(new CacheModel("cachelinks"),tableName);
			if (cacheLink==null){
				cacheLink=new ArrayList<String>();
				cacheLink.add(linkName); 
				putObject(new CacheModel("cachelinks"),tableName,cacheLink); 
				return;
			}else if (!cacheLink.contains(linkName)){ 
				cacheLink.add(linkName); 
				//putObject(new CacheModel("cachelinks"),tableName,cacheLink); 
			}
		}catch  (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
 
	public void addCacheLink(List<String> Caches,String linkName){
		try{
			for(String table:Caches){ 
				addCacheLink(table,linkName); 
			}
		}catch  (Exception e) { 
			e.printStackTrace(); 
		}
	}
	
	@SuppressWarnings("unchecked")
	public void flushCacheLinks(String tableName){
		try{
			List<String> cacheLinks=new ArrayList<String>();
			cacheLinks=  (List<String> )getObject(new CacheModel("cachelinks"),tableName);
			log.debug("cachelinks:" +  cacheLinks);
			for(String alink :cacheLinks){ 
				flush(new CacheModel(alink));
				flushTimes=flushAtomLong.getAndIncrement();
				log.debug("flush" + alink);
			}
		}catch  (Exception e) { 
			 
		}
	}
	 
	
 
	public void flush(CacheModel cacheModel) { 
		if (cacheModel==null) return;
		Ehcache tmpCache = getCache(cacheModel,false);
		 if (tmpCache!=null) {
			 tmpCache.removeAll(); 
			 flushTimes=flushAtomLong.getAndIncrement(); 
		 } 
		 if (cacheModel !=null){
			 flushCacheLinks(cacheModel.getName());
		 }
		/* if (cacheModel.getLinkView()!=null) { 
			 flushView(new CacheModel(cacheModel.getLinkView(),cacheModel.getMaxElements()));
		 }*/
	}
	
	
	
	

	public void removeObject(CacheModel cacheModel, Object key) { 
		if (cacheModel==null) return;
		if (key==null) return;
		Ehcache tmpCache = getCache(cacheModel,false);
		 if (tmpCache!=null) {
			 tmpCache.remove(key) ;
		 }   
		 
	} 
 
	public Object getObject(CacheModel cacheModel, Object key) {
		if (cacheModel==null) return CacheResult.NOT_FOUNT;
		if (key==null) return CacheResult.NOT_FOUNT;
		Element elem = getCache(cacheModel).get(key);
		if (elem==null){  
			//synchronized (lock) {
			//	elem = getCache(cacheModel).get(key);
			//	if (elem == null) { 
					return CacheResult.NOT_FOUNT;
			//	} 
			//}
		} 
	   getTimes=getAtomLong.getAndIncrement();
	   Object result=elem.getObjectValue();
	   if(isNULL_OBJ(result)){ 
		   return  CacheResult.NULL;
	   } 
	   return cloneObj(result);
	    
	} 

	public void putObject(CacheModel cacheModel, Object key, Object object) { 
		if (cacheModel==null) return;
		if (key==null) return;
		Serializable data = (Serializable) object ; 
		Object obj=null;
		if (data==null){
			obj=NULL_OBJ;
		}else{			 
			obj=cloneObj(data);
		}
		 getCache(cacheModel).put(new Element(key.toString(), obj));
		 putTimes=putAtomLong.getAndIncrement();  
	}
	 
	
	public void putLog( String key, String object) {
		getCache(new CacheModel(cacheNameFlushSql,100)).put(new Element(key.toString(), object));
	}
	
	@Override
	public void shutDown() {
		cacheManager.shutdown();
	}
	
	
	
	 
	
	
	protected  Object getObjectSyn(CacheModel cacheModel, Object key) {
		Element elem = getCache(cacheModel).get(key); 
		if (elem==null){   
			return null; 
		} 
	    getTimes=getAtomLong.getAndIncrement();
	    return elem.getObjectValue();
	}
	
	protected  Ehcache getSessionCache() {
		String cacheName="session";
		Integer maxElements=100000;
		Ehcache cache =cacheManager.getEhcache(cacheName);
		if (cache==null){  
			synchronized (lock) {
				cache = cacheManager.getEhcache(cacheName);
				if (cache == null) { 
					cache = new Cache(cacheName,maxElements, false, false, 0, 0);  
					cacheManager.addCache(cache); 
				}
			}
		} 
		return cache; 
		 //   return cacheManager.getEhcache("session");
	}
	
	protected  Ehcache getCache(String cacheName){ 
		return getCache(new CacheModel(cacheName,10000)); 
	}
	
	protected  Ehcache getCache(CacheModel cacheModel){
		return getCache(cacheModel,true);
	} 
	protected  Ehcache getCache(CacheModel cacheModel,Boolean create){
		String cacheName=cacheModel.getName();
		Integer maxElements=cacheModel.getMaxElements();
		Ehcache cache =cacheManager.getEhcache(cacheName);
		if (cache==null){  
			if (!create) return null;
			synchronized (lock) {
				cache = cacheManager.getEhcache(cacheName); 
				if (cache == null) {
					if (maxElements==null){
						maxElements=10000;
					} 
					cache = new Cache(cacheName,maxElements, false, false, 36000, 36000);  
					cacheManager.addCache(cache); 
				}
			}
		} 
		return cache; 
	}
     
	protected Object cloneObj(Object obj) {
		if (obj == null)
			return null;
		try {
			ByteArrayOutputStream bao = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bao);
			oos.writeObject(obj); 
			return new ObjectInputStream(new ByteArrayInputStream(bao.toByteArray())).readObject();
		} catch (Exception e) {
			log.info("bytes to Object fail", e);
			return null;
		}
	}
	
	protected boolean isNULL_OBJ(Object obj) {
        if (obj == null)
            return true;
        if (!(obj instanceof byte[]))
            return false;
        byte[] data = (byte[])obj;
        if (data.length == 0)
            return true;
        return false;
    }
	 
}
