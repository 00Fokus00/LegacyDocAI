package org.fokus.legacydocai.services;

import lombok.SneakyThrows;
import org.fokus.legacydocai.model.Chat;
import org.fokus.legacydocai.model.Role;
import org.fokus.legacydocai.repository.ChatRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.fokus.legacydocai.model.Role.ASSISTANT;
import static org.fokus.legacydocai.model.Role.USER;


@Service
public class ChatService {
    private final ChatRepository chatRepo;

    private final ChatClient chatClient;

    private final ChatEntryService chatEntryService;

    public ChatService(ChatRepository chatRepo, ChatClient chatClient, ChatEntryService chatEntryService) {
        this.chatRepo = chatRepo;
        this.chatClient = chatClient;
        this.chatEntryService = chatEntryService;
    }

    public List<Chat> getAllChats() {
        return chatRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public Chat createNewChat(String title) {
        Chat chat = Chat.builder().title(title).build();
        chatRepo.save(chat);
        return chat;
    }

    public Chat getChat(Long chatId) {
        return chatRepo.findById(chatId).orElseThrow();
    }

    public void deleteChat(Long chatId) {
        chatRepo.deleteById(chatId);
    }

    public void addChatEntry(Long chatId, String prompt, Role role) {
        chatEntryService.addChatEntry(chatId, prompt, role);
    }

    @Transactional
    public void proceedInteraction(Long chatId, String prompt) {
        chatEntryService.addChatEntry(chatId, prompt, USER);
        String answer = chatClient.prompt().user(prompt).call().content();
        chatEntryService.addChatEntry(chatId, answer, ASSISTANT);
    }

    public SseEmitter proceedInteractionWithStreaming(Long chatId, String userPrompt) {

        SseEmitter sseEmitter = new SseEmitter(0L);
        final StringBuilder answer = new StringBuilder();

        chatClient
                .prompt(userPrompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .chatResponse()
                .subscribe(
                        (ChatResponse response) -> processToken(response, sseEmitter, answer),
                        sseEmitter::completeWithError,
                        sseEmitter::complete);
        return sseEmitter;
    }



    @SneakyThrows
    private static void processToken(ChatResponse response, SseEmitter emitter, StringBuilder answer) {
        var token = response.getResult().getOutput();
        emitter.send(token);
        answer.append(token.getText());
    }
}

