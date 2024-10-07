package org.example.expert.domain.todo.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoSearchCondition {
    String keyword;
    LocalDate start;
    LocalDate end;
    String nickname;
}
