package org.example.expert.domain.user;

import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@SpringBootTest
@Transactional
public class UserBatchInsertTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Rollback(value = false)
    public void generateAndSaveUsersWithJdbcTemplate() {
        int batchSize = 10000;  // 한번에 저장할 배치 크기
        int totalUsers = 1000000; // 총 생성할 유저 수

        List<Object[]> batchArgs = new ArrayList<>();

        for (int i = 0; i < totalUsers; i++) {
            String nickname = UUID.randomUUID().toString().substring(0, 7); // UUID로 유일한 닉네임 생성
            String email = UUID.randomUUID().toString() + "@example.com";  // UUID로 유일한 이메일 생성
            String password = "password";
            String userRole = "ROLE_USER";

            batchArgs.add(new Object[]{email, nickname, password, userRole});

            // 배치마다 insert 실행
            if (batchArgs.size() == batchSize) {
                jdbcTemplate.batchUpdate("INSERT INTO users (email, nickname, password, user_role) VALUES (?, ?, ?, ?)", batchArgs);
                batchArgs.clear(); // 배치 완료 후 리스트 초기화
            }
        }

        // 남은 유저 저장
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate("INSERT INTO users (email, nickname, password, user_role) VALUES (?, ?, ?, ?)", batchArgs);
        }
    }

}
