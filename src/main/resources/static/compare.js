// Comparison page JavaScript

// Theme toggle functionality
function toggleTheme() {
    const html = document.documentElement;
    const currentTheme = html.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

    html.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);

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
document.addEventListener('DOMContentLoaded', loadComparisons);

