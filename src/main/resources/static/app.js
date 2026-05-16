const chatArea = document.getElementById('chatArea');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');

messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

sendBtn.addEventListener('click', sendMessage);

document.querySelectorAll('.quick-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        messageInput.value = btn.dataset.cmd;
        sendMessage();
    });
});

function sendMessage() {
    const text = messageInput.value.trim();
    if (!text) return;

    appendMessage('user', text);
    messageInput.value = '';

    const typing = showTyping();

    fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: text })
    })
    .then(res => res.json())
    .then(data => {
        removeTyping(typing);
        appendMessage('bot', data.reply || '系统出了点问题，请稍后再试。');
    })
    .catch(err => {
        removeTyping(typing);
        appendMessage('bot', '网络出错了，请检查连接。');
    });
}

function appendMessage(role, content) {
    const div = document.createElement('div');
    div.className = 'message ' + role;

    if (role === 'bot') {
        div.innerHTML = `
            <div class="avatar bot-avatar">🤖</div>
            <div class="bubble">${renderMarkdown(content)}</div>
        `;
    } else {
        div.innerHTML = `
            <div class="avatar user-avatar">👤</div>
            <div class="bubble">${escapeHtml(content)}</div>
        `;
    }

    chatArea.appendChild(div);
    chatArea.scrollTop = chatArea.scrollHeight;
}

function showTyping() {
    const div = document.createElement('div');
    div.className = 'message bot typing-msg';
    div.innerHTML = `
        <div class="avatar bot-avatar">🤖</div>
        <div class="bubble typing-indicator">
            <span></span><span></span><span></span>
        </div>
    `;
    chatArea.appendChild(div);
    chatArea.scrollTop = chatArea.scrollHeight;
    return div;
}

function removeTyping(el) {
    if (el && el.parentNode) {
        el.parentNode.removeChild(el);
    }
}

function renderMarkdown(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/### (.+)/g, '<h3>$1</h3>')
        .replace(/## (.+)/g, '<h2>$1</h2>')
        .replace(/- (.+)/g, '• $1')
        .replace(/\n/g, '<br>');
}

function escapeHtml(text) {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

// Settings panel
const settingsPanel = document.getElementById('settingsPanel');
const apiKeyInput = document.getElementById('apiKeyInput');
const dbPathInput = document.getElementById('dbPathInput');
const apiKeyHint = document.getElementById('apiKeyHint');

function toggleSettings() {
    const visible = settingsPanel.classList.toggle('visible');
    if (visible) {
        loadSettings();
    }
}

function loadSettings() {
    fetch('/api/settings')
        .then(res => res.json())
        .then(data => {
            if (data.apiKey) {
                apiKeyInput.value = data.apiKey;
                apiKeyHint.textContent = '当前: ' + data.apiKey;
            } else {
                apiKeyInput.value = '';
                apiKeyHint.textContent = '未设置';
            }
            dbPathInput.value = data.dbPath || '';
        })
        .catch(() => {
            apiKeyHint.textContent = '加载失败';
        });
}

function saveSettings() {
    const body = {};
    const apiKey = apiKeyInput.value.trim();
    const dbPath = dbPathInput.value.trim();


    if (apiKey) body.apiKey = apiKey;
    if (dbPath) body.dbPath = dbPath;

    if (!apiKey && !dbPath) {
        appendMessage('bot', '请至少填写一项设置。');
        return;
    }

    fetch('/api/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(res => res.json())
    .then(data => {
        settingsPanel.classList.remove('visible');
        appendMessage('bot', data.message || '设置已保存');
        if (data.restartRequired) {
            appendMessage('bot', '⚠️ 请手动重启应用，使数据库路径变更生效。');
        }
    })
    .catch(() => {
        appendMessage('bot', '保存设置失败，请检查网络连接。');
    });
}

// Poll for reminders every 30 seconds
setInterval(() => {
    fetch('/api/reminders')
        .then(res => res.json())
        .then(data => {
            if (data.reminders && data.reminders.length > 0) {
                data.reminders.forEach(msg => {
                    appendMessage('bot', msg);
                });
            }
        });
}, 30000);
