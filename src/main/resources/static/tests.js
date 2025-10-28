// Test management page JavaScript

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
            <div class="test-section">
                <div class="test-section-title">Input:</div>
                <pre class="test-data">${escapeHtml(test.input)}</pre>
            </div>
            <div class="test-section">
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

