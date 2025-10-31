// Test Management JavaScript

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
        showStatus('Animations Enabled', true);
    } else {
        body.classList.add('animations-disabled');
        localStorage.setItem('animations', 'disabled');
        showStatus('Animations Disabled', true);
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

function showStatus(message, isSuccess) {
    const statusDiv = document.getElementById('status-message');
    statusDiv.textContent = message;
    statusDiv.className = 'status-message ' + (isSuccess ? 'success' : 'error');
    statusDiv.style.display = 'block';

    setTimeout(() => {
        statusDiv.style.display = 'none';
    }, 5000);
}

async function addTestCase() {
    const input = document.getElementById('new-test-input').value;
    const output = document.getElementById('new-test-output').value;

    if (!input.trim() || !output.trim()) {
        showStatus('⚠️ Please fill in both input and output fields', false);
        return;
    }

    try {
        const response = await fetch('/api/tests/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ input, output })
        });

        const result = await response.json();

        if (result.success) {
            showStatus('✅ ' + result.message, true);
            // Clear form
            document.getElementById('new-test-input').value = '';
            document.getElementById('new-test-output').value = '';
            // Reload page after 1 second
            setTimeout(() => loadTestCases(), 1000);
        } else {
            showStatus('❌ ' + result.message, false);
        }
    } catch (error) {
        showStatus('❌ Network error: ' + error.message, false);
    }
}

function toggleEdit(testNumber) {
    const viewMode = document.getElementById('view-' + testNumber);
    const editMode = document.getElementById('edit-' + testNumber);
    const editBtn = document.getElementById('btn-edit-' + testNumber);

    if (editMode.style.display === 'none' || !editMode.style.display) {
        viewMode.style.display = 'none';
        editMode.style.display = 'block';
        editBtn.textContent = '👁️ View';
        editBtn.style.background = '#95a5a6';
    } else {
        viewMode.style.display = 'block';
        editMode.style.display = 'none';
        editBtn.textContent = '✏️ Edit';
        editBtn.style.background = '#3498db';
    }
}

function cancelEdit(testNumber) {
    toggleEdit(testNumber);
}

async function saveTestCase(testNumber) {
    const input = document.getElementById('edit-input-' + testNumber).value;
    const output = document.getElementById('edit-output-' + testNumber).value;

    if (!input.trim() || !output.trim()) {
        showStatus('⚠️ Input and output cannot be empty', false);
        return;
    }

    try {
        const response = await fetch('/api/tests/update', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ testNumber, input, output })
        });

        const result = await response.json();

        if (result.success) {
            showStatus('✅ ' + result.message, true);
            setTimeout(() => loadTestCases(), 1000);
        } else {
            showStatus('❌ ' + result.message, false);
        }
    } catch (error) {
        showStatus('❌ Network error: ' + error.message, false);
    }
}

async function deleteTestCase(testNumber) {
    if (!confirm('Are you sure you want to delete Test Case #' + testNumber + '?')) {
        return;
    }

    try {
        const response = await fetch('/api/tests/delete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ testNumber })
        });

        const result = await response.json();

        if (result.success) {
            showStatus('✅ ' + result.message, true);
            setTimeout(() => loadTestCases(), 1000);
        } else {
            showStatus('❌ ' + result.message, false);
        }
    } catch (error) {
        showStatus('❌ Network error: ' + error.message, false);
    }
}

function renderEditableTestCase(testNumber, test) {
    const div = document.createElement('div');
    div.className = 'editable-test-case';
    div.id = 'test-' + testNumber;

    div.innerHTML = `
        <div class="test-case-header">
            <h3>Test Case #${testNumber}</h3>
            <div class="test-actions">
                <button class="btn-edit" onclick="toggleEdit(${testNumber})" id="btn-edit-${testNumber}">
                    ✏️ Edit
                </button>
                <button class="btn-delete" onclick="deleteTestCase(${testNumber})">
                    🗑️ Delete
                </button>
            </div>
        </div>
        
        <!-- View mode -->
        <div class="test-view-mode" id="view-${testNumber}">
            <div class="test-section test-section-top">
                <div class="test-section-title">Input:</div>
                <pre class="test-data">${escapeHtml(test.input)}</pre>
            </div>
            <div class="test-section test-section-bottom">
                <div class="test-section-title">Expected Output:</div>
                <pre class="test-data">${escapeHtml(test.output)}</pre>
            </div>
        </div>
        
        <!-- Edit mode (hidden by default) -->
        <div class="test-edit-mode" id="edit-${testNumber}" style="display: none;">
            <div class="form-group">
                <label>Input:</label>
                <textarea class="test-input" id="edit-input-${testNumber}" rows="5">${escapeHtml(test.input)}</textarea>
            </div>
            <div class="form-group">
                <label>Expected Output:</label>
                <textarea class="test-output" id="edit-output-${testNumber}" rows="5">${escapeHtml(test.output)}</textarea>
            </div>
            <div class="edit-actions">
                <button class="btn-save" onclick="saveTestCase(${testNumber})">💾 Save</button>
                <button class="btn-cancel" onclick="cancelEdit(${testNumber})">❌ Cancel</button>
            </div>
        </div>
    `;

    return div;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

async function loadTestCases() {
    try {
        const response = await fetch('/api/tests');
        const tests = await response.json();

        document.getElementById('total-tests').textContent = tests.length;
        document.getElementById('test-count').textContent = tests.length;

        const container = document.getElementById('tests-container');
        container.innerHTML = '';

        if (tests.length === 0) {
            const noTests = document.createElement('div');
            noTests.className = 'no-tests';
            noTests.textContent = 'No test cases available. Add your first test case above!';
            container.appendChild(noTests);
        } else {
            tests.forEach((test, idx) => {
                const testNumber = idx + 1;
                container.appendChild(renderEditableTestCase(testNumber, test));
            });
        }
    } catch (error) {
        console.error('Error loading test cases:', error);
        showStatus('❌ Error loading test cases', false);
    }
}

// Keyboard shortcuts
document.addEventListener('keydown', function(e) {
    // 'A' key - Toggle Animations (only if not typing in input/textarea)
    if (e.key === 'a' || e.key === 'A') {
        const target = e.target;
        if (target.tagName !== 'INPUT' && target.tagName !== 'TEXTAREA') {
            e.preventDefault();
            toggleAnimations();
            return;
        }
    }

    // Ctrl/Cmd + Enter to add test case (when in add form)
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
        const activeElement = document.activeElement;
        if (activeElement.id === 'new-test-input' || activeElement.id === 'new-test-output') {
            e.preventDefault();
            addTestCase();
        }
    }
});

// Load test cases on page load
document.addEventListener('DOMContentLoaded', loadTestCases);

