package com.youfang.dao.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nutz.mvc.annotation.At;
import org.nutz.mvc.annotation.Ok;
import org.nutz.mvc.annotation.Param;

import com.youfang.web.annotation.Authority;
import com.youfang.web.annotation.EnumAuthority;

public class CacheStatusAction extends MemeoryCacheImpl {
	@At("/cache/status")
	@Ok("json")
	@Authority(value=EnumAuthority.Admin)
	public Object cache_status(){
		
		Map<String,Object> map=new HashMap<String,Object>();
		map.put("flushTimes", flushTimes);
		map.put("getTimes", getTimes);
		map.put("putTimes", putTimes);
		map.put("flushSql", getCache(cacheNameFlushSql).getKeys());
		map.put("cachelist", cacheManager.getCacheNames());  
		return map; 
	}
	
	 
	@SuppressWarnings("unchecked")
	@At("/cache/get/*")
	@Ok("json")
	@Authority(value=EnumAuthority.Admin)
	public Object cache_get(@Param("cachename") String cachename){
		
		Map<String,Object> map=new HashMap<String,Object>();
		 List<String> keys=new ArrayList<String>();
		 keys=getCache(cachename).getKeys();
		 map.put("keys", keys);
		 for(String key:keys){
			 Object value=getCache(cachename).get(key).getObjectValue();
			 map.put(key, value);
		 }
		return map; 
	}
	 
	@At("/cache/addlink/*")
	@Ok("json")
	@Authority(value=EnumAuthority.Admin)
	public void addlink(@Param("cachename") String cachename,@Param("linkname") String linkName){
		 
		addCacheLink(cachename,linkName);
	}
	 
	@At("/cache/addlink2tbs/*")
	@Ok("json")
	@Authority(value=EnumAuthority.Admin)
	public void addlink2tbs(@Param("linkname") String linkName){
		List<String> tableNames = new ArrayList<String>();
		tableNames.add("t_user");
		tableNames.add("t_houseinfo");
		tableNames.add("t_city");
		addCacheLink( tableNames,  linkName);
		 
	}
	
	@At("/cache/flushlink/*")
	@Ok("json")
	@Authority(value=EnumAuthority.Admin)
	public void flushLink(@Param("cachename") String cachename){
		
		flushCacheLinks(cachename); 
		 
	}
	 
	@At("/cache/getLink/*")
	@Ok("json")
	@Authority(value=EnumAuthority.Admin)
	public Object getLink(@Param("cachename") String cachename){
		
		return getCacheLink(cachename); 
		 
	}
}
