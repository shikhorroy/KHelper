// Main page JavaScript for handling pending requests

// Theme toggle functionality
function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);

    // Update icon
    const icon = document.getElementById('theme-icon');
    if (icon) {
        icon.textContent = newTheme === 'dark' ? '☀️' : '🌙';
    }
}

// Initialize theme on page load
(function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);

    const icon = document.getElementById('theme-icon');
    if (icon) {
        icon.textContent = savedTheme === 'dark' ? '☀️' : '🌙';
    }
})();

let autoRefreshInterval;

function showToast(message, type) {
    // Remove any existing toast
    const existingToast = document.querySelector('.toast');
    if (existingToast) {
        existingToast.remove();
    }

    // Create new toast
    const toast = document.createElement('div');
    toast.className = 'toast ' + type;

    const icon = document.createElement('span');
    icon.className = 'toast-icon';
    icon.textContent = type === 'success' ? '✓' : '✗';

    const text = document.createElement('span');
    text.textContent = message;

    toast.appendChild(icon);
    toast.appendChild(text);
    document.body.appendChild(toast);

    // Auto-remove after 5 seconds with slide out animation
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-in';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, 5000);
}

function handleRequest(id, accept) {
    const action = accept ? 'accept' : 'reject';
    const message = accept ? 'Problem Accepted!' : 'Problem Rejected!';
    const type = accept ? 'success' : 'error';

    fetch('/' + action + '/' + id, { method: 'POST' })
        .then(response => {
            if (response.ok) {
                showToast(message, type);
                // Reload after showing toast (2 seconds to see the notification)
                setTimeout(() => loadPendingRequests(), 2000);
            } else {
                showToast('Error: Could not process request', 'error');
            }
        })
        .catch(err => {
            console.error(err);
            showToast('Network error occurred', 'error');
        });
}

function renderPendingRequest(request) {
    const div = document.createElement('div');
    div.className = 'pending-request';

    let html = `
        <h2>📝 ${escapeHtml(request.problem.name || 'Unknown Problem')}</h2>
        
        <div class="problem-info">
            <strong>Group:</strong> ${escapeHtml(request.problem.group || 'Unknown')}
        </div>
        <div class="problem-info">
            <strong>URL:</strong> 
            <a href="${escapeHtml(request.problem.url || '#')}" target="_blank">${escapeHtml(request.problem.url || 'Unknown')}</a>
        </div>
        <div class="problem-info">
            <strong>Time Limit:</strong> ${request.problem.timeLimit || -1} ms
        </div>
        <div class="problem-info">
            <strong>Memory Limit:</strong> ${request.problem.memoryLimit || -1} MB
        </div>
        <div class="problem-info">
            <strong>Test Cases:</strong> ${request.problem.tests.length}
        </div>
    `;

    if (request.problem.tests && request.problem.tests.length > 0) {
        html += `<div class="tests-section"><h3>Test Cases (${request.problem.tests.length}):</h3>`;

        request.problem.tests.forEach((test, idx) => {
            html += `
                <div class="test-case">
                    <h4>Test ${idx + 1}</h4>
                    <div class="test-content">
                        <div class="test-section">
                            <div class="test-section-title">Input:</div>
                            <div class="test-data">${escapeHtml(test.input)}</div>
                        </div>
                        <div class="test-section">
                            <div class="test-section-title">Output:</div>
                            <div class="test-data">${escapeHtml(test.output)}</div>
                        </div>
                    </div>
                </div>
            `;
        });

        html += '</div>';
    }

    html += `
        <div class="buttons">
            <button class="accept" onclick="handleRequest('${request.id}', true)">✓ Accept</button>
            <button class="reject" onclick="handleRequest('${request.id}', false)">✗ Reject</button>
        </div>
    `;

    div.innerHTML = html;
    return div;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadPendingRequests() {
    try {
        const response = await fetch('/api/pending-requests');
        const data = await response.json();

        const container = document.getElementById('requests-container');
        container.innerHTML = '';

        document.getElementById('pending-count').textContent = `Pending: ${data.requests.length}`;

        if (data.requests.length === 0) {
            const noRequests = document.createElement('div');
            noRequests.className = 'no-requests';
            noRequests.textContent = 'No pending requests. Waiting for Competitive Companion...';
            container.appendChild(noRequests);
        } else {
            data.requests.forEach(request => {
                container.appendChild(renderPendingRequest(request));
            });
        }
    } catch (error) {
        console.error('Error loading pending requests:', error);
    }
}

// Load pending requests on page load
document.addEventListener('DOMContentLoaded', () => {
    loadPendingRequests();

    // Auto-refresh every 2 seconds
    autoRefreshInterval = setInterval(loadPendingRequests, 2000);
});

// Clean up interval on page unload
window.addEventListener('beforeunload', () => {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval);
    }
});

