package top.pxczxn.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * pxczxn 博客社区模块化单体启动入口。
 */
@SpringBootApplication(scanBasePackages = {"top.pxczxn.community", "com.mars"})
@MapperScan({
        "com.mars.system.mapper",
        "com.mars.file.mapper",
        "com.mars.gen.mapper",
        "com.mars.message.mapper",
        "com.mars.sms.mapper",
        "com.mars.job.mapper",
        "top.pxczxn.community.user.persistence",
        "top.pxczxn.community.blog.persistence",
        "top.pxczxn.community.file.persistence",
        "top.pxczxn.community.article.persistence",
        "top.pxczxn.community.taxonomy.persistence",
        "top.pxczxn.community.moderation.persistence",
        "top.pxczxn.community.notification.persistence",
        "top.pxczxn.community.social.persistence"
})
@EnableScheduling
public class PxczxnCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(PxczxnCommunityApplication.class, args);
    }
}
