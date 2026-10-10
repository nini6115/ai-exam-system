package com.aiexam.exam.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作弊/异常行为记录实体
 * <p>
 * 注意：cheat_record 表没有 deleted / update_time 字段，故不继承 BaseEntity。
 */
@Data
@TableName("cheat_record")
public class CheatRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 答卷ID */
    private Long sheetId;

    /** 考试ID（冗余） */
    private Long examId;

    /** 考生ID（冗余） */
    private Long userId;

    /** 类型：1切屏 2离开超时 3复制粘贴 4多端登录 5其他 */
    private Integer type;

    /** 描述 */
    private String description;

    /** 详细信息（JSON列，存JSON字符串） */
    private String detail;

    /** 发生时间 */
    private LocalDateTime occurTime;

    /** 是否处理：0未 1已 */
    private Integer handled;

    /** 处理人ID */
    private Long handlerId;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleNote;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
