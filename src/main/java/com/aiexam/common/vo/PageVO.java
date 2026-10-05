package com.aiexam.common.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页通用VO
 */
@Data
public class PageVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    private List<T> list;

    /** 总条数 */
    private long total;

    /** 页码 */
    private long pageNum;

    /** 每页条数 */
    private long pageSize;

    public static <T> PageVO<T> of(IPage<?> page, List<T> list) {
        PageVO<T> vo = new PageVO<>();
        vo.setList(list);
        vo.setTotal(page.getTotal());
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        return vo;
    }
}
