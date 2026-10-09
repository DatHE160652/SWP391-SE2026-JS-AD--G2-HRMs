const PositionApi = {
    async get(url) {
        const response = await fetch(positionApiUrl(url), { headers: { Accept: 'application/json' } });
        return handlePositionResponse(response);
    },

    async post(url, data) {
        const response = await fetch(positionApiUrl(url), {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
            },
            body: new URLSearchParams(data)
        });
        return handlePositionResponse(response);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (page === 'position-list') {
        loadPositions();
    }
    if (page === 'position-form') {
        setupPositionForm();
    }
});

async function loadPositions() {
    try {
        const payload = await PositionApi.get('/positions');
        renderPositionRows(payload.data);
        document.querySelector('#totalPositions').textContent = payload.data.length;
    } catch (error) {
        showPositionMessage(error.message, 'error');
    }
}

async function setupPositionForm() {
    const positionId = new URLSearchParams(location.search).get('id');
    const form = document.querySelector('#positionForm');
    await loadDepartmentOptions(form);

    if (positionId) {
        document.querySelector('#positionFormTitle').textContent = 'Update Position';
        const payload = await PositionApi.get(`/positions/detail?id=${positionId}`);
        fillPositionForm(form, payload.data);
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (positionId) {
            await updatePosition(form);
        } else {
            await createPosition(form);
        }
    });
}

async function createPosition(form) {
    try {
        const payload = await PositionApi.post('/positions/create', readPositionForm(form));
        showPositionMessage(payload.message);
        form.reset();
    } catch (error) {
        showPositionMessage(error.message, 'error');
    }
}

async function updatePosition(form) {
    try {
        const payload = await PositionApi.post('/positions/update', readPositionForm(form));
        showPositionMessage(payload.message);
    } catch (error) {
        showPositionMessage(error.message, 'error');
    }
}

async function deletePosition(positionId) {
    if (!confirm('Delete this position?')) {
        return;
    }

    try {
        const payload = await PositionApi.post('/positions/delete', { positionId });
        showPositionMessage(payload.message);
        await loadPositions();
    } catch (error) {
        showPositionMessage(error.message, 'error');
    }
}

function renderPositionRows(positions) {
    const tbody = document.querySelector('#positionRows');
    if (!positions.length) {
        tbody.innerHTML = '<tr><td colspan="5">No positions found.</td></tr>';
        return;
    }

    tbody.innerHTML = positions.map(position => `
        <tr>
            <td><strong>${safePositionValue(position.positionTitle)}</strong></td>
            <td>${safePositionValue(position.departmentName)}</td>
            <td>${safePositionValue(position.description)}</td>
            <td>${safePositionValue(position.createdAt)}</td>
            <td class="actions">
                <a href="position-form?id=${position.positionId}">Edit</a>
                <button class="danger" onclick="deletePosition(${position.positionId})">Delete</button>
            </td>
        </tr>
    `).join('');
}

function fillPositionForm(form, position) {
    Object.keys(position).forEach(key => {
        if (form[key] && position[key] !== null) {
            form[key].value = position[key];
        }
    });
}

function readPositionForm(form) {
    return Object.fromEntries(new FormData(form).entries());
}

async function loadDepartmentOptions(form) {
    const payload = await PositionApi.get('/departments');
    const departments = payload.data || [];
    form.departmentId.innerHTML = '<option value="">Select Department</option>' + departments.map(department => `
        <option value="${department.departmentId}">${safePositionValue(department.departmentName)}</option>
    `).join('');
}

function positionApiUrl(url) {
    const first = location.pathname.split('/').filter(Boolean)[0] || '';
    const contextPath = first === 'position' ? '' : `/${first}`;
    return `${contextPath}${url}`;
}

async function handlePositionResponse(response) {
    const payload = await response.json();
    if (!response.ok || payload.success === false) {
        throw new Error(payload.message || 'Request failed.');
    }
    return payload;
}

function showPositionMessage(message, type = 'success') {
    const element = document.querySelector('#message');
    element.textContent = message;
    element.className = `message show ${type}`;
}

function safePositionValue(value) {
    return value === null || value === undefined || value === '' ? '-' : String(value);
}
