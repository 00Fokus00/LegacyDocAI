document.addEventListener("DOMContentLoaded", function () {

    const sendButton = document.getElementById("send-button");
    const chatInput = document.getElementById("chat-input");
    const messagesContainer = document.getElementById("messages");

    function scrollToBottom() {
        messagesContainer.scrollTo({
            top: messagesContainer.scrollHeight,
            behavior: "smooth"
        });
    }

    function sendMessage() {

        const prompt = chatInput.value.trim();

        if (!prompt) {
            return;
        }

        chatInput.value = "";

        // USER MESSAGE

        const userDiv = document.createElement("div");
        userDiv.className = "message user";

        userDiv.innerHTML = `
            <img src="/images/user.png" alt="User">
            <div class="bubble">${prompt}</div>
        `;

        messagesContainer.appendChild(userDiv);

        scrollToBottom();

        // CHAT ID

        const pathParts = window.location.pathname.split("/");
        const chatId = pathParts[pathParts.length - 1];

        const url =
            `/chat-stream/${chatId}?userPrompt=${encodeURIComponent(prompt)}`;

        // AI MESSAGE

        const aiDiv = document.createElement("div");
        aiDiv.className = "message assistant";

        aiDiv.innerHTML = `
            <img src="/images/ai.png" alt="AI">
        `;

        const aiBubble = document.createElement("div");
        aiBubble.className = "bubble";

        // LOADER

        aiBubble.innerHTML = `
            <div class="loader">
                <span></span>
                <span></span>
                <span></span>
            </div>
        `;

        aiDiv.appendChild(aiBubble);

        messagesContainer.appendChild(aiDiv);

        scrollToBottom();

        const eventSource = new EventSource(url);

        let fullText = "";
        let started = false;

        eventSource.onmessage = function (event) {

            const data = JSON.parse(event.data);

            let token = data.text;

            fullText += token;

            if (!started) {
                aiBubble.innerHTML = "";
                started = true;
            }

            aiBubble.innerHTML = marked.parse(fullText);

            scrollToBottom();
        };

        eventSource.onerror = function (e) {

            console.error("SSE error:", e);

            eventSource.close();

            if (!fullText) {
                aiBubble.innerHTML =
                    "<p>Ошибка генерации ответа.</p>";
            }
        };
    }

    // BUTTON CLICK

    if (sendButton) {

        sendButton.addEventListener("click", function () {
            sendMessage();
        });
    }

    // ENTER SEND

    if (chatInput) {

        chatInput.addEventListener("keydown", function (event) {

            if (event.key === "Enter" && !event.shiftKey) {

                event.preventDefault();

                sendMessage();
            }
        });
    }

    // PROJECT MODAL

    const projectModal =
        document.getElementById("project-modal");

    const openProjectModal =
        document.getElementById("open-project-modal");

    const closeProjectModal =
        document.getElementById("close-project-modal");

    if (openProjectModal) {

        openProjectModal.addEventListener("click", () => {

            projectModal.classList.remove("hidden");
        });
    }

    if (closeProjectModal) {

        closeProjectModal.addEventListener("click", () => {

            projectModal.classList.add("hidden");
        });
    }

    document.querySelectorAll(".message.assistant .bubble.markdown-content").forEach(function (bubble) {
        const rawMarkdown = bubble.textContent;
        bubble.innerHTML = marked.parse(rawMarkdown);
    });

});