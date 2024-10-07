package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.dto.TodoGetCondition;
import org.example.expert.domain.todo.dto.TodoSearchCondition;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TodoRepositoryQuery {

    Page<Todo> findTodosByCondition(TodoGetCondition condition, Pageable pageable);

    Page<TodoSearchResponse> searchTodos(TodoSearchCondition condition, Pageable pageable);

    Optional<Todo> findByIdWithUser(Long todoId);
}
