package com.aiexam.analysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 成绩导出结果VO（Controller 据此返回 CSV 文件流）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreExportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 下载文件名（含扩展名） */
    private String fileName;

    /** CSV 文件内容（UTF-8 编码，带 BOM） */
    private byte[] content;
}
