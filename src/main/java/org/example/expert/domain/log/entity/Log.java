package org.example.expert.domain.log.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.expert.domain.common.entity.Timestamped;

@Getter
@Entity
@NoArgsConstructor
public class Log extends Timestamped {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long requestUserId;
    private long managerUserId;
    private long todoId;

    public Log(long requestUserId, long managerUserId, long todoId) {
        this.requestUserId = requestUserId;
        this.managerUserId = managerUserId;
        this.todoId = todoId;
    }
}
