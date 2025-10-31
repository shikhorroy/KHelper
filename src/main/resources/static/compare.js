// Comparison page JavaScript

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

    // Auto-remove after 3 seconds
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

function toggleExpand(fileId) {
    const content = document.getElementById('content-' + fileId);
    const button = document.getElementById('btn-' + fileId);

    if (content.style.display === 'none' || !content.style.display) {
        content.style.display = 'block';
        button.textContent = '▼ Hide Output Content';
        button.classList.add('expanded');

        // Load content if not already loaded
        const codeElement = document.getElementById('code-' + fileId);
        if (codeElement && codeElement.textContent === 'Loading content...') {
            const fileName = fileId.replace(/_/g, '.');
            loadFileContent(fileName, fileId);
        }
    } else {
        content.style.display = 'none';
        button.textContent = '▶ Show Output Content';
        button.classList.remove('expanded');
    }
}

function renderFileComparison(comparison) {
    const div = document.createElement('div');
    div.className = `file-comparison ${comparison.matched ? 'matched' : 'failed'}`;

    let html = `
        <div class="file-header">
            <h3>${comparison.matched ? '✅' : '❌'} ${escapeHtml(comparison.fileName)}</h3>
            <span class="status-badge ${comparison.matched ? 'badge-success' : 'badge-error'}">
                ${escapeHtml(comparison.message)}
            </span>
        </div>
    `;

    if (!comparison.expectedExists || !comparison.actualExists) {
        html += `<div class="error-message">`;
        if (!comparison.expectedExists) {
            html += 'Expected file not found<br>';
        }
        if (!comparison.actualExists) {
            html += 'Actual output file not found';
        }
        html += '</div>';
    } else if (comparison.matched) {
        // Expandable section for matched files
        const fileId = comparison.fileName.replace(/\./g, '_');
        html += `
            <div class="expandable-section">
                <button class="expand-button" onclick="toggleExpand('${fileId}')" id="btn-${fileId}">
                    ▶ Show Output Content
                </button>
                <div class="expandable-content" id="content-${fileId}" style="display: none;">
                    <div class="matched-content">
                        <h4>Full Output Content:</h4>
                        <div class="output-preview">
                            <div class="content-stats">📄 Loading...</div>
                            <pre class="code-content" id="code-${fileId}">Loading content...</pre>
                        </div>
                    </div>
                </div>
            </div>
        `;
    } else if (comparison.differences && comparison.differences.length > 0) {
        html += `
            <div class="differences-section">
                <h4>Differences (${comparison.differences.length} line(s)):</h4>
        `;

        comparison.differences.forEach(diff => {
            html += `
                <div class="diff-row">
                    <div class="diff-line-number">Line ${diff.lineNumber}</div>
                    <div class="diff-content">
                        <div class="expected-line">
                            <div class="diff-label">Expected:</div>
                            <div class="diff-text">${escapeHtml(diff.expected) || '(empty line)'}</div>
                        </div>
                        <div class="actual-line">
                            <div class="diff-label">Actual:</div>
                            <div class="diff-text">${escapeHtml(diff.actual) || '(empty line)'}</div>
                        </div>
                    </div>
                </div>
            `;
        });

        html += '</div>';
    }

    div.innerHTML = html;
    return div;
}

async function loadFileContent(fileName, fileId) {
    try {
        const response = await fetch(`/api/file-content/${fileName}`);
        const data = await response.json();

        if (data.success) {
            const lines = data.content.split('\n').length;
            const chars = data.content.length;

            document.getElementById('code-' + fileId).textContent = data.content;
            document.querySelector(`#content-${fileId} .content-stats`).textContent =
                `📄 ${lines} line(s) | ${chars} character(s)`;
        } else {
            document.getElementById('code-' + fileId).textContent = 'Error loading file content';
        }
    } catch (error) {
        console.error('Error loading file content:', error);
        document.getElementById('code-' + fileId).textContent = 'Error loading file content';
    }
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadComparisons() {
    try {
        const response = await fetch('/api/compare');
        const result = await response.json();

        // Update summary cards
        document.getElementById('matched-count').textContent = result.matchedFiles;
        document.getElementById('failed-count').textContent = result.totalFiles - result.matchedFiles;
        document.getElementById('total-count').textContent = result.totalFiles;

        // Update matched card styling
        const matchedCard = document.getElementById('matched-card');
        if (result.matchedFiles === result.totalFiles) {
            matchedCard.classList.add('success');
        } else {
            matchedCard.classList.add('warning');
        }

        // Render comparisons
        const container = document.getElementById('comparisons-container');
        container.innerHTML = '';

        if (result.comparisons.length === 0) {
            const noResults = document.createElement('div');
            noResults.className = 'no-results';
            noResults.textContent = 'No comparison results available';
            container.appendChild(noResults);
        } else {
            result.comparisons.forEach(comparison => {
                container.appendChild(renderFileComparison(comparison));
            });
        }
    } catch (error) {
        console.error('Error loading comparisons:', error);
        const container = document.getElementById('comparisons-container');
        container.innerHTML = '<div class="error-message">Error loading comparison results</div>';
    }
}

// Load comparisons on page load
// Keyboard shortcut for toggling animations
document.addEventListener('keydown', (event) => {
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

document.addEventListener('DOMContentLoaded', loadComparisons);

