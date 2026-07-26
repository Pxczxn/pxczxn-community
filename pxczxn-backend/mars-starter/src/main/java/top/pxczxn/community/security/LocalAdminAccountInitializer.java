package top.pxczxn.community.security;

import cn.hutool.crypto.digest.BCrypt;
import com.mars.system.entity.SysRole;
import com.mars.system.entity.SysUser;
import com.mars.system.entity.SysUserRole;
import com.mars.system.service.SysRoleService;
import com.mars.system.service.SysUserRoleService;
import com.mars.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Provides the accepted local-only development administrator.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAdminAccountInitializer implements ApplicationRunner {

    private final SysUserService userService;
    private final SysRoleService roleService;
    private final SysUserRoleService userRoleService;

    @Value("${pxczxn.local-admin.username}")
    private String username;

    @Value("${pxczxn.local-admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            user = new SysUser();
            user.setUsername(username);
            user.setNickname("本地管理员");
            user.setUserType("admin");
            user.setStatus(1);
            user.setGender(0);
            user.setIsQuit(0);
            user.setDeleted(0);
            user.setCreateTime(LocalDateTime.now());
        }

        user.setPassword(BCrypt.hashpw(password));
        user.setMustChangePassword(0);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setTemporaryPasswordIssuedAt(null);
        user.setUpdateTime(LocalDateTime.now());
        userService.saveOrUpdate(user);

        SysRole adminRole = roleService.getByCode("admin");
        if (adminRole != null) {
            boolean alreadyAssigned = userRoleService.lambdaQuery()
                    .eq(SysUserRole::getUserId, user.getId())
                    .eq(SysUserRole::getRoleId, adminRole.getId())
                    .exists();
            if (!alreadyAssigned) {
                SysUserRole assignment = new SysUserRole();
                assignment.setUserId(user.getId());
                assignment.setRoleId(adminRole.getId());
                userRoleService.save(assignment);
            }
        }

        log.info("Local administrator account is ready: username={}", username);
    }
}
