package org.nutz.plugins.cache.dao;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLHint;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlExecuteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;

public class NSqlAdapter extends MySqlASTVisitorAdapter {

    public List<String> tableNames = new ArrayList<String>();
    public  SQLBinaryOpExpr wheres =  null;
    public List<SQLUpdateSetItem> setItems=new ArrayList<SQLUpdateSetItem>();
    public List<SQLSelectItem> selList=new ArrayList<SQLSelectItem>();
    public boolean visit(SQLExprTableSource x) {
    	tableNames.add(x.toString());
    	
        return super.visit(x);
    }

	@Override
	public void endVisit(MySqlSelectQueryBlock x) {
		// TODO Auto-generated method stub
		if (x.getWhere()!=null){
			Class<?> clz = x.getWhere().getClass();
			if (clz.equals(SQLBinaryOpExpr.class)){
				wheres= (SQLBinaryOpExpr) x.getWhere();
				selList=x.getSelectList();
			}
		}
		super.endVisit(x);
	}

	@Override
	public boolean visit(MySqlDeleteStatement x) {
		// TODO Auto-generated method stub
		if (x.getWhere()!=null){
			Class<?> clz = x.getWhere().getClass();
			if (clz.equals(SQLBinaryOpExpr.class)){
				wheres= (SQLBinaryOpExpr) x.getWhere();
			}
		}
		return super.visit(x);
	}

	@Override
	public boolean visit(MySqlUpdateStatement x) {
		// TODO Auto-generated method stub
		if (x.getWhere()!=null){
			Class<?> clz = x.getWhere().getClass();
			if (clz.equals(SQLBinaryOpExpr.class)){
				wheres= (SQLBinaryOpExpr) x.getWhere();
			}
		}
		setItems=x.getItems();
		//tableNames.add(x.toString());
		return super.visit(x);
	}

	 
    
}
