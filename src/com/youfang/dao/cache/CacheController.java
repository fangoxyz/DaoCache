package com.youfang.dao.cache;

import java.util.List;

/**
 * 
* @ClassName: CacheController    
* @Description: 缓存操作接口，提供对缓存数据的增删查，如要实现对其他缓存操作，需实现该接口
* @author 小俊不乖
* 		  modified by 面  on 2015-3-5
* @date 2012-7-6 上午10:14:01
 */
public interface CacheController {
	public boolean isReady();
	/**
	* @Title: flush    
	* @Description: 清理该域所有的数据
	* @param cacheModel   域模型
	* @return void 
	* @throws
	* @author 小俊不乖
	 */
	 
	public void flush(CacheModel cacheModel);
	
	/**
	 * 
	* @Title: removeObject    
	* @Description: 清理指定域的指定对象    
	* @param @param cacheModel
	* @param @param key    设定文件    
	* @return void    返回类型    
	* @throws
	* @author 小俊不乖
	 */
	public void removeObject(CacheModel cacheModel, Object key);
	
	/**
	 * 
	* @Title: getObject    
	* @Description: 获取域中的指定对象
	* @param  cacheModel
	* @param key
	* @return Object    返回对象  
	* @throws
	* @author 小俊不乖
	 */
	public Object getObject(CacheModel cacheModel, Object key);
	/**
	 * 
	* @Title: putObject    
	* @Description: 向域中存放对象
	* @param cacheModel
	* @param key 对应的key
	* @param object    需存放的对象
	* @return void  
	* @throws
	* @author 小俊不乖
	 */
	public void putObject(CacheModel cacheModel, Object key, Object object);
	public void shutDown();

	public void putLog(String key, String string);
	
	
	/*
	* @Title: getCacheLink    
	* @Description: 取某个表的关联关系
	* @param String 缓存域名或表名
	* @return List<String> 返回一个关联关系的列表
	* @author 面 
	* @date 2015-5-18
	 */
	public List<String> getCacheLink(String tableName); 
	/**
	* @Title: existsCacheLink    
	* @Description: 某个缓存域中是否有另外一个缓存的关联关系
	* @param String tableName 主缓存域   linkName 关联的缓存域名或表名
	* @return Boolean 
	* @author 面 
	* @date 2015-5-18
	 */
	public Boolean existsCacheLink(String tableName,String linkName);  
	/**
	* @Title: addCacheLink    
	* @Description: 向某个缓存域中增加另一个缓存的关联关系
	* @param String tableName 主缓存域   linkName 关联的缓存域名或表名
	* @return void 
	* @author 面 
	* @date 2015-5-18
	 */
	public void addCacheLink(String tableName,String linkName);
	/**
	* @Title: addCacheLink    
	* @Description: 向指定的缓存域列表中增加另一个缓存的关联关系
	* @param List  Caches 一个缓存域的列表,   linkName 关联的缓存域名或表名
	* @return void 
	* @author 面 
	* @date 2015-5-18
	 */
	public void addCacheLink(List<String> Caches,String linkName); 
	/**
	* @Title: flushCacheLinks    
	* @Description: 清除指定的缓存域所有关联缓存中的数据
	* @param String tableName  缓存域名或表名
	* @return void 
	* @author 面 
	* @date 2015-5-18
	 */
	public void flushCacheLinks(String tableName);
}
