package org.nutz.dao.impl;

import javax.sql.DataSource;

import org.nutz.dao.Condition;
import org.nutz.dao.SqlManager;
import org.nutz.dao.entity.LinkField;
import org.nutz.dao.entity.LinkVisitor;
import org.nutz.dao.impl.sql.pojo.ConditionPItem;
import org.nutz.dao.sql.Criteria;
import org.nutz.dao.sql.PItem;
import org.nutz.dao.sql.Pojo;
import org.nutz.dao.util.Pojos;
import org.nutz.dao.util.cri.SqlExpressionGroup;
import org.nutz.lang.ContinueLoop;
import org.nutz.lang.Each;
import org.nutz.lang.ExitLoop;
import org.nutz.lang.Lang;
import org.nutz.lang.LoopException;
/**
 * 
* @ClassName: NutDaoExt    
* @Description: 重写NutDao的fetchlinks以实现fetchlinks缓存
* @author   面  
* @date  2015-5-8
 */
public class NutDaoExt extends org.nutz.dao.impl.NutDao {
	
 
	public NutDaoExt() {
		super();
		// TODO Auto-generated constructor stub
	}

	public NutDaoExt(DataSource dataSource, SqlManager sqlManager) {
		super(dataSource, sqlManager);
		// TODO Auto-generated constructor stub
	}

	public NutDaoExt(DataSource dataSource) {
		super(dataSource);
		// TODO Auto-generated constructor stub
	}
	
 
    @Override
    public <T> T fetchLinks(final T obj, final String regex, final Condition cnd) {
        if (null == obj)
            return null;
        Lang.each(obj, false, new Each<Object>() {
            public void invoke(int index, Object ele, int length)
                    throws ExitLoop, ContinueLoop, LoopException {
                EntityOperator opt = _optBy(ele);
                if (null == opt)
                    return;
                opt.entity.visitMany(ele, regex, doLinkQuery(opt, cnd));
                opt.entity.visitManyMany(ele, regex, doLinkQuery(opt, cnd));
                opt.entity.visitOne(ele, regex, doFetch(opt));
                opt.exec();
            }
        });
        
        return obj;
    }
    
    private LinkVisitor doFetch(final EntityOperator opt) {
        return new LinkVisitor() {
            public void visit(final Object obj, final LinkField lnk) {
                Pojo pojo = opt.maker().makeQuery(lnk.getLinkedEntity());
                pojo.setOperatingObject(obj);
                pojo.append(Pojos.Items.cnd(lnk.createCondition(obj)));
                pojo.setAfter(lnk.getCallback());
                _exec(pojo);
                lnk.setValue(obj, pojo.getObject(Object.class));
            }
        };
    }
 
  	    private LinkVisitor doLinkQuery(final EntityOperator opt, final Condition cnd) {
    	         return new LinkVisitor() {
    	             public void visit(final Object obj, final LinkField lnk) {

    	                Pojo pojo = opt.maker().makeQuery(lnk.getLinkedEntity());
    	                pojo.setOperatingObject(obj);
    	                PItem[] _cndItems = Pojos.Items.cnd(lnk.createCondition(obj));
    	                pojo.append(_cndItems);
    	                if (cnd != null) {
    	                   if (cnd instanceof Criteria) {
    	                        Criteria cri = (Criteria) cnd;
    	                        SqlExpressionGroup seg = cri.where();
    	                        if (_cndItems.length > 0 && seg != null && !seg.isEmpty()) {
    	                            seg.setTop(false);
    	                            pojo.append(Pojos.Items.wrap(" AND "));
    	                        }
    	                        pojo.append(cri);
    	                        if (cri.getPager() != null) {
    	                            pojo.setPager(cri.getPager());
    	                            expert.formatQuery(pojo);
    	                        }
    	                    }
    	                    // 普通条件
    	                    else {
    	                        pojo.append(new ConditionPItem(cnd));
    	                    }
    	                 }
    	
    	                pojo.setAfter(lnk.getCallback());
    	                pojo.setEntity(lnk.getLinkedEntity());
    	                 _exec(pojo);
    	
    	                lnk.setValue(obj, pojo.getResult());
    	             }
    	         };
    	     }
}
