package com.youfang.dao.cache;

import java.util.List;

/**
* @ClassName:WhereModel    
* @Description:Where 条件模型    
* @author  面
* @date     2015-5-10
 */
public class WhereModel {

	private List<String> tableNames; 

	/**
	 * 域名称
	 */
	private String left;
	
	/**
	 * 该域的数据最大个数
	 */
	private String op;
	
	private String right;
	
	private String sel;
	

	
	public String getSel() {
		return sel;
	}


	public void setSel(String sel) {
		this.sel = sel;
	}


	public WhereModel(String left) {
		super();
		this.left = left;
		this.op = null;
		this.right = null;
		this.sel=null;
		this.tableNames=null;
	}


	public WhereModel(String left, String op) {
		super();
		this.left = left;
		this.op = op;
		this.right = null;
		this.sel=null;
		this.tableNames=null;
	}

	public WhereModel(String left, String op, String right) {
		super();
		this.left = left;
		this.op = op;
		this.right = right;
		this.sel=null;
		this.tableNames=null;
	}
	
	

	public WhereModel(String left, String op, String right, String sel) {
		super();
		this.left = left;
		this.op = op;
		this.right = right;
		this.sel = sel;
		this.tableNames=null;
	}
	
	public WhereModel(String left, String op, String right, String sel,List<String> tables) {
		super();
		this.left = left;
		this.op = op;
		this.right = right;
		this.sel = sel;
		this.tableNames=tables;
	}
	public WhereModel(List<String> tables) {
		super();
		this.left = null;
		this.op = null;
		this.right = null;
		this.sel = null;
		this.tableNames=tables;
	}


	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}
	 
	public List<String> getTableNames() {
		return tableNames;
	}


	public void setTableNames(List<String> tableNames) {
		this.tableNames = tableNames;
	}
	 
  
}
