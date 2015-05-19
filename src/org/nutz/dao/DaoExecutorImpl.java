package org.nutz.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nutz.dao.entity.LinkVisitor;
import org.nutz.dao.impl.entity.NutEntity;
import org.nutz.dao.impl.jdbc.NutPojo;
import org.nutz.dao.impl.sql.NutStatement;
import org.nutz.dao.impl.sql.run.NutDaoExecutor;
import org.nutz.dao.sql.DaoStatement;
import org.nutz.dao.sql.Pojo;
import org.nutz.dao.sql.PojoCallback;
import org.nutz.ioc.loader.annotation.IocBean;
import org.nutz.log.Log;
import org.nutz.log.Logs;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.oracle.parser.OracleStatementParser;
import com.alibaba.druid.sql.dialect.postgresql.parser.PGSQLStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.mysql.jdbc.Blob;
import com.mysql.jdbc.Clob;
import com.youfang.dao.cache.CacheExecExterior;

/**
 * 重写daoexecutor，提供事件注册给外部调用，每当执行到该实现类时，轮询并执行对应的已注册信息。
 * @author 小俊不乖
 *		   modified by 面  on 2015-3-5
 */
@IocBean
public class DaoExecutorImpl extends NutDaoExecutor {
	
	private static final Log log = Logs.get();
	
	 
	/**
	 * 外部已注册的列表
	 */
	private static  List<ExecExteriorMethod> list = new ArrayList<ExecExteriorMethod>();

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
		if (prepSql == null) {
			super.exec(conn, st);
			return;
		}
		SQLStatementParser parser = sqlParser(prepSql);
		List<SQLStatement> statementList = parser.parseStatementList();
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
		
		long starttime=System.currentTimeMillis();
	      
		boolean isReturn = false;
		for (int i = 0; i < list.size(); i++) {
			if(list.get(i).getExecuteType() == ExecExteriorMethod.BEFORE ||
					list.get(i).getExecuteType() == ExecExteriorMethod.AROUND){
				if(!list.get(i).executeBefore(toExampleStatement(st.getParamMatrix(),st.toPreparedStatement()),st,conn)){
					isReturn = true;
				}
			}
		}
		if(isReturn){
			return;
		}
		super.exec(conn, st);
	
		/*
		 * execute完成后，执行所有已注册的对象
		 */
		for (int i = 0; i < list.size(); i++) {
			if(list.get(i).getExecuteType() == ExecExteriorMethod.AFTER ||
					list.get(i).getExecuteType() == ExecExteriorMethod.AROUND){
				list.get(i).executeAfter(toExampleStatement(st.getParamMatrix(),st.toPreparedStatement()),st);
			}
		}
		long endtime=System.currentTimeMillis();
		System.out.println("用时:" + (endtime-starttime) + "ms");
	}
	
}
