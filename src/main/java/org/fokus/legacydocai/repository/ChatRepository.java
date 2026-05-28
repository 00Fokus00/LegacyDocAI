package org.fokus.legacydocai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.fokus.legacydocai.model.Chat;

public interface ChatRepository extends JpaRepository<Chat, Long> {

}
