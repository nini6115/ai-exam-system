package com.aiexam.exam.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 防作弊上报结果VO
 */
@Data
public class CheatReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 累计切屏次数（离开超时上报时返回当前值） */
    private Integer switchCount;

    /** 最大切屏次数（0不限） */
    private Integer maxScreenSwitch;

    /** 是否已超限 */
    private Boolean exceeded;

    /** 服务端本次动作：0仅记录 1前端警告（已超限但配置为仅警告） 2已强制交卷 */
    private Integer action;
}
