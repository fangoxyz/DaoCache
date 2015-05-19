2015-5-19
1.根据兽解决的缓存保存Null值的问题的原理，做了一些修改.
2.增加1级,如t_user的一级缓存为t_user_L1___
3.增加了缓存之间的关联关系，用于多表查询以及视图查询，多表查询时缓存名使用##连接，如t_user##t_usergroup，当清除t_user表的缓存时会清除缓存名为t_user##t_usergroup的缓存.
4.多表查询以及视图查询相当于L3
2015-5-19前
1.ehcache.xml是配置ehcache文件，放在classes目录
2.dao.js文件通过js文式配置daocache需要缓存的表，以及配置DaoExecutor以及NutDaoExt