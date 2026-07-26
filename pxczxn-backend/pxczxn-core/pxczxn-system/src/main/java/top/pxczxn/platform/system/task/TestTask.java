package top.pxczxn.platform.system.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 星语社区示例定时任务
 *
 * @className: TestTask
 * @author pxczxn
 * @date: 2026/3/3 20:55
 */
@Slf4j
@Component
public class TestTask {

    /**
     * 测试任务执行
     */
    public void test() {
        log.info("测试定时任务");
    }
}
