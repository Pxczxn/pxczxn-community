package top.pxczxn.platform.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 待移除脚手架能力的过渡开关。
 *
 * <p>所有字段默认关闭，阶段 6 删除对应入口和实现前先建立不可达边界。</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pxczxn.features.scaffold")
public class ScaffoldFeatureProperties {

    private boolean payment;
    private boolean sms;
    private boolean wechat;
    private boolean socialLogin;
    private boolean chat;
    private boolean sshServer;
    private boolean codegen;
    private boolean samples;
}
