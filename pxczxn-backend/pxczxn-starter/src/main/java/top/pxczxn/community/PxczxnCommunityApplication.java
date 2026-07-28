package top.pxczxn.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.mybatis.spring.annotation.MapperScan;

/**
 * pxczxn 博客社区模块化单体启动入口。
 */
@SpringBootApplication(scanBasePackages = {"top.pxczxn.community", "top.pxczxn.platform"})
@MapperScan({
        "top.pxczxn.platform.system.mapper",
        "top.pxczxn.platform.file.mapper",
        "top.pxczxn.platform.gen.mapper",
        "top.pxczxn.platform.message.mapper",
        "top.pxczxn.platform.sms.mapper",
        "top.pxczxn.platform.job.mapper",
        "top.pxczxn.community.user.persistence",
        "top.pxczxn.community.blog.persistence",
        "top.pxczxn.community.file.persistence",
        "top.pxczxn.community.article.persistence",
        "top.pxczxn.community.taxonomy.persistence",
        "top.pxczxn.community.moderation.persistence",
        "top.pxczxn.community.notification.persistence",
        "top.pxczxn.community.social.persistence",
        "top.pxczxn.community.team.persistence",
        "top.pxczxn.community.team.submission.persistence",
        "top.pxczxn.community.collaboration.persistence",
        "top.pxczxn.community.chat.persistence",
        "top.pxczxn.community.series.persistence",
        "top.pxczxn.community.report.persistence",
        "top.pxczxn.community.block.persistence",
        "top.pxczxn.community.appeal.persistence"
        ,"top.pxczxn.community.sanction.persistence"
        ,"top.pxczxn.community.abuse.persistence"
        ,"top.pxczxn.community.search.persistence"
        ,"top.pxczxn.community.editorial.persistence"
})
@EnableScheduling
public class PxczxnCommunityApplication {

    public static void main(String[] args) {
        SpringApplication.run(PxczxnCommunityApplication.class, args);
    }
}
