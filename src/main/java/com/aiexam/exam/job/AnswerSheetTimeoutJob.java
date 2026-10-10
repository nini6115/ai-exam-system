package com.aiexam.exam.job;

import com.aiexam.exam.service.AnswerSheetService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 超时答卷自动收卷任务：扫描答题中且已过应交卷时间的答卷，逐张强制收卷
 * <p>
 * 注意：不加 @Component——Quartz 通过 SpringBeanJobFactory 用容器 createBean 实例化 Job，
 * @Autowired 字段可正常注入；标注 @Component 反而会被当作普通 Bean 多实例化一次。
 * 业务逻辑全部在 AnswerSheetService，本类只做调度入口。
 */
@Slf4j
@DisallowConcurrentExecution
public class AnswerSheetTimeoutJob implements Job {

    @Autowired
    private AnswerSheetService answerSheetService;

    @Override
    public void execute(JobExecutionContext context) {
        int done = answerSheetService.autoSubmitTimeoutSheets();
        // 静默轮不打日志，防止每 30 秒刷屏
        if (done > 0) {
            log.info("超时自动交卷完成，本轮收卷 {} 份", done);
        }
    }
}
