// Archive management JavaScript with group support

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
        showNotification('Animations Enabled', 'success');
    } else {
        body.classList.add('animations-disabled');
        localStorage.setItem('animations', 'disabled');
        showNotification('Animations Disabled', 'success');
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

let currentDeleteArchiveId = null;
let currentDeleteGroupName = null;
let currentDeleteArchiveName = null;
let currentProblemData = null;
let currentRenameGroupName = null;
let currentDeleteGroupData = null;

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

// Load archives on page load
document.addEventListener('DOMContentLoaded', () => {
    loadArchives();
    checkCurrentProblem();
});

// Load all archives grouped by groups
async function loadArchives() {
    try {
        const response = await fetch('/api/archives');
        const data = await response.json();

        displayGroupedArchives(data);
    } catch (error) {
        console.error('Error loading archives:', error);
        showNotification('Failed to load archives', 'error');
    }
}

// Display archives grouped in the UI
function displayGroupedArchives(data) {
    const groupsList = document.getElementById('groupsList');
    const emptyState = document.getElementById('emptyState');
    const groupCount = document.getElementById('groupCount');
    const problemCount = document.getElementById('problemCount');

    groupCount.textContent = data.totalGroups;
    problemCount.textContent = data.totalProblems;

    if (data.totalGroups === 0) {
        groupsList.innerHTML = '';
        emptyState.style.display = 'block';
        return;
    }

    emptyState.style.display = 'none';
    groupsList.innerHTML = data.groups.map(group => createGroupCard(group)).join('');
}

// Create HTML for a group card with its problems
function createGroupCard(group) {
    const problemsHtml = group.problems.map(problem => createProblemCard(problem, group.groupName)).join('');

    return `
        <div class="group-container">
            <div class="group-header" onclick="toggleGroup('${escapeHtml(group.groupName)}')">
                <div class="group-header-left">
                    <span class="group-toggle" id="toggle-${escapeHtml(group.groupName)}">▶</span>
                    <div class="group-info">
                        <h2>📁 ${escapeHtml(group.groupName)}</h2>
                        <p>${group.problemCount} problem${group.problemCount !== 1 ? 's' : ''}</p>
                    </div>
                </div>
                <div class="group-actions" onclick="event.stopPropagation()">
                    <button class="btn btn-group-action" onclick="renameGroup('${escapeHtml(group.groupName)}')">
                        ✏️ Rename
                    </button>
                    <button class="btn btn-group-action" onclick="deleteGroup('${escapeHtml(group.groupName)}', ${group.problemCount})">
                        🗑️ Delete
                    </button>
                </div>
            </div>
            <div class="group-problems" id="problems-${escapeHtml(group.groupName)}">
                <div class="problem-list">
                    ${problemsHtml}
                </div>
            </div>
        </div>
    `;
}

// Create HTML for a single problem card
function createProblemCard(problem, groupName) {
    const archivedDate = new Date(problem.archivedAt).toLocaleString();

    return `
        <div class="archive-item">
            <div class="archive-header">
                <div class="archive-title">
                    <h3>${escapeHtml(problem.name)}</h3>
                </div>
                <div class="archive-actions">
                    <button class="btn btn-import" onclick="importArchive('${escapeHtml(problem.id)}', '${escapeHtml(groupName)}')">
                        📥 Import
                    </button>
                    <button class="btn btn-delete" onclick="deleteArchive('${escapeHtml(problem.id)}', '${escapeHtml(groupName)}', '${escapeHtml(problem.name)}')">
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
                    <span>${problem.testCount} test case${problem.testCount !== 1 ? 's' : ''}</span>
                </div>
            </div>
            ${problem.url ? `<a href="${escapeHtml(problem.url)}" target="_blank" class="archive-url">🔗 ${escapeHtml(problem.url)}</a>` : ''}
        </div>
    `;
}

// Toggle group expansion
function toggleGroup(groupName) {
    const problemsDiv = document.getElementById(`problems-${groupName}`);
    const toggleIcon = document.getElementById(`toggle-${groupName}`);

    if (problemsDiv && toggleIcon) {
        problemsDiv.classList.toggle('expanded');
        toggleIcon.classList.toggle('expanded');
    }
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
                currentProblemData = problem;
                showOverwriteModal();
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
async function importArchive(archiveId, groupName) {
    if (!confirm('Import this archive? This will replace your current sample inputs/outputs and Solution.kt')) {
        return;
    }

    try {
        const response = await fetch(`/api/archives/import/${encodeURIComponent(groupName)}/${encodeURIComponent(archiveId)}`, {
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
function deleteArchive(archiveId, groupName, archiveName) {
    currentDeleteArchiveId = archiveId;
    currentDeleteGroupName = groupName;
    currentDeleteArchiveName = archiveName;
    document.getElementById('deleteArchiveName').textContent = archiveName;
    document.getElementById('deleteModal').style.display = 'block';
}

// Confirm delete archive
async function confirmDelete() {
    if (!currentDeleteArchiveId || !currentDeleteGroupName) return;

    try {
        const response = await fetch(`/api/archives/${encodeURIComponent(currentDeleteGroupName)}/${encodeURIComponent(currentDeleteArchiveId)}`, {
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
    currentDeleteGroupName = null;
    currentDeleteArchiveName = null;
}

// Rename group
function renameGroup(groupName) {
    currentRenameGroupName = groupName;
    document.getElementById('currentGroupName').value = groupName;
    document.getElementById('newGroupName').value = '';
    document.getElementById('renameGroupModal').style.display = 'block';
}

// Confirm rename group
async function confirmRenameGroup() {
    const newGroupName = document.getElementById('newGroupName').value.trim();

    if (!newGroupName) {
        showNotification('Please enter a new group name', 'warning');
        return;
    }

    if (newGroupName === currentRenameGroupName) {
        showNotification('New name must be different from current name', 'warning');
        return;
    }

    try {
        const response = await fetch('/api/archives/group/rename', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                oldGroupName: currentRenameGroupName,
                newGroupName: newGroupName
            })
        });

        const result = await response.json();

        if (result.success) {
            showNotification('Group renamed successfully', 'success');
            loadArchives();
        } else {
            showNotification(result.message, 'error');
        }

    } catch (error) {
        console.error('Error renaming group:', error);
        showNotification('Failed to rename group', 'error');
    } finally {
        closeRenameGroupModal();
    }
}

// Close rename group modal
function closeRenameGroupModal() {
    document.getElementById('renameGroupModal').style.display = 'none';
    currentRenameGroupName = null;
}

// Delete group
function deleteGroup(groupName, problemCount) {
    currentDeleteGroupData = { groupName, problemCount };
    document.getElementById('deleteGroupName').textContent = groupName;
    document.getElementById('deleteGroupCount').textContent = problemCount;
    document.getElementById('deleteGroupModal').style.display = 'block';
}

// Confirm delete group
async function confirmDeleteGroup() {
    if (!currentDeleteGroupData) return;

    try {
        const response = await fetch(`/api/archives/group/${encodeURIComponent(currentDeleteGroupData.groupName)}`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            showNotification('Group deleted successfully', 'success');
            loadArchives();
        } else {
            showNotification(result.message, 'error');
        }

    } catch (error) {
        console.error('Error deleting group:', error);
        showNotification('Failed to delete group', 'error');
    } finally {
        closeDeleteGroupModal();
    }
}

// Close delete group modal
function closeDeleteGroupModal() {
    document.getElementById('deleteGroupModal').style.display = 'none';
    currentDeleteGroupData = null;
}

// Show overwrite modal
function showOverwriteModal() {
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
    const renameGroupModal = document.getElementById('renameGroupModal');
    const deleteGroupModal = document.getElementById('deleteGroupModal');

    if (event.target === deleteModal) {
        closeDeleteModal();
    }
    if (event.target === overwriteModal) {
        closeOverwriteModal();
    }
    if (event.target === renameGroupModal) {
        closeRenameGroupModal();
    }
    if (event.target === deleteGroupModal) {
        closeDeleteGroupModal();
    }
}

