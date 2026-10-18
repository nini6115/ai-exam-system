-- 移除数据字典与操作日志模块：功能代码已删，同步删除对应表
DROP TABLE IF EXISTS `sys_dict_data`;
DROP TABLE IF EXISTS `sys_dict_type`;
DROP TABLE IF EXISTS `sys_oper_log`;
