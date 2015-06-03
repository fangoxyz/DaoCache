package com.youfang.dao.cache;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.nutz.dao.ExecExteriorMethod;
import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.MappingField;
import org.nutz.dao.entity.annotation.Table;
import org.nutz.dao.entity.annotation.View;
import org.nutz.dao.sql.DaoStatement;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.plugins.cache.dao.CacheResult;
import org.nutz.plugins.cache.dao.NSqlAdapter;
import org.nutz.plugins.cache.dao.ThreadPoolExt;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.fastjson.JSON;
import com.youfang.util.encrypt.EncryptUtil;

/**
 * 实现缓存
 * @author 小俊不乖
 *			modified by 面  on 2015-3-5
 */
public class CacheExecExterior implements ExecExteriorMethod {

	 
	/**
	 * 缓存域数组,表名做key，方便在执行SQL的时候读取表名
	 */
	private static Map<String, CacheModel> cacheModelMap = new HashMap<String, CacheModel>(); 
	
	/**
	 * 是否启用缓存，默认不启用
	 */
	private static boolean isEnableCache = false;
	
	/**
	 * 需要加入缓存的类地址和缓存时间
	 */
	private List<List<String>> clazzList;
	private Map<String, Set<String>> cacheLinks;
	
	/**
	 * 执行方式
	 */
	private static final int EXECUTETYPE = ExecExteriorMethod.AROUND;
	
	private static CacheController cacheController = null;
	
	private static Log log = Logs.getLog(CacheExecExterior.class);
	
	public void init(){
		while (!isReady()){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.info( " Cache is started... ");
		//log.info("直接操作了cachelinks");
		for(String link :this.cacheLinks.keySet()){
			cacheController.putObject(new CacheModel("cachelinks"),link, cacheLinks.get(link)); 
		} 
	}
	public boolean canCache(String sqls){
		WhereModel where=getOneWhere(sqls);
		if (where==null) return false;
		List<String> tableNames =where.getTableNames();
		//List<String> tableNames = getTableFromSql(sqls.toString()); 
		for(String tableName:tableNames){
			if(tableName == null || "".equals(tableName)){
				return false;
			} 
			if(cacheModelMap.get(tableName) == null){
				 
				return false;
			}
		}
		return true;
	}
	 
	/*
	 * 当执行了数据库操作后,"insert"、"update"、"delete"则清理数据,"select"则缓存数据
	 */
	public void executeAfter(String sqls, DaoStatement st) {
		if(!isEnableCache){
			return;
		}
		 
		try{ 
			//用druid分析sql语句，分析结果存在wher中
			WhereModel where=getOneWhere(sqls.toString());  
			//如果sql分析结果为null,即为不可分析的sql语句，则不做任何操作.
			if (where ==null ){
				return; 	
			}
			Boolean canCache=true;
			Boolean key=false;  
			String left=where.getLeft();
			String oper=where.getOp();
			String sel1=where.getSel();
			String rightWhere=where.getRight();
			List<String>  tables=where.getTableNames(); 
			String type = getSqlExecuteType(sqls.toString()); 
			/**
			 * 判断此条sql语句是否是主键sql
			 */
			if(("insert".equalsIgnoreCase(type) || "update".equalsIgnoreCase(type) || "delete".equalsIgnoreCase(type)) && left!=null && oper!=null && oper.equalsIgnoreCase("equality")  && tables!=null && tables.size()==1 ){
				key=isKey(st,left,tables.get(0));
			}else if( "select".equalsIgnoreCase(type) && left!=null && oper!=null && oper.equalsIgnoreCase("equality")&& sel1.equalsIgnoreCase("*") && tables!=null && tables.size()==1  ){ 
				key=isKey(st,left,tables.get(0));
			} 
			
			for(String tableName:tables){
				/**
				 * 遍历sql中涉及的所有表,如果有一个表不在缓存域中，则不能缓存
				 * 此处主要是为了在select语句时缓存数据
				 */ 
				if(tableName == null || "".equals(tableName)){
					canCache=false; 
					break;
				} 
				if( cacheModelMap.get(tableName) == null){
					canCache=false; 
					break;
				}
				 
				if("insert".equalsIgnoreCase(type) || "update".equalsIgnoreCase(type) || "delete".equalsIgnoreCase(type)){
					String op=(String) st.getContext().attr("cache-flush"); 
					if (op!=null && ("no").equalsIgnoreCase(op)){
						log.debug("sql:" + sqls.toString() + ",启用[cache-flush=no]直接修改缓存,不清除域数据：" + tableName); 
						return;
						//此sql注明不清除缓存，则直接跳出
					}else{
						log.debug("sql:" + sqls.toString() + ",删除域相关数据：" + tableName);   
						if (key){
							/**
							 * 1.如果是update 或者 delete的主键操作 清除L2全部以及L1的单条数据
							 * 2.由于insert into时 iskey不可能是ture，所以此处不处理
							 */
							cacheController.flush(new CacheModel(tableName));  
							cacheController.removeObject(new CacheModel(genL1CacheName(tableName)), rightWhere);
							log.debug("清除L1缓存" + genL1CacheName(tableName) + "中id为" + rightWhere + "的一条及l2中的所有：" + tableName +"  sql:"+ sqls.toString());
						}else{
							/**
							 * 1.如果是insert操作 则清除 L2，不对L1产生影响
							 * 2.如果是update或delete的非主键操作 则清除L2和L1
							 */
							cacheController.flush(new CacheModel(tableName));
							if( "update".equalsIgnoreCase(type) || "delete".equalsIgnoreCase(type)){ 
								cacheController.flush(new CacheModel(genL1CacheName(tableName)));
							}
							log.debug("清除L1缓存 " + genL1CacheName(tableName) + " 所有数据，及 l2 " + tableName +" 中的所有数据   sql:"+ sqls.toString());
						} 
						cacheController.putLog(sqls.toString(),""); 
					}
				}
			}
			
			if( "select".equalsIgnoreCase(type) && canCache && tables!=null && tables.size()>=1){ 
				if (key){
					/**
					 * 如果select语句是根据主键查询的 ，那么向L1中写数据，并且 key是where id=1111部分中的1111
					 */ 
					cacheController.putObject(new CacheModel(genL1CacheName(tables.get(0)),cacheModelMap.get(tables.get(0)).getMaxElements()),rightWhere, st.getResult()); 
					log.debug("缓存域数据到L1：" + genL1CacheName(tables.get(0)) + " key:" + rightWhere + "  sql:"+ sqls.toString());
				}else{
					/**
					 * 如果是非主键的select 语句，那么向L2中写数据,key是根据Sha256(sql)
					 * 
					 */
					cacheController.addCacheLink(tables, genL2CacheName(tables));
					cacheController.putObject(new CacheModel(genL2CacheName(tables),cacheModelMap.get(tables.get(0)).getMaxElements()),EncryptUtil.Sha256(sqls.toString(),null), st.getResult());
					log.debug("缓存域数据到L2：" + genL2CacheName(tables) + " key:" + EncryptUtil.Sha256(sqls.toString(),null) + "  sql:"+ sqls.toString());
				} 
			}
				 
		}catch (Throwable e) {
			if (log.isDebugEnabled())
				log.debug("executeAfter E!!", e);
			
		}	
	}

	public boolean executeBefore(String sqls,  DaoStatement st, Connection conn ) {
		if(!isEnableCache){
			return true;
		}
		try{
			/*
			 * 如果数组只有一个对象，并且是select语句，就查询缓存数据。
			 * 如果数组有多个对象，不查询缓存数据
			 */ 
			if(sqls != null && !"".equals(sqls)){
				/*
				 *获取表名，判断是否已经加入域 
				 */
				 
				WhereModel where=getOneWhere(sqls.toString());
				if (where==null) return true;
				List<String> tableNames =where.getTableNames();
				//List<String> tableNames = getTableFromSql(sqls.toString()); 
				for(String tableName:tableNames){
					if(tableName == null || "".equals(tableName)){
						return true;
					} 
					if(cacheModelMap.get(tableName) == null){
						 
						return true;
					} 
				}
				/*
				 * 判断是否查询
				 */ 
				if("select".equals(getSqlExecuteType(sqls.toString())) && null!=tableNames && tableNames.size()>=1  ){
					Object object = null;
					Boolean key=false;
					String  rightWhere="";
					
					if (where !=null ){
						String left=where.getLeft();
						String op=where.getOp();
						String sel1=where.getSel();
						rightWhere=where.getRight();  
						if (left!=null && op!=null && op.equalsIgnoreCase("equality") && sel1.equalsIgnoreCase("*")   && tableNames.size()==1  ){
							key=isKey(st,left,tableNames.get(0)); 
						}
					} 
					
					
					
					if (key){
						object = cacheController.getObject(new CacheModel(genL1CacheName(tableNames.get(0)),cacheModelMap.get(tableNames.get(0)).getMaxElements()),rightWhere);
						 
					}else{
						object = cacheController.getObject(new CacheModel(genL2CacheName(tableNames),cacheModelMap.get(tableNames.get(0)).getMaxElements()), EncryptUtil.Sha256(sqls.toString(),null));
						
					}  
					if(object != null  && !CacheResult.NOT_FOUNT.equals(object)){ 
						 if (CacheResult.NULL.equals(object)){
							 object = null; 
						 }
						 st.getContext().setResult(object); 
						 if (key){
							  log.debug("已获取缓存数据。从L1缓存"+ genL1CacheName(tableNames.get(0)) + "  key:" + rightWhere +   "  sql:"+ sqls.toString());
						 }else{
							  log.debug("已获取缓存数据。从L2缓存"+ genL2CacheName(tableNames) + "  key:" + EncryptUtil.Sha256(sqls.toString(),null) + "  sql:"+ sqls.toString());
						 }
						return false;
					}
					
					return true;
				}
				
			}
			return true;
		}catch (Throwable e) {
			log.error(e);
			return true;  
		}
	}
 
	
	public int getExecuteType() {
		return EXECUTETYPE;
	}

	public HashMap<String, Object> getTemp() {
		return null;
	}

	/**
	 * 
	* @Title: getTableFromSql    
	* @Description: 获取sql中的表名  
	* @param @param sqlKey
	* @param @return    设定文件    
	* @return String    返回类型    
	* @throws
	* @author 小俊不乖
	 */
	private static List<String> getTableFromSql(String sql) {
		/*
		 * 将用户输入的相关的清理掉，空替换成@，转换成小写
		 * 然后使用from@进行定位，替换之前的所有字符，定位下一个@字符，然后获取表名
		 */
		List<String> ret = new ArrayList<String>();
		sql = sql.replaceAll("[\"|'].*[\"|']", "").replaceAll("\\s+", "@").toLowerCase();
		String type = getSqlExecuteType(sql);
		if("update".equals(type)){
			sql = sql.substring(7, sql.length() );
			ret.add(sql.substring(0,sql.indexOf("@")));
			return ret;
		}else if("insert".equals(type)){
			//如果下个词是into，读取下一个词，否则返回下个词
			sql = sql.substring(7, sql.length() );
			if("into".equals(sql.substring(0, sql.indexOf("@")))){
				ret.add(sql.substring(5, sql.indexOf("(")));
				return ret;
				 
			}else{
				ret.add(sql.substring(0, sql.indexOf("(")));
				return ret; 
			}
		}
		sql = sql.substring(sql.indexOf("from@") + 5, sql.length());
		if(sql.indexOf("@") < 0){
			ret.add(sql);
			return ret; 
		}
		ret.add(sql.substring(0, sql.indexOf("@")));
		int tab_index=0;
		while(sql.indexOf("from@") > 0){ 
			sql = sql.substring(sql.indexOf("from@") + 5, sql.length());
			if(sql.indexOf("@") < 0){
				ret.add(sql);
				return ret; 
			}
			ret.add(sql.substring(0, sql.indexOf("@")));
		}
		 
		return ret;  
	}
	
	public static void main (String[] args){
		CacheExecExterior ce=new CacheExecExterior();
		 
		WhereModel wm=ce.getOneWhere("update   t_housepicture set value=1111  where id=111");
		log.debug(JSON.toJSON(wm));
	}
	 
	
	private   WhereModel getOneWhere(String sql){ 
		 
			SQLStatementParser parser = new MySqlStatementParser(sql);
		
			List<SQLStatement> statementList = null;
			
			try {
	            statementList = parser.parseStatementList();
	        }
	        catch (Exception e) {
	            log.debug("parser SQL sql, skip cache detect!! SQL=" + sql); 
	            return null;
	        }
			
			if (statementList.size() != 1) {
				log.warn("more than one sql in one DaoStatement!! skip cache detect!!"); 
				return null;
			} 
			SQLStatement sqlStatement = statementList.get(0);
			if (sqlStatement == null) {
				log.warn("sqlStatement is null,can't parse SQL !! skip cache detect!! SQL=" + sql);  
				return null;
			}
			// 检查需要执行的sql
			NSqlAdapter adapter = new NSqlAdapter();
			sqlStatement.accept(adapter); // 得到将会操作的表 
			List<String> tableNames = adapter.tableNames;  
			//log.debug("tablename:" + tableNames);
			if (adapter.wheres ==null) {
				return new WhereModel(tableNames);
			}
			String left=adapter.wheres.getLeft()==null?null:adapter.wheres.getLeft().toString();
			String op=adapter.wheres.getOperator()==null?null:adapter.wheres.getOperator().toString();
			String right=adapter.wheres.getRight()==null?null:adapter.wheres.getRight().toString();
			String sel1="";
			 
			if (op==null ){
				return new WhereModel(tableNames);
			}
			if (!op.equalsIgnoreCase("equality")){
				return new WhereModel(tableNames);
			}
			List<SQLSelectItem> selList=adapter.selList;
			if (selList!=null){ 
				if (selList.size()==1 ){
					sel1=selList.get(0).getExpr().toString(); 
				}
			} 
			return new WhereModel(left,op,right,sel1,tableNames); 
		 
	}
	
	private Boolean isKey(DaoStatement st,String field,String cacheName){ 
		 
		Boolean isID=false;
		if (field==null ) return false;
		Entity<?> entityPojo=st.getEntity(); 
		MappingField fieldID=entityPojo.getIdField(); 
		String pojoTable=entityPojo.getTableName();
		if (pojoTable==null) return false;
		//如果缓存名是视图，则不创建一级缓存，视图永远没有主键
		if (!pojoTable.equalsIgnoreCase(cacheName)) return false;
		
		if (fieldID!=null  && fieldID.getColumnName()!=null) { 
			isID= fieldID.getColumnName().equalsIgnoreCase(field);
		}
		
		
		return isID ;
	}
	 
	private static String genL1CacheName(String tableName) { 
		if (tableName==null) return null; 
		return tableName+"_L1___";
	}
	
	private static String genL2CacheName(List<String> tableNames) { 
		if (tableNames==null) return null;
		if (tableNames.size()<=0) return null;
		String cacheName="";
		for(String table:tableNames){
			cacheName=cacheName + table +"##";
		}
		return cacheName.substring(0,cacheName.length()-2);
	}
	 
	/**
	 * 
	* @Title: getSqlExecuteType    
	* @Description: 获取SQL是查询，删除，等操作
	* @param @param sql
	* @param @return    设定文件    
	* @return String    返回类型    
	* @throws
	* @author zengjun
	 */
	private static String getSqlExecuteType(String sql){
		/*
		 * 过滤用户输入，替换空格，转换成小写
		 * 从0索引到第一个@符号截取字符串
		 */
		sql = sql.replaceAll("[\"|'].*[\"|']", "").replaceAll("\\s+", "@").toLowerCase();
		return sql.substring(0, sql.indexOf("@"));
	}
	public List<List<String>> getClazzList() {
		return clazzList;
	}
	
	public boolean isReady(){
		return cacheController.isReady();
	}
	
	 
	
	public void setCacheLinks(Map<String, Set<String>> cacheLinks) { 
		this.cacheLinks=cacheLinks;
	}
	public void setClazzList(List<List<String>> clazzList) {
		//目前调试起来是单例的，以后不晓得会不会加单例的控制，这里控制一下
		if(this.clazzList != null || clazzList == null){
			return;
		}
		Serializable serializable = null;
		for(int i = 0;i<clazzList.size();i++){
			try {
				try {
					serializable = (Serializable)Class.forName(clazzList.get(i).get(0)).newInstance();
					
				} catch (Exception e) {
					
				}
				if(serializable == null){
					log.error(clazzList.get(i).get(0) + " 这个类没有实现 java.io.Serializable ");
					continue;
				}
				Table table = Class.forName(clazzList.get(i).get(0)).getAnnotation(Table.class);
				View view = Class.forName(clazzList.get(i).get(0)).getAnnotation(View.class);
				if(table == null ){
					log.error(clazzList.get(i).get(0) + " 未使用@Table这个注解，不能创建域。");
					continue;
				}
				if(table != null ){
					cacheModelMap.put(table.value(), new CacheModel(table.value(),
						Integer.parseInt(String.valueOf(clazzList.get(i).get(1)))));
				} 
				log.info(clazzList.get(i).get(0) + " 加入到域成功"); 
				
				if(view == null ){
					log.warn(clazzList.get(i).get(0) + " 未使用@View这个注解，不能创建域。");
					continue;
				}
				cacheModelMap.put(view.value(), new CacheModel(view.value(),
						Integer.parseInt(String.valueOf(clazzList.get(i).get(1)))));
				log.info(clazzList.get(i).get(0) + " 加入到域成功");
			} catch (ClassNotFoundException e) {
				log.error(e);
			}
			
		}
		this.clazzList = clazzList;
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub
		log.warn(  " ehcache is shutdown...");
		cacheController.shutDown();
	} 
	
}
