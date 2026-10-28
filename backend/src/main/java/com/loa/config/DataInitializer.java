package com.loa.config;

import com.loa.entity.ForbiddenWord;
import com.loa.entity.User;
import com.loa.repository.ForbiddenWordRepository;
import com.loa.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ForbiddenWordRepository forbiddenWordRepository;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           ForbiddenWordRepository forbiddenWordRepository,
                           @Value("${loa.admin.username:admin}") String adminUsername,
                           @Value("${loa.admin.password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.forbiddenWordRepository = forbiddenWordRepository;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername(adminUsername).isEmpty()) {
            User u = new User();
            u.setUsername(adminUsername);
            u.setPassword(adminPassword);
            u.setNickname("管理员");
            u.setRole("ADMIN");
            userRepository.save(u);
        }
        if (forbiddenWordRepository.count() == 0) {
            forbiddenWordRepository.saveAll(List.of(
                    new ForbiddenWord("最便宜", "极限词", 2),
                    new ForbiddenWord("第一", "极限词", 2),
                    new ForbiddenWord("绝对", "极限词", 2),
                    new ForbiddenWord("国家级", "资质词", 2),
                    new ForbiddenWord("包治百病", "功效词", 2)
            ));
        }
    }
}
