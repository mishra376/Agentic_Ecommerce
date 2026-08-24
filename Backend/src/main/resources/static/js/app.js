document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const chatForm = document.getElementById('chatForm');
    const userInput = document.getElementById('userInput');
    const messagesFeed = document.getElementById('messagesFeed');
    const welcomeScreen = document.getElementById('welcomeScreen');
    const typingIndicator = document.getElementById('typingIndicator');
    const messagesContainer = document.getElementById('messagesContainer');
    const clearChatBtn = document.getElementById('clearChatBtn');
    const newChatBtn = document.getElementById('newChatBtn');
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');
    const suggestionCards = document.querySelectorAll('.suggestion-card');
    const chatHistory = document.getElementById('chatHistory');

    // State Variables
    let messagesList = [];
    let chatSessions = [
        { id: 1, title: 'E-Commerce Guide', active: true }
    ];

    // Initialize Lucide Icons
    lucide.createIcons();

    // Toggle Mobile Sidebar
    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('open');
        });
    }

    // Close Sidebar on window resize if open
    window.addEventListener('resize', () => {
        if (window.innerWidth > 768) {
            sidebar.classList.remove('open');
        }
    });

    // Form Submit Event Handler
    chatForm.addEventListener('submit', (e) => {
        e.preventDefault();
        const text = userInput.value.trim();
        if (!text) return;
        
        handleUserMessage(text);
    });

    // Suggestion Cards Event Listeners
    suggestionCards.forEach(card => {
        card.addEventListener('click', () => {
            const prompt = card.getAttribute('data-prompt');
            if (prompt) {
                handleUserMessage(prompt);
            }
        });
    });

    // Clear Chat Button Handler
    clearChatBtn.addEventListener('click', () => {
        clearChatFeed();
    });

    // New Chat Button Handler
    newChatBtn.addEventListener('click', () => {
        clearChatFeed();
        // Reset active session state
        document.querySelectorAll('.history-item').forEach(item => item.classList.remove('active'));
        
        // Add a new session item to sidebar
        const sessionId = Date.now();
        const newSession = { id: sessionId, title: 'New Conversation', active: true };
        chatSessions.push(newSession);
        
        renderHistorySidebar();
        sidebar.classList.remove('open');
    });

    // Send User Message
    function handleUserMessage(text) {
        // Append user message
        addMessageToFeed('user', text);
        userInput.value = '';
        
        // Show Typing Indicator
        showTyping(true);
        scrollToBottom();

        // If it's a new conversation, update history title based on the first message
        const activeSession = chatSessions.find(s => s.active);
        if (activeSession && activeSession.title === 'New Conversation') {
            activeSession.title = text.length > 25 ? text.substring(0, 22) + '...' : text;
            renderHistorySidebar();
        }

        // Call backend API
        fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: text })
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Server returned status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            showTyping(false);
            if (data && data.reply) {
                addMessageToFeed('assistant', data.reply);
            } else {
                addMessageToFeed('assistant', "Received an invalid empty response from the server.");
            }
        })
        .catch(error => {
            showTyping(false);
            console.error('API Error:', error);
            
            // Helpful error response
            const errorHtml = `### ⚠️ Connection Failed\n\n` +
                              `I was unable to reach the Spring Boot backend server at \`/api/chat\`.\n\n` +
                              `**Please verify that:**\n` +
                              `- The Java backend is compiled and running successfully.\n` +
                              `- The server is bound to port \`8080\`.\n` +
                              `- Database services (PostgreSQL / MongoDB) are active (if database schemas are being compiled).\n\n` +
                              `*Error details: ${error.message}*`;
            addMessageToFeed('assistant', errorHtml);
        });
    }

    // Add Message to DOM Feed
    function addMessageToFeed(sender, text) {
        // Hide welcome screen on first message
        if (welcomeScreen.style.display !== 'none') {
            welcomeScreen.style.display = 'none';
        }

        messagesList.push({ sender, text });

        const messageWrapper = document.createElement('div');
        messageWrapper.classList.add('message-wrapper', sender);

        const avatar = document.createElement('div');
        avatar.classList.add('message-avatar');
        avatar.innerHTML = sender === 'user' ? '<i data-lucide="user"></i>' : '<i data-lucide="sparkles"></i>';

        const bubble = document.createElement('div');
        bubble.classList.add('message-bubble');
        bubble.innerHTML = formatResponseText(text);

        messageWrapper.appendChild(avatar);
        messageWrapper.appendChild(bubble);
        messagesFeed.appendChild(messageWrapper);

        // Render Lucide icons in the message bubble
        lucide.createIcons();
        scrollToBottom();
    }

    // Show/Hide Typing Indicator
    function showTyping(show) {
        typingIndicator.style.display = show ? 'flex' : 'none';
    }

    // Scroll to Bottom of Messages Container
    function scrollToBottom() {
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    // Clear Chat
    function clearChatFeed() {
        messagesFeed.innerHTML = '';
        messagesList = [];
        welcomeScreen.style.display = 'block';
        showTyping(false);
    }

    // Render Side Panel Conversations
    function renderHistorySidebar() {
        chatHistory.innerHTML = '';
        chatSessions.forEach(session => {
            const item = document.createElement('div');
            item.classList.add('history-item');
            if (session.active) item.classList.add('active');
            
            item.innerHTML = `
                <i data-lucide="message-square"></i>
                <span class="history-title">${session.title}</span>
            `;

            item.addEventListener('click', () => {
                if (session.active) {
                    sidebar.classList.remove('open');
                    return;
                }
                chatSessions.forEach(s => s.active = false);
                session.active = true;
                renderHistorySidebar();
                
                // For demo purposes, we clear feed or restore empty feed for new active conversation
                clearChatFeed();
                if (session.title !== 'New Conversation' && session.title !== 'E-Commerce Guide') {
                    addMessageToFeed('assistant', `Welcome back to **${session.title}** session! Ready to query more backend REST APIs.`);
                }
                sidebar.classList.remove('open');
            });

            chatHistory.appendChild(item);
        });
        
        lucide.createIcons();
    }

    // Parse and Format Chat Response Markdown
    function formatResponseText(text) {
        // Escaping HTML characters first to avoid XSS
        let html = text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');

        // Block Code Blocks: ```code```
        html = html.replace(/```(?:[a-zA-Z0-9]+)?\n([\s\S]*?)\n```/g, '<pre><code>$1</code></pre>');

        // Inline Code Blocks: `code`
        html = html.replace(/`([^`\n]+)`/g, '<code>$1</code>');

        // Titles: ### text
        html = html.replace(/^###\s*(.*?)$/gm, '<h3>$1</h3>');

        // Bold text: **text**
        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

        // Lists formatting: - item
        html = html.replace(/^\s*-\s+(.*?)$/gm, '<li>$1</li>');
        
        // Wrap contiguous <li> tags with <ul>
        // Match lists spanning across lines
        html = html.replace(/(<li>.*?<\/li>\s*)+/gs, (match) => `<ul>${match}</ul>`);

        // Convert double returns into paragraphs or singular returns into breaks
        html = html.replace(/\n/g, '<br>');

        // Clean up redundant tags that get double-spaced
        html = html.replace(/<\/h3><br>/g, '</h3>');
        html = html.replace(/<\/pre><br>/g, '</pre>');
        html = html.replace(/<\/ul><br>/g, '</ul>');
        html = html.replace(/<br><ul>/g, '<ul>');
        html = html.replace(/<ul><br>/g, '<ul>');
        html = html.replace(/<\/li><br>/g, '</li>');

        return html;
    }
});
