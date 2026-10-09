document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'documents') {
        setupDocumentListPage();
    }
    if (page === 'document-upload') {
        setupDocumentUploadPage();
    }
    if (page === 'detail') {
        setupEmployeeDetailDocumentTab();
    }
});

async function setupDocumentListPage() {
    const employeeId = getEmployeeIdFromUrl();
    setupDocumentLinks(employeeId);
    await loadDocuments(employeeId);
}

function setupDocumentUploadPage() {
    const employeeId = getEmployeeIdFromUrl();
    const form = document.querySelector('#documentUploadForm');
    form.employeeId.value = employeeId;
    setupDocumentLinks(employeeId);

    form.addEventListener('submit', async event => {
        event.preventDefault();
        await uploadDocument(form, employeeId);
    });
}

function setupEmployeeDetailDocumentTab() {
    const employeeId = getEmployeeIdFromUrl('id');
    setupDocumentLinks(employeeId);

    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', async () => {
            document.querySelectorAll('.tab-button').forEach(item => item.classList.remove('active'));
            document.querySelectorAll('.tab-panel').forEach(panel => panel.classList.add('hidden'));
            button.classList.add('active');
            document.querySelector(`#${button.dataset.tab}Tab`).classList.remove('hidden');

            if (button.dataset.tab === 'documents') {
                await loadDocuments(employeeId);
            }
        });
    });
}

async function loadDocuments(employeeId) {
    try {
        const payload = await DocumentApi.get(`/documents?employeeId=${employeeId}`);
        renderDocumentRows(payload.data);
    } catch (error) {
        showDocumentMessage(error.message, 'error');
    }
}

async function uploadDocument(form, employeeId) {
    try {
        const payload = await DocumentApi.upload('/documents/upload', new FormData(form));
        showDocumentMessage(payload.message);
        form.reset();
        form.employeeId.value = employeeId;
        if (typeof loadEmployeeHistory === 'function') {
            await loadEmployeeHistory(employeeId, '#historyRows');
        }
    } catch (error) {
        showDocumentMessage(error.message, 'error');
    }
}

async function deleteDocument(documentId) {
    if (!confirm('Delete this document?')) {
        return;
    }

    try {
        const payload = await DocumentApi.post('/documents/delete', { documentId });
        showDocumentMessage(payload.message);
        const employeeId = getEmployeeIdFromUrl(document.body.dataset.page === 'detail' ? 'id' : 'employeeId');
        await loadDocuments(employeeId);
        if (typeof loadEmployeeHistory === 'function') {
            await loadEmployeeHistory(employeeId, '#historyRows');
        }
    } catch (error) {
        showDocumentMessage(error.message, 'error');
    }
}

function renderDocumentRows(documents) {
    const tbody = document.querySelector('#documentRows');
    if (!tbody) {
        return;
    }
    if (!documents.length) {
        tbody.innerHTML = '<tr><td colspan="5">No documents found.</td></tr>';
        return;
    }

    tbody.innerHTML = documents.map(document => `
        <tr>
            <td>${safeDocumentValue(document.documentName)}</td>
            <td>${safeDocumentValue(document.documentType)}</td>
            <td>${safeDocumentValue(document.uploadedDate)}</td>
            <td>${safeDocumentValue(document.uploadedBy)}</td>
            <td class="actions">
                <a href="${documentApiUrl(`/documents/download?id=${document.documentId}&inline=true`)}" target="_blank" rel="noopener">View</a>
                <a href="${documentApiUrl(`/documents/download?id=${document.documentId}`)}" download="${safeDocumentValue(document.fileName || document.documentName || 'document')}">Download</a>
                <button class="danger" onclick="deleteDocument(${document.documentId})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function setupDocumentLinks(employeeId) {
    document.querySelectorAll('#uploadDocumentLink').forEach(link => {
        link.href = `../document/document-upload.html?employeeId=${employeeId}`;
    });
    document.querySelectorAll('#cancelDocumentLink, #cancelDocumentButton').forEach(link => {
        link.href = `../document/employee-document-list.html?employeeId=${employeeId}`;
    });
}

function getEmployeeIdFromUrl(param = 'employeeId') {
    const value = new URLSearchParams(location.search).get(param);
    if (!value) {
        showDocumentMessage('Employee id is required.', 'error');
        return '';
    }
    return value;
}

const DocumentApi = {
    async get(url) {
        const response = await fetch(documentApiUrl(url), { headers: { Accept: 'application/json' } });
        return handleDocumentResponse(response);
    },

    async post(url, data) {
        const response = await fetch(documentApiUrl(url), {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: new URLSearchParams(data)
        });
        return handleDocumentResponse(response);
    },

    async upload(url, formData) {
        const response = await fetch(documentApiUrl(url), {
            method: 'POST',
            headers: { Accept: 'application/json' },
            body: formData
        });
        return handleDocumentResponse(response);
    }
};

function documentApiUrl(url) {
    const first = location.pathname.split('/').filter(Boolean)[0] || '';
    const rootFolders = ['employee', 'document'];
    const contextPath = rootFolders.includes(first) ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handleDocumentResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || 'Request failed.');
    }
    return payload;
}

function showDocumentMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    if (!element) {
        return;
    }
    element.textContent = message;
    element.className = `message show ${type}`;
}

function safeDocumentValue(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}
