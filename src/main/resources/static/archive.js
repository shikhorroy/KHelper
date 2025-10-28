// Archive management JavaScript

let currentDeleteArchiveId = null;
let currentProblemData = null;

// Load archives on page load
document.addEventListener('DOMContentLoaded', () => {
    loadArchives();
    checkCurrentProblem();
});

// Load all archives
async function loadArchives() {
    try {
        const response = await fetch('/api/archives');
        const data = await response.json();

        displayArchives(data);
    } catch (error) {
        console.error('Error loading archives:', error);
        showNotification('Failed to load archives', 'error');
    }
}

// Display archives in the UI
function displayArchives(data) {
    const archiveList = document.getElementById('archiveList');
    const emptyState = document.getElementById('emptyState');
    const archiveCount = document.getElementById('archiveCount');

    archiveCount.textContent = data.count;

    if (data.count === 0) {
        archiveList.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    archiveList.innerHTML = data.archives.map(archive => createArchiveCard(archive)).join('');
}

// Create HTML for a single archive card
function createArchiveCard(archive) {
    const archivedDate = new Date(archive.archivedAt).toLocaleString();

    return `
        <div class="archive-item">
            <div class="archive-header">
                <div class="archive-title">
                    <h3>${escapeHtml(archive.name)}</h3>
                    <p class="archive-group">${escapeHtml(archive.group)}</p>
                </div>
                <div class="archive-actions">
                    <button class="btn btn-import" onclick="importArchive('${archive.id}')">
                        📥 Import
                    </button>
                    <button class="btn btn-delete" onclick="deleteArchive('${archive.id}', '${escapeHtml(archive.name)}')">
                        🗑️ Delete
                    </button>
                </div>
            </div>
            <div class="archive-info">
                <div class="archive-info-item">
                    <span>📅</span>
                    <span>${archivedDate}</span>
                </div>
                <div class="archive-info-item">
                    <span>🧪</span>
                    <span>${archive.testCount} test case${archive.testCount !== 1 ? 's' : ''}</span>
                </div>
            </div>
            ${archive.url ? `<a href="${escapeHtml(archive.url)}" target="_blank" class="archive-url">🔗 ${escapeHtml(archive.url)}</a>` : ''}
        </div>
    `;
}

// Check if there's a current problem to archive
async function checkCurrentProblem() {
    try {
        const response = await fetch('/.problem-meta.txt');
        if (response.ok) {
            const metaText = await response.text();
            const lines = metaText.split('\n');
            if (lines.length >= 3 && lines[0].trim()) {
                // Parse time and memory limits
                const limitsLine = lines[3] || '';
                const tlMatch = limitsLine.match(/TL\(ms\)=(-?\d+)/);
                const mlMatch = limitsLine.match(/ML\(MB\)=(-?\d+)/);

                // Count test cases
                const inputFiles = await listFiles('sample/input');

                // There's a current problem
                currentProblemData = {
                    name: lines[0].trim(),
                    group: lines[1].trim(),
                    url: lines[2].trim(),
                    timeLimit: tlMatch ? parseInt(tlMatch[1]) : -1,
                    memoryLimit: mlMatch ? parseInt(mlMatch[1]) : -1,
                    testCount: inputFiles.length
                };

                // Display current problem info
                displayCurrentProblem(currentProblemData);
            } else {
                showNoProblem();
            }
        } else {
            showNoProblem();
        }
    } catch (error) {
        console.log('No current problem found');
        showNoProblem();
    }
}

// Display current problem information
function displayCurrentProblem(problem) {
    const currentProblemSection = document.getElementById('currentProblemSection');
    const noProblemSection = document.getElementById('noProblemSection');

    currentProblemSection.style.display = 'block';
    noProblemSection.style.display = 'none';

    document.getElementById('currentProblemName').textContent = problem.name;
    document.getElementById('currentProblemGroup').textContent = problem.group;

    // Format time limit
    const timeText = problem.timeLimit > 0
        ? `${problem.timeLimit} ms`
        : 'Not specified';
    document.getElementById('currentProblemTime').textContent = timeText;

    // Format memory limit
    const memoryText = problem.memoryLimit > 0
        ? `${problem.memoryLimit} MB`
        : 'Not specified';
    document.getElementById('currentProblemMemory').textContent = memoryText;

    // Test count
    const testText = `${problem.testCount} test case${problem.testCount !== 1 ? 's' : ''}`;
    document.getElementById('currentProblemTests').textContent = testText;

    // URL
    const urlElement = document.getElementById('currentProblemUrl');
    if (problem.url) {
        urlElement.href = problem.url;
        urlElement.style.display = 'inline-flex';
    } else {
        urlElement.style.display = 'none';
    }
}

// Show no problem message
function showNoProblem() {
    const currentProblemSection = document.getElementById('currentProblemSection');
    const noProblemSection = document.getElementById('noProblemSection');

    currentProblemSection.style.display = 'none';
    noProblemSection.style.display = 'block';
}

// Archive the current problem
async function archiveCurrentProblem() {
    if (!currentProblemData) {
        showNotification('No current problem to archive. Please load a problem first.', 'warning');
        return;
    }

    try {
        // Read metadata to get full problem details
        const response = await fetch('/.problem-meta.txt');
        if (!response.ok) {
            showNotification('No problem metadata found', 'error');
            return;
        }

        const metaText = await response.text();
        const lines = metaText.split('\n');

        // Parse time and memory limits
        const limitsLine = lines[3] || '';
        const tlMatch = limitsLine.match(/TL\(ms\)=(-?\d+)/);
        const mlMatch = limitsLine.match(/ML\(MB\)=(-?\d+)/);

        // Get test cases
        const inputFiles = await listFiles('sample/input');
        const outputFiles = await listFiles('sample/output');

        const tests = [];
        for (let i = 1; i <= inputFiles.length; i++) {
            const inputContent = await readFile(`sample/input/${i}.txt`);
            const outputContent = await readFile(`sample/output/${i}.txt`);
            if (inputContent !== null && outputContent !== null) {
                tests.push({
                    input: inputContent,
                    output: outputContent
                });
            }
        }

        const problem = {
            name: lines[0].trim(),
            group: lines[1].trim(),
            url: lines[2].trim(),
            timeLimit: tlMatch ? parseInt(tlMatch[1]) : -1,
            memoryLimit: mlMatch ? parseInt(mlMatch[1]) : -1,
            interactive: false,
            tests: tests
        };

        // Try to archive
        const archiveResponse = await fetch('/api/archives/archive', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(problem)
        });

        const result = await archiveResponse.json();

        if (result.success) {
            showNotification('Problem archived successfully!', 'success');
            loadArchives();
            // Refresh current problem display
            await checkCurrentProblem();
        } else {
            // Archive already exists
            if (result.message.includes('already exists')) {
                showOverwriteModal(problem);
            } else {
                showNotification(result.message, 'error');
            }
        }

    } catch (error) {
        console.error('Error archiving problem:', error);
        showNotification('Failed to archive problem', 'error');
    }
}

// Import an archive
async function importArchive(archiveId) {
    if (!confirm('Import this archive? This will replace your current sample inputs/outputs and Solver.kt')) {
        return;
    }

    try {
        const response = await fetch(`/api/archives/import/${archiveId}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showNotification(result.message, 'success');
            // Reload current problem data
            await checkCurrentProblem();
        } else {
            showNotification(result.message, 'error');
        }

    } catch (error) {
        console.error('Error importing archive:', error);
        showNotification('Failed to import archive', 'error');
    }
}

// Delete an archive (show confirmation)
function deleteArchive(archiveId, archiveName) {
    currentDeleteArchiveId = archiveId;
    document.getElementById('deleteArchiveName').textContent = archiveName;
    document.getElementById('deleteModal').style.display = 'block';
}

// Confirm delete
async function confirmDelete() {
    if (!currentDeleteArchiveId) return;

    try {
        const response = await fetch(`/api/archives/${currentDeleteArchiveId}`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            showNotification('Archive deleted successfully', 'success');
            loadArchives();
        } else {
            showNotification(result.message, 'error');
        }

    } catch (error) {
        console.error('Error deleting archive:', error);
        showNotification('Failed to delete archive', 'error');
    } finally {
        closeDeleteModal();
    }
}

// Close delete modal
function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    currentDeleteArchiveId = null;
}

// Show overwrite modal
function showOverwriteModal(problemData) {
    currentProblemData = problemData;
    document.getElementById('overwriteModal').style.display = 'block';
}

// Confirm overwrite
async function confirmOverwrite() {
    if (!currentProblemData) return;

    try {
        const response = await fetch('/api/archives/archive-overwrite', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(currentProblemData)
        });

        const result = await response.json();

        if (result.success) {
            showNotification('Archive overwritten successfully!', 'success');
            loadArchives();
            // Refresh current problem display
            await checkCurrentProblem();
        } else {
            showNotification(result.message, 'error');
        }

    } catch (error) {
        console.error('Error overwriting archive:', error);
        showNotification('Failed to overwrite archive', 'error');
    } finally {
        closeOverwriteModal();
    }
}

// Close overwrite modal
function closeOverwriteModal() {
    document.getElementById('overwriteModal').style.display = 'none';
}

// Helper function to read file content
async function readFile(path) {
    try {
        const response = await fetch(`/${path}`);
        if (response.ok) {
            return await response.text();
        }
        return null;
    } catch (error) {
        return null;
    }
}

// Helper function to list files (simplified - assumes sequential numbering)
async function listFiles(path) {
    const files = [];
    for (let i = 1; i <= 100; i++) {
        const response = await fetch(`/${path}/${i}.txt`);
        if (response.ok) {
            files.push(`${i}.txt`);
        } else {
            break;
        }
    }
    return files;
}

// Show notification
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
        notification.remove();
    }, 4000);
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Close modals when clicking outside
window.onclick = function (event) {
    const deleteModal = document.getElementById('deleteModal');
    const overwriteModal = document.getElementById('overwriteModal');

    if (event.target === deleteModal) {
        closeDeleteModal();
    }
    if (event.target === overwriteModal) {
        closeOverwriteModal();
    }
}

