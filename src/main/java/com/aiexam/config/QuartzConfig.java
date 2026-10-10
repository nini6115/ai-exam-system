package com.aiexam.config;

import com.aiexam.exam.job.AnswerSheetTimeoutJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 定时任务配置
 * <p>
 * Scheduler 由 spring-boot-starter-quartz 自动装配（内存 JobStore，见 application.yml），
 * Boot 会把容器里的 JobDetail/Trigger Bean 自动注册进调度器。
 */
@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail answerSheetTimeoutJobDetail() {
        return JobBuilder.newJob(AnswerSheetTimeoutJob.class)
                .withIdentity("answerSheetTimeoutJob", "exam")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger answerSheetTimeoutJobTrigger() {
        return TriggerBuilder.newTrigger()
                .forJob(answerSheetTimeoutJobDetail())
                .withIdentity("answerSheetTimeoutTrigger", "exam")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        // 30 秒一轮：与前端草稿保存节奏一致，超时收卷最大延迟 30 秒；
                        // 扫描的是 status=1 且已过期的极小子集，压力可忽略
                        .withIntervalInSeconds(30)
                        .repeatForever()
                        // 错过一轮无所谓，下一轮扫描兜底
                        .withMisfireHandlingInstructionFireNow())
                .build();
    }
}
