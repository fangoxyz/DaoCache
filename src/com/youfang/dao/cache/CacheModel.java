package com.youfang.dao.cache;
/**
* @ClassName: CacheModel    
* @Description: 缓存模型，可以认为这是一个域    
* @author 小俊不乖
*         modified by 面  on 2015-3-5
* @date 2012-7-6 上午10:04:30
 */
public class CacheModel {

	/**
	 * 域名称
	 */
	private String name;
	
	/**
	 * 该域的数据最大个数
	 */
	private Integer maxElements;
	
	 
	
	public CacheModel(){
		
	}
	

 
	
	public CacheModel(String name,Integer maxElements){ 
		this.name = name;
		this.maxElements = maxElements;
	}
	public CacheModel(String name){ 
		this.name = name;
		this.maxElements = null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	 
	 
	public Integer getMaxElements() {
		return maxElements;
	}


	public void setMaxElements(Integer maxElements) {
		this.maxElements = maxElements;
	}
 
}
