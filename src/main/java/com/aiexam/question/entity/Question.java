package com.aiexam.question.entity;

import com.aiexam.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 题目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "question", autoResultMap = true)
public class Question extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 题目ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题型：1单选 2多选 3判断 4填空 5简答 */
    private Integer type;

    /** 难度：1简单 2中等 3困难 */
    private Integer difficulty;

    /** 知识点/分类 */
    private String category;

    /** 题干 */
    private String title;

    /** 选项（JSON数组，选择题用） */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    /** 参考答案：单选"A"，多选"ABC"（字母升序），判断"对"/"错"，填空/简答存文本 */
    private String answer;

    /** 答案解析 */
    private String analysis;

    /** 默认分值 */
    private BigDecimal score;

    /** 创建人ID */
    private Long creatorId;
}
