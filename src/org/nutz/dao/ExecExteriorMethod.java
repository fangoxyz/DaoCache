package org.nutz.dao;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.nutz.dao.sql.DaoStatement;
/**
 * 执行外部方法接口，
 * 实现该接口，并在mainmodule中将已实现该接口的对象注册到DaoExecutorImpl中，
 * 当每次执行SQL语句时，将迭代所有已注册的对象，并执行execute方法！
 * @author 小俊不乖
 *			modified by 面  on 2015-3-5
 */
public interface ExecExteriorMethod {

	/**
	 * 指示实现类的execute方法只在DaoExecuteImpl的execute之前执行
	 */
	public final static int BEFORE = 1;
	
	/**
	 * 指示实现类的execute方法只在DaoExecuteImpl的execute之后执行
	 */
	public final static int AFTER = 2;
	
	/**
	 * 指示实现类的execute方法围绕DaoExecuteImpl的execute前后执行
	 */
	public final static int AROUND = 3;
	
	
	/**
	 * 该方法与Daoexecutor接口提供的方法类似，该方法是在调用DaoExector的execute之前调用
	 * @param dataSource  @see DaoExecutor
	 * @param runner @see DaoExecutor
	 * @param sqls @see DaoExecutor
	 * @return 该布尔类型是指示在DaoExector执行了之后是否继续执行，false为返回数据，true为继续执行
	 */
	public boolean executeBefore(String sqls, DaoStatement st, Connection conn);
	
	/**
	 * 该方法与Daoexecutor接口提供的方法类似，该方法是在调用DaoExector的execute之后调用，
	 * @param dataSource  @see DaoExecutor
	 * @param runner @see DaoExecutor
	 * @param sqls @see DaoExecutor
	 */
	public void executeAfter(String sqls, DaoStatement st);
	
	/**
	 * 实现类可以实现该方法，该方法提供实现类允许在方法执行时传递数据
	 * @return
	 */
	public HashMap<String, Object> getTemp(); 
	
	public boolean canCache(String sqls);
	
	/**
	 * 获取实现类的执行方式
	 * @return
	 */
	public int getExecuteType();
	
	public void shutDown();
}
