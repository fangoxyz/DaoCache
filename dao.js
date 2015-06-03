var ioc = {
		 

		cache : {

			type : "com.youfang.dao.cache.CacheExecExterior",

			fields : {

						cacheController : [{refer : 'cacheController'}],

						clazzList : [
			                            ["com.youfang.biz.model.City",100]
										,["com.youfang.biz.model.XiaoQu",10000]
										,["com.youfang.biz.model.QuXian",1000]
										,["com.youfang.biz.model.UserGroup",100]
										,["com.youfang.biz.model.Zone",5000] 
										,["com.youfang.biz.model.Permission",5000]
										,["com.youfang.biz.model.KeyWordsPool",100]
										,["com.youfang.biz.model.BlackList",1000]
										,["com.youfang.biz.model.FriendlyLink",100]
										,["com.youfang.biz.model.Fav",10000]
										,["com.youfang.biz.model.Saler",100000]
										,["com.youfang.biz.model.User",10000]
										,["com.youfang.biz.model.FangchanInfo",10000]
										,["com.youfang.biz.model.FangKnowledge",10000]
										,["com.youfang.biz.model.ZhiShiComment",1000]
										,["com.youfang.biz.model.ZhiShiElement",1000]
										,["com.youfang.biz.model.NewsComment",1000]
										,["com.youfang.biz.model.NewsElement",1000]
										,["com.youfang.biz.model.KnowledgeClass",1000] 
			                                     ],
						//这里配置视图的关联关系，视图从哪几个基本表中取得数据就对应地写几个关联关系
						cacheLinks : {
		                            "t_houseinfo":["v_houseinfo"],
		                            "t_xiaoqu":["v_houseinfo"],
		                            "t_houseinfo_other":["v_houseinfo"] 
                         			},
						isEnableCache : true

			} ,
			events : {
				create : "init"
			} 

		},

		cacheController : {

			type : "com.youfang.dao.cache.MemeoryCacheImpl",
			
		},

		daoExecutorImpl : {

			type : 

				"org.nutz.dao.DaoExecutorImpl",

			fields : {

				execMethod : {refer : 'cache'} 

			},
			events : {
				depose : "shutDown"
			} 
			

		},
		 
	 
	dataSource : {
		type : "com.alibaba.druid.pool.DruidDataSource",
		
		events : {
			depose : "close"
		},
		fields : { 
			driverClassName : "com.mysql.jdbc.Driver",
			url : "jdbc:mysql://192.168.0.253:3306/youfang?useUnicode=true&characterEncoding=utf-8",
			username : "root",
			password : ""
			 
		}
	},
	
	dao : {
		type : "org.nutz.dao.impl.NutDaoExt",
		fields : {
			dataSource : {
				refer : 'dataSource'
			},
			executor : {
				refer : 'daoExecutorImpl'
			}
		}
	}
};