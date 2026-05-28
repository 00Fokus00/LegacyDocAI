package org.fokus.legacydocai;

import org.fokus.legacydocai.repository.ChatRepository;
import org.fokus.legacydocai.services.PostgresChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LegacyDocAiApplication {

//    private static final PromptTemplate MY_PROMPT_TEMPLATE = new PromptTemplate(
//            "{query}\n\n" +
//                    "Контекст:\n" +
//                    "---------------------\n" +
//                    "{question_answer_context}\n" +
//                    "---------------------\n\n" +
//                    "Отвечай только на основе контекста выше. Если информации нет в контексте, сообщи, что не можешь ответить."
//    );


    private static final PromptTemplate MY_PROMPT_TEMPLATE = new PromptTemplate(
            "Ты — ведущий системный архитектор и технический писатель. Твоя задача — анализировать исходный код legacy-проекта и генерировать точную, понятную и структурированную документацию или комментарии.\n\n" +
                    "Запрос пользователя:\n" +
                    "{query}\n\n" +
                    "Контекст (исходный код и метаданные):\n" +
                    "---------------------\n" +
                    "{question_answer_context}\n" +
                    "---------------------\n\n" +
                    "Правила формирования ответа:\n" +
                    "1. Отвечай строго на основе предоставленного контекста. Не додумывай бизнес-логику, которой нет в коде.\n" +
                    "2. Если для полного ответа на запрос в контексте не хватает данных (например, класс ссылается на метод, тела которого нет в контексте), прямо укажи на это.\n" +
                    "3. Используй профессиональную ИТ-терминологию.\n" +
                    "4. Форматируй ответ с использованием Markdown (выделяй названия методов, классов и переменных `шрифтом кода`, используй списки и заголовки для читаемости).\n" +
                    "5. Если пользователь просит добавить комментарии к коду, возвращай код с комментариями в стандартном для этого языка формате (например, Javadoc для Java)."
    );

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private VectorStore vectorStore;


    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultAdvisors(getHistoryAdvisor(), getRagAdviser()).build();
    }

    private Advisor getRagAdviser() {
        return QuestionAnswerAdvisor.builder(vectorStore).promptTemplate(MY_PROMPT_TEMPLATE).searchRequest(
                SearchRequest.builder().topK(4).build()
        ).build();
    }


    private Advisor getHistoryAdvisor() {
        return MessageChatMemoryAdvisor.builder(getChatMemory()).order(-10).build();
    }

    private ChatMemory getChatMemory() {
        return PostgresChatMemory.builder()
                .maxMessages(12)
                .chatMemoryRepository(chatRepository)
                .build();
    }


    public static void main(String[] args) {
        SpringApplication.run(LegacyDocAiApplication.class, args);
    }

}
