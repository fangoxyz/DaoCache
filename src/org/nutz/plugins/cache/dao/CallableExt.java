package org.nutz.plugins.cache.dao;

import java.sql.Connection;
import java.util.concurrent.Callable;

import org.nutz.dao.sql.DaoStatement;

public abstract   class CallableExt implements Callable<Object> { 

    protected String sql; 
    protected Connection conn; 
	protected DaoStatement st; 

    public CallableExt(String sql) {
        this.sql= sql;
    }
  

    public String getSql() {
		return sql;
	}
     
    
    public CallableExt(String sql, Connection conn, DaoStatement st) {
		super();
		this.sql = sql;
		this.conn = conn;
		this.st = st;
	} 
    
	@Override
    public abstract   Object call() ;
	
	 
	public abstract void callAtRepeat();
	
	 
}

