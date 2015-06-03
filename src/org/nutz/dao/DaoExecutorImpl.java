package org.nutz.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.util.List;

import org.nutz.dao.impl.sql.run.NutDaoExecutor;
import org.nutz.dao.sql.DaoStatement;
import org.nutz.dao.sql.SqlType;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;
import org.nutz.plugins.cache.dao.CallableExt;
import org.nutz.plugins.cache.dao.ThreadPoolExt;
import org.nutz.trans.Trans;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;
import com.alibaba.druid.sql.dialect.postgresql.parser.PGSQLStatementParser;
import com.alibaba.druid.sql.dialect.sqlserver.parser.SQLServerStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.mysql.jdbc.Blob;
import com.mysql.jdbc.Clob;

/**
 * 重写daoexecutor，提供事件注册给外部调用，每当执行到该实现类时，轮询并执行对应的已注册信息。
 * @author 小俊不乖
 *		   modified by 面  on 2015-3-5
 */
@IocBean
public class DaoExecutorImpl extends NutDaoExecutor {
	
	private static final Log log = Logs.get();
	
	private static final  ThreadPoolExt exec = new ThreadPoolExt();
	 
	/**
	 * 外部已注册的列表
	 */
	private static   ExecExteriorMethod  execMethod =null ;

	private String toExampleStatement(Object[][] mtrx, String sql) {

        StringBuilder sb = new StringBuilder();

        String[] ss = sql.split("[?]");

        int i = 0;

        if (mtrx.length > 0) {

            for (; i < mtrx[0].length; i++) {

                sb.append(ss[i]);

                Object obj = mtrx[0][i];

                if (obj != null) {

                    if (obj instanceof Blob) {

                        Blob blob = (Blob) obj;

                        obj = "Blob(" + blob.hashCode() + ")";

                    } else if (obj instanceof Clob) {

                        Clob clob = (Clob) obj;

                        obj = "Clob(" + clob.hashCode() + ")";

                    } else if (obj instanceof byte[] || obj instanceof char[]) {

                        if (Array.getLength(obj) > 10240)

                            obj = "*BigData[len=" + Array.getLength(obj) + "]";

                    } else if (obj instanceof InputStream) {

                        try {

                            obj = "*InputStream[len=" + ((InputStream) obj).available() + "]";

                        }

                        catch (IOException e) {}

                    } else if (obj instanceof Reader) {

                        obj = "*Reader@" + obj.hashCode();

                    }

                }

                sb.append(Sqls.formatFieldValue(obj));

            }

        }

        if (i < ss.length)

            sb.append(ss[i]);

 

        return sb.toString();

    }

	protected DB db = DB.MYSQL;

	protected SQLStatementParser sqlParser(String sql) {
		switch (db) {
		case MYSQL:
			return new MySqlStatementParser(sql);
		case SQLSERVER:
			return new SQLServerStatementParser(sql);
		case ORACLE:
			return new OracleStatementParser(sql);
		case PSQL:
			return new PGSQLStatementParser(sql);
		default:
			throw new DaoException("daocache not support at this database");
		}
	}
	
	
	 
	public void exec(Connection conn, DaoStatement st) {
		/*
		 * 执行execute方法前执行所有已注册的对象，
		 * 如果实现类要求不执行下面的方法，需要执行完所有的已注册对象，
		 * 然后终止！。
		 */
		String prepSql = st.toPreparedStatement();
		String sql;
		if (prepSql == null) {
			super.exec(conn, st);
			return;
		}
		SQLStatementParser parser = sqlParser(prepSql);
		//log.debug("st=====" + prepSql);
		List<SQLStatement> statementList = null;
		try {
            statementList = parser.parseStatementList();
        }
        catch (Exception e) {
            log.debug("parser SQL sql, skip cache detect!! SQL=" + prepSql);
            super.exec(conn, st);
            return;
        }
		if (statementList.size() != 1) {
			log.warn("more than one sql in one DaoStatement!! skip cache detect!!");
			super.exec(conn, st);
			return;
		}
		SQLStatement sqlStatement = statementList.get(0);
		if (sqlStatement == null) {
			log.warn("can't parse SQL !! skip cache detect!! SQL=" + prepSql);
			super.exec(conn, st);
			return;
		}
		sql=toExampleStatement(st.getParamMatrix(),st.toPreparedStatement());
		//long starttime=System.currentTimeMillis();
		
		 
		if(executeBefore(sql,conn,st)){ //1.首先到缓存中去取数据
			return;                      //2.如果缓存中有数据，则退出，不去数据库取
		}
		
		if (st.getSqlType()==SqlType.SELECT && Trans.isTransactionNone() && execMethod.canCache(sql)==true){  //3.如果缓存中没数据并且是select语句,则用Futer模式到数据库中去取
			 exec.addTaskAndWaitResult(new CallableExt(sql,conn,st){ 
				
				@Override
			    public Object call()  {							     //如果线程中池没有当前sql语句执行，则执行这里
					execuSql(this.conn, this.st);					//这里执行select 语句进数据读数据
					executeDBAfter(this.sql,this.conn,this.st);    //这里包括写入缓存
					return "OK";								   //表示本线程执行完毕
			    }
				
				public void callAtRepeat(){							 //如果线程中池有当前sql语句执行，则执行这里，这里阻塞，等待正在执行的sql语行完，然后去缓存中取数据
					if(executeBefore(this.sql,this.conn,this.st)){ //1.首先到缓存中去取数据
						return;                      //2.如果缓存中有数据，则退出，不去数据库取
					}     
					execuSql(this.conn, this.st);					//这里执行select 语句进数据读数据
					executeDBAfter(this.sql,this.conn,this.st);    //这里包括写入缓存 
					
				}
			});
			 
		}else{									//非select模式，直接执行就行了. 这里待考虑
			execuSql(conn, st);                     //执行sql语句
			executeDBAfter(sql,conn,st);			//执行完后对写入缓存或清空缓存
		}
		 
		//long endtime=System.currentTimeMillis();
		//System.out.println("用时:" + (endtime-starttime) + "ms");
	}
	
	private void execuSql(Connection conn, DaoStatement st){
		//log.warn("execuSql"  );
		super.exec(conn, st);
	}
	
	private boolean executeBefore(String sql,Connection conn, DaoStatement st){
		//log.warn("executeBefore"  );
		if(execMethod.getExecuteType() == ExecExteriorMethod.BEFORE ||
				execMethod.getExecuteType() == ExecExteriorMethod.AROUND){
			if(!execMethod.executeBefore(sql,st,conn)){
				return true;
			}
		}
		return false;
	}
	private void executeDBAfter(String sql,Connection conn, DaoStatement st){
		//log.warn("执行了数据库" + exec.getNewThreadId()) ;
		//log.warn("executeDBAfter"  );
		if(execMethod.getExecuteType() == ExecExteriorMethod.AFTER ||
				execMethod.getExecuteType() == ExecExteriorMethod.AROUND){
			execMethod.executeAfter(sql,st);
		}
	}
	
	 
	public void shutDown() {
		// TODO Auto-generated method stub
		log.warn(  " exec is shutdown...");
		exec.purgeCancelTask(); 
		exec.stop(); 
		execMethod.shutDown();
	} 
	 
	
}
