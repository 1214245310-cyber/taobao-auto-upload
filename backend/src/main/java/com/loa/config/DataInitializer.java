package com.loa.config;

import com.loa.entity.ForbiddenWord;
import com.loa.entity.User;
import com.loa.repository.ForbiddenWordRepository;
import com.loa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ForbiddenWordRepository forbiddenWordRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           ForbiddenWordRepository forbiddenWordRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${loa.admin.username:admin}") String adminUsername,
                           @Value("${loa.admin.password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.forbiddenWordRepository = forbiddenWordRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User u = new User();
            u.setUsername(adminUsername);
            u.setPassword(passwordEncoder.encode(adminPassword));
            u.setNickname("管理员");
            u.setRole("ADMIN");
            u.setStatus("0");
            userRepository.save(u);
        }
        if (forbiddenWordRepository.count() == 0) {
            List<ForbiddenWord> seeds = List.of(
                    new ForbiddenWord("最便宜", "极限词", 2),
                    new ForbiddenWord("第一", "极限词", 2),
                    new ForbiddenWord("绝对", "极限词", 2),
                    new ForbiddenWord("国家级", "资质词", 2),
                    new ForbiddenWord("包治百病", "功效词", 2)
            );
            // 种子词无真实操作人，统一标记为 system
            seeds.forEach(w -> w.setCreatedBy("system"));
            forbiddenWordRepository.saveAll(seeds);
        }
    }
}
