package com.aiexam.exam.mapper;

import com.aiexam.exam.entity.AnswerSheet;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 答卷 Mapper（单表 CRUD 全部由 MyBatis-Plus 提供）
 */
@Mapper
public interface AnswerSheetMapper extends BaseMapper<AnswerSheet> {

    /**
     * 条件抢占收卷：只有答题中(1)的答卷能被打上终态（SQL 在 XML 中）
     * <p>
     * 事务内执行会持有行锁到提交，手动/切屏强制/超时自动三方并发时
     * 只有一个入口能改成功，其余入口 affected=0 直接放弃，保证判分落库恰好一次。
     *
     * @param id         答卷ID
     * @param status     终态：2已交卷 3强制交卷 4超时自动交卷
     * @param submitType 交卷方式：1手动 2超时 3切屏超限
     * @param now        交卷时间
     * @return 影响行数：0=已被其他入口收卷
     */
    int markSubmitted(@Param("id") Long id, @Param("status") int status,
                      @Param("submitType") int submitType, @Param("now") LocalDateTime now);

    /**
     * 原子累加切屏次数（守卫答题中状态，SQL 在 XML 中）
     *
     * @param id 答卷ID
     * @return 影响行数：0=答卷已不在答题中状态
     */
    int increaseScreenSwitch(@Param("id") Long id);
}
