package org.nutz.plugins.cache.dao;

import java.util.concurrent.FutureTask;

/**
 * Created by apple on 15/5/26.
 */
public abstract class FutureTaskExt extends FutureTask<Object> {
    public FutureTaskExt(CallableExt callable) { 
        super(callable);
        this.sql=callable.getSql();
    } 
    private String sql; 
	 
	public String getSql() {
		return sql;
	}

	public abstract void callAtRepeat();
	
}
