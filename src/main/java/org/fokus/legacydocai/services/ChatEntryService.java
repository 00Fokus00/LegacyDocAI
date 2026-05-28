package org.fokus.legacydocai.services;

import org.fokus.legacydocai.model.Chat;
import org.fokus.legacydocai.model.ChatEntry;
import org.fokus.legacydocai.model.Role;
import org.fokus.legacydocai.repository.ChatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatEntryService {

    private final ChatRepository chatRepo;

    public ChatEntryService(ChatRepository chatRepo) {
        this.chatRepo = chatRepo;
    }

    @Transactional
    public void addChatEntry(Long chatId, String content, Role role) {
        Chat chat = chatRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found with id: " + chatId));
        chat.addChatEntry(ChatEntry.builder()
                .content(content)
                .role(role)
                .build());
    }

}
