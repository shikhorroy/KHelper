// Main page JavaScript for handling pending requests

// Theme toggle functionality with full-screen animation
function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    const themeButton = document.querySelector('.theme-toggle');

    if (!themeButton) return;

    // Create or get overlay
    let overlay = document.getElementById('theme-transition-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'theme-transition-overlay';
        overlay.className = 'theme-transition-overlay';
        document.body.appendChild(overlay);
    }

    // Clear any existing content
    overlay.innerHTML = '';
    overlay.classList.add('active');

    // Create expanding/contracting wave circle
    const wave = document.createElement('div');
    wave.className = 'theme-orb';
    wave.style.cssText = `
        top: 50%;
        left: 50%;
        background: transparent;
    `;

    // Create icon element (separate from wave)
    const icon = document.createElement('div');
    icon.style.cssText = `
        position: absolute;
        top: 50%;
        left: 50%;
        font-size: 64px;
        z-index: 10001;
    `;

    if (newTheme === 'light') {
        // Switching to light theme: Light expands from center, sun appears then vanishes
        wave.style.background = 'radial-gradient(circle, rgba(255, 255, 255, 0.95) 0%, rgba(255, 255, 255, 0.8) 40%, rgba(255, 255, 255, 0.5) 70%, transparent 100%)';
        icon.textContent = '☀️';

        overlay.appendChild(wave);
        overlay.appendChild(icon);

        // Start animations
        requestAnimationFrame(() => {
            wave.classList.add('expanding-light');
            icon.classList.add('sun-appear-vanish');
        });

        // Apply theme change during animation
        setTimeout(() => {
            html.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        }, 300);

        // Update button icon
        setTimeout(() => {
            const buttonIcon = document.getElementById('theme-icon');
            if (buttonIcon) {
                buttonIcon.textContent = '🌙';
                buttonIcon.classList.add('icon-fade-in');
                setTimeout(() => buttonIcon.classList.remove('icon-fade-in'), 300);
            }
        }, 500);

        // Clean up
        setTimeout(() => {
            overlay.classList.remove('active');
            overlay.innerHTML = '';
        }, 1200);

    } else {
        // Switching to dark theme: Dark contracts from edges, moon appears then vanishes
        wave.style.cssText = `
            top: 50%;
            left: 50%;
            width: 300vmax;
            height: 300vmax;
            background: radial-gradient(circle at center, transparent 0%, rgba(13, 13, 13, 0.7) 30%, rgba(0, 0, 0, 0.85) 60%, rgba(0, 0, 0, 0.95) 100%);
        `;
        icon.textContent = '🌙';

        overlay.appendChild(wave);
        overlay.appendChild(icon);

        // Hide button icon
        const buttonIcon = document.getElementById('theme-icon');
        if (buttonIcon) {
            buttonIcon.classList.add('icon-fade-out');
        }

        // Start animations
        requestAnimationFrame(() => {
            wave.classList.add('contracting-dark');
            icon.classList.add('moon-appear-vanish');
        });

        // Apply theme change during animation
        setTimeout(() => {
            html.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
        }, 300);

        // Update button icon
        setTimeout(() => {
            if (buttonIcon) {
                buttonIcon.classList.remove('icon-fade-out');
                buttonIcon.textContent = '☀️';
                buttonIcon.classList.add('icon-fade-in');
                setTimeout(() => buttonIcon.classList.remove('icon-fade-in'), 300);
            }
        }, 500);

        // Clean up
        setTimeout(() => {
            overlay.classList.remove('active');
            overlay.innerHTML = '';
        }, 1200);
    }
}

// Animation toggle functionality
function toggleAnimations() {
    const body = document.body;
    const isDisabled = body.classList.contains('animations-disabled');

    if (isDisabled) {
        body.classList.remove('animations-disabled');
        localStorage.setItem('animations', 'enabled');
        showToast('Animations Enabled', 'success');
    } else {
        body.classList.add('animations-disabled');
        localStorage.setItem('animations', 'disabled');
        showToast('Animations Disabled', 'success');
    }
}

// Shortcuts menu toggle functionality
function toggleShortcutsMenu() {
    const menu = document.getElementById('shortcuts-menu');
    if (menu.style.display === 'none' || !menu.style.display) {
        menu.style.display = 'block';
    } else {
        menu.style.display = 'none';
    }
}

// Close shortcuts menu when clicking outside
document.addEventListener('click', function(event) {
    const menu = document.getElementById('shortcuts-menu');
    const toggleButton = document.querySelector('.shortcuts-toggle');

    if (menu && toggleButton && menu.style.display === 'block') {
        if (!menu.contains(event.target) && !toggleButton.contains(event.target)) {
            menu.style.display = 'none';
        }
    }
});

// Initialize theme on page load
(function initTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);

    const icon = document.getElementById('theme-icon');
    if (icon) {
        icon.textContent = savedTheme === 'dark' ? '☀️' : '🌙';
    }
})();

// Initialize animations state on page load
(function initAnimations() {
    const animationsState = localStorage.getItem('animations') || 'enabled';
    if (animationsState === 'disabled') {
        document.body.classList.add('animations-disabled');
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

    fetch('/' + action + '/' + id, {method: 'POST'})
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

// Keyboard shortcuts
document.addEventListener('keydown', (event) => {
    // Check for Ctrl+Shift+D - Clear All
    if (event.ctrlKey && event.shiftKey && event.key === 'D') {
        event.preventDefault();
        clearAllFiles();
    }

    // Check for 'A' key - Toggle Animations (only if not typing in input/textarea)
    if (event.key === 'a' || event.key === 'A') {
        const target = event.target;
        // Don't trigger if user is typing in an input field
        if (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA') {
            event.preventDefault();
            toggleAnimations();
        }
    }
});

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

// Clear all files and folders to initial state - show modal
function clearAllFiles() {
    document.getElementById('clearAllModal').style.display = 'block';
}

// Close clear all modal
function closeClearAllModal() {
    document.getElementById('clearAllModal').style.display = 'none';
}

// Select all items in clear modal
function selectAllClearItems() {
    document.getElementById('clearTestInputs').checked = true;
    document.getElementById('clearTestOutputs').checked = true;
    document.getElementById('clearActualOutputs').checked = true;
    document.getElementById('clearMatchedTests').checked = true;
    document.getElementById('clearProblemMeta').checked = true;
    document.getElementById('clearSingleFiles').checked = true;
}

// Deselect all items in clear modal
function deselectAllClearItems() {
    document.getElementById('clearTestInputs').checked = false;
    document.getElementById('clearTestOutputs').checked = false;
    document.getElementById('clearActualOutputs').checked = false;
    document.getElementById('clearMatchedTests').checked = false;
    document.getElementById('clearProblemMeta').checked = false;
    document.getElementById('clearSingleFiles').checked = false;
}

// Confirm and execute clear all
async function confirmClearAll() {
    const options = {
        clearTestInputs: document.getElementById('clearTestInputs').checked,
        clearTestOutputs: document.getElementById('clearTestOutputs').checked,
        clearActualOutputs: document.getElementById('clearActualOutputs').checked,
        clearMatchedTests: document.getElementById('clearMatchedTests').checked,
        clearProblemMeta: document.getElementById('clearProblemMeta').checked,
        clearSingleFiles: document.getElementById('clearSingleFiles').checked
    };

    // Check if at least one item is selected
    const hasSelection = Object.values(options).some(v => v);
    if (!hasSelection) {
        showToast('Please select at least one item to delete', 'error');
        return;
    }

    // Close modal
    closeClearAllModal();

    try {
        const response = await fetch('/api/clear-all', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(options)
        });

        const result = await response.json();

        if (result.success) {
            const deletedCount = result.deletedItems.length;
            const message = deletedCount > 0
                ? `Successfully cleared ${deletedCount} item(s)`
                : 'Selected items were already clean - nothing to delete';

            showToast(message, 'success');

            // Show detailed info in console
            console.log('🗑️ Clear All Results:');
            console.log('  Success:', result.success);
            console.log('  Message:', result.message);
            console.log('  Deleted items:', result.deletedItems);

            // Reload page after 2 seconds
            setTimeout(() => {
                window.location.reload();
            }, 2000);
        } else {
            showToast(result.message, 'error');
        }
    } catch (error) {
        console.error('Error clearing files:', error);
        showToast('Network error: ' + error.message, 'error');
    }
}

// Close modal when clicking outside
window.addEventListener('click', (event) => {
    const modal = document.getElementById('clearAllModal');
    if (event.target === modal) {
        closeClearAllModal();
    }
});

