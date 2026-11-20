// WorkSmart Hub dashboard.
// All data comes from the REST API (/api/...). Pagination, search, filtering and sorting
// are done by the database on the server, so the page stays fast with thousands of employees.

const DEPT_LABEL = { IT: "IT", HR: "HR", FINANCE: "Finance", MARKETING: "Marketing" };
const STATUS_LABEL = { ACTIVE: "Active", ON_LEAVE: "On Leave", RESIGNED: "Resigned" };
const STATUS_BADGE = { ACTIVE: "success", ON_LEAVE: "warning", RESIGNED: "secondary" };
const PAGE_SIZE = 5;

const state = {
    page: 0,
    totalPages: 1,
    sortBy: "id",
    direction: "asc",
    isAdmin: false,
    canEdit: false,          // ADMIN or HR
    username: "",
    rows: new Map()          // id -> employee, so "Edit" doesn't need another request
};

let chart = null;
let searchTimer = null;

const $ = (id) => document.getElementById(id);
const money = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 });

/* ---------------- API helper ---------------- */

async function api(path, options = {}) {
    const response = await fetch(path, {
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        ...options
    });

    if (response.status === 401) {                 // session expired / not logged in
        window.location.href = "/login.html";
        throw new Error("Not authenticated");
    }
    if (!response.ok) {
        let body = null;
        try { body = await response.json(); } catch (e) { /* no JSON body */ }
        const error = new Error(body?.message || `Request failed (${response.status})`);
        error.status = response.status;
        error.fieldErrors = body?.fieldErrors || null;
        throw error;
    }
    return response.status === 204 ? null : response.json();
}

/* ---------------- UI helpers ---------------- */

function showToast(message, type = "success") {
    const toast = $("appToast");
    toast.className = `toast align-items-center text-bg-${type} border-0`;
    $("toastBody").textContent = message;
    bootstrap.Toast.getOrCreateInstance(toast).show();
}

function showError(error) {
    const box = $("alertBox");
    box.textContent = "";
    const title = document.createElement("div");
    title.textContent = error.message;
    box.appendChild(title);
    if (error.fieldErrors) {
        const list = document.createElement("ul");
        list.className = "mb-0";
        Object.values(error.fieldErrors).forEach((msg) => {
            const li = document.createElement("li");
            li.textContent = msg;
            list.appendChild(li);
        });
        box.appendChild(list);
    }
    box.classList.remove("d-none");
}

function clearError() {
    $("alertBox").classList.add("d-none");
}

function td(text) {
    const cell = document.createElement("td");
    cell.textContent = text ?? "";        // textContent => user data can never run as HTML/JS
    return cell;
}

/* ---------------- Loading data ---------------- */

async function loadEmployees() {
    $("loadingSpinner").style.display = "block";
    const params = new URLSearchParams({
        page: state.page,
        size: PAGE_SIZE,
        sortBy: state.sortBy,
        direction: state.direction
    });
    const q = $("searchInput").value.trim();
    if (q) params.set("q", q);
    if ($("filterDepartment").value) params.set("department", $("filterDepartment").value);
    if ($("filterStatus").value) params.set("status", $("filterStatus").value);

    try {
        const data = await api(`/api/employees?${params}`);
        // If the last row on the last page was deleted, step back one page
        if (data.content.length === 0 && state.page > 0) {
            state.page = Math.max(data.totalPages - 1, 0);
            return loadEmployees();
        }
        state.totalPages = Math.max(data.totalPages, 1);
        renderTable(data.content);
        renderPager();
    } catch (error) {
        if (error.message !== "Not authenticated") showError(error);
    } finally {
        $("loadingSpinner").style.display = "none";
    }
}

async function loadStats() {
    try {
        const stats = await api("/api/employees/stats");
        $("totalEmployees").textContent = stats.total;
        $("itCount").textContent = stats.byDepartment.IT;
        $("hrCount").textContent = stats.byDepartment.HR;
        $("financeCount").textContent = stats.byDepartment.FINANCE;
        $("marketingCount").textContent = stats.byDepartment.MARKETING;
        renderChart(stats.byDepartment);
    } catch (error) {
        if (error.message !== "Not authenticated") showError(error);
    }
}

function refreshAll() {
    return Promise.all([loadEmployees(), loadStats()]);
}

/* ---------------- Rendering ---------------- */

function renderTable(employees) {
    const body = $("employeeTable");
    body.replaceChildren();
    state.rows.clear();

    if (employees.length === 0) {
        const row = document.createElement("tr");
        const cell = td("No employees found");
        cell.colSpan = 9;
        cell.className = "text-center text-muted";
        row.appendChild(cell);
        body.appendChild(row);
        return;
    }

    employees.forEach((emp) => {
        state.rows.set(emp.id, emp);
        const row = document.createElement("tr");

        row.appendChild(td(emp.id));
        row.appendChild(td(emp.name));
        row.appendChild(td(emp.email));
        row.appendChild(td(DEPT_LABEL[emp.department] ?? emp.department));
        row.appendChild(td(emp.salary == null ? "—" : money.format(emp.salary)));   // USER role: salary hidden
        row.appendChild(td(emp.role));
        row.appendChild(td(emp.phone));

        const statusCell = document.createElement("td");
        const badge = document.createElement("span");
        badge.className = `badge text-bg-${STATUS_BADGE[emp.status] ?? "secondary"}`;
        badge.textContent = STATUS_LABEL[emp.status] ?? emp.status;
        statusCell.appendChild(badge);
        row.appendChild(statusCell);

        const actions = document.createElement("td");
        actions.className = "text-nowrap";
        if (state.canEdit) {
            const edit = document.createElement("button");
            edit.className = "btn btn-warning btn-sm me-1";
            edit.textContent = "Edit";
            edit.addEventListener("click", () => startEdit(emp.id));
            actions.appendChild(edit);
        }

        if (state.isAdmin) {   // UI convenience only; the server enforces this rule too
            const del = document.createElement("button");
            del.className = "btn btn-danger btn-sm";
            del.textContent = "Delete";
            del.addEventListener("click", () => deleteEmployee(emp.id, emp.name));
            actions.appendChild(del);
        }
        if (!actions.hasChildNodes()) actions.textContent = "View only";
        row.appendChild(actions);
        body.appendChild(row);
    });
}

function renderPager() {
    $("pageInfo").textContent = `Page ${state.page + 1} of ${state.totalPages}`;
    $("prevBtn").disabled = state.page <= 0;
    $("nextBtn").disabled = state.page >= state.totalPages - 1;
}

function renderChart(byDepartment) {
    const labels = Object.keys(byDepartment).map((k) => DEPT_LABEL[k] ?? k);
    const values = Object.values(byDepartment);
    if (chart) {
        chart.data.labels = labels;
        chart.data.datasets[0].data = values;
        chart.update();
        return;
    }
    chart = new Chart($("employeeChart"), {
        type: "bar",
        data: {
            labels,
            datasets: [{
                label: "Employees",
                data: values,
                backgroundColor: ["#198754", "#212529", "#ffc107", "#0dcaf0"]
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
        }
    });
}

/* ---------------- Create / edit / delete ---------------- */

function readForm() {
    return {
        name: $("name").value.trim(),
        email: $("email").value.trim(),
        role: $("role").value.trim(),
        phone: $("phone").value.trim(),
        department: $("department").value || null,
        status: $("status").value || null,
        salary: $("salary").value === "" ? null : Number($("salary").value)
    };
}

function resetForm() {
    $("employeeForm").reset();
    $("employeeId").value = "";
    $("formTitle").textContent = "Add Employee";
    $("submitBtn").textContent = "Add Employee";
    $("cancelEditBtn").classList.add("d-none");
}

function startEdit(id) {
    const emp = state.rows.get(id);
    if (!emp) return;
    clearError();
    $("employeeId").value = emp.id;
    $("name").value = emp.name;
    $("email").value = emp.email;
    $("role").value = emp.role ?? "";
    $("phone").value = emp.phone ?? "";
    $("department").value = emp.department;
    $("status").value = emp.status;
    $("salary").value = emp.salary;
    $("formTitle").textContent = `Edit Employee #${emp.id}`;
    $("submitBtn").textContent = "Update Employee";
    $("cancelEditBtn").classList.remove("d-none");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

async function saveEmployee(event) {
    event.preventDefault();
    clearError();
    const id = $("employeeId").value;
    const button = $("submitBtn");
    button.disabled = true;

    try {
        await api(id ? `/api/employees/${id}` : "/api/employees", {
            method: id ? "PUT" : "POST",
            body: JSON.stringify(readForm())
        });
        showToast(id ? "Employee updated successfully" : "Employee added successfully");
        resetForm();
        await refreshAll();
    } catch (error) {
        if (error.message !== "Not authenticated") showError(error);
    } finally {
        button.disabled = false;
    }
}

async function deleteEmployee(id, name) {
    if (!confirm(`Delete ${name}? This cannot be undone.`)) return;
    clearError();
    try {
        await api(`/api/employees/${id}`, { method: "DELETE" });
        showToast("Employee deleted");
        await refreshAll();
    } catch (error) {
        if (error.status === 403) {
            showToast("Only an ADMIN can delete employees", "danger");
        } else if (error.message !== "Not authenticated") {
            showError(error);
        }
    }
}

/* ---------------- PDF export ---------------- */

async function downloadPDF() {
    try {
        const employees = await api("/api/employees/export");
        const { jsPDF } = window.jspdf;
        const doc = new jsPDF({ orientation: "landscape" });
        doc.setFontSize(16);
        doc.text("Employee Management Report", 14, 16);
        doc.setFontSize(9);
        doc.text(`Generated ${new Date().toLocaleString()}  |  ${employees.length} employees`, 14, 22);

        doc.autoTable({
            startY: 27,
            head: [["#", "Name", "Email", "Department", "Role", "Phone", "Status", "Salary (INR)"]],
            body: employees.map((e, i) => [
                i + 1, e.name, e.email, DEPT_LABEL[e.department] ?? e.department, e.role ?? "",
                e.phone ?? "", STATUS_LABEL[e.status] ?? e.status,
                Number(e.salary).toLocaleString("en-IN", { minimumFractionDigits: 2 })
            ]),
            styles: { fontSize: 8 },
            headStyles: { fillColor: [33, 37, 41] }
        });
        doc.save("employees.pdf");      // autoTable paginates automatically
    } catch (error) {
        if (error.message !== "Not authenticated") showError(error);
    }
}

/* ---------------- Misc ---------------- */

function updateDateTime() {
    $("dateTime").textContent = new Date().toLocaleString();
}

function applyDarkMode(on) {
    document.body.classList.toggle("dark-mode", on);
    try { localStorage.setItem("darkMode", on ? "1" : "0"); } catch (e) { /* ignore */ }
}

async function logout() {
    try { await fetch("/api/auth/logout", { method: "POST", credentials: "same-origin" }); } catch (e) { /* ignore */ }
    window.location.href = "/login.html";
}

function updateSortIndicators() {
    document.querySelectorAll("th.sortable").forEach((th) => {
        const label = th.dataset.label ?? (th.dataset.label = th.textContent.trim());
        const active = th.dataset.sort === state.sortBy;
        th.textContent = label + (active ? (state.direction === "asc" ? " ▲" : " ▼") : "");
    });
}

function goToFirstPageAndReload() {
    state.page = 0;
    loadEmployees();
}

/* ---------------- Start ---------------- */

async function init() {
    try {
        const me = await api("/api/auth/me");
        state.isAdmin = me.role === "ADMIN";
        state.canEdit = me.role === "ADMIN" || me.role === "HR";
        state.username = me.username;
        $("userBadge").textContent = `${me.username} (${me.role})`;
    } catch (e) {
        return;   // api() already redirected to the login page
    }

    // UI convenience only: the server rejects forbidden requests regardless of what is shown here
    if (!state.canEdit) {
        $("formCard").classList.add("d-none");
        $("pdfBtn").classList.add("d-none");
    }
    if (state.isAdmin) {
        $("userAdminCard").classList.remove("d-none");
    }

    try { applyDarkMode(localStorage.getItem("darkMode") === "1"); } catch (e) { /* ignore */ }

    $("employeeForm").addEventListener("submit", saveEmployee);
    $("cancelEditBtn").addEventListener("click", () => { resetForm(); clearError(); });
    $("logoutBtn").addEventListener("click", logout);
    $("pdfBtn").addEventListener("click", downloadPDF);
    $("darkModeBtn").addEventListener("click", () => applyDarkMode(!document.body.classList.contains("dark-mode")));
    $("prevBtn").addEventListener("click", () => { if (state.page > 0) { state.page--; loadEmployees(); } });
    $("nextBtn").addEventListener("click", () => { if (state.page < state.totalPages - 1) { state.page++; loadEmployees(); } });
    $("filterDepartment").addEventListener("change", goToFirstPageAndReload);
    $("filterStatus").addEventListener("change", goToFirstPageAndReload);

    // Debounce: wait until the user stops typing for 300 ms instead of calling the API on every keystroke
    $("searchInput").addEventListener("input", () => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(goToFirstPageAndReload, 300);
    });

    document.querySelectorAll("th.sortable").forEach((th) => {
        th.addEventListener("click", () => {
            const field = th.dataset.sort;
            state.direction = state.sortBy === field && state.direction === "asc" ? "desc" : "asc";
            state.sortBy = field;
            updateSortIndicators();
            goToFirstPageAndReload();
        });
    });

    updateSortIndicators();
    updateDateTime();
    setInterval(updateDateTime, 1000);
    await refreshAll();
    if (state.isAdmin) await loadUsers();
}

/* ---------------- Admin: manage users ---------------- */

async function loadUsers() {
    try {
        const users = await api("/api/admin/users");
        const body = $("userTable");
        body.replaceChildren();
        users.forEach((user) => {
            const row = document.createElement("tr");
            row.appendChild(td(user.id));
            row.appendChild(td(user.username));

            const roleCell = document.createElement("td");
            const select = document.createElement("select");
            select.className = "form-select form-select-sm";
            ["ADMIN", "HR", "USER"].forEach((role) => {
                const option = document.createElement("option");
                option.value = role;
                option.textContent = role;
                option.selected = role === user.role;
                select.appendChild(option);
            });
            if (user.username === state.username) {
                select.disabled = true;               // you cannot change your own role
                select.title = "You cannot change your own role";
            }
            select.addEventListener("change", () => changeRole(user.id, select.value));
            roleCell.appendChild(select);
            row.appendChild(roleCell);
            body.appendChild(row);
        });
    } catch (error) {
        if (error.message !== "Not authenticated") showError(error);
    }
}

async function changeRole(id, role) {
    clearError();
    try {
        await api(`/api/admin/users/${id}/role`, { method: "PUT", body: JSON.stringify({ role }) });
        showToast("Role updated");
    } catch (error) {
        showError(error);
    }
    await loadUsers();
}

init();
