// Sends username + password only. The server decides the role (always USER) so a visitor
// can never make themselves an admin by editing this file or the request.
document.getElementById("registerForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const errorBox = document.getElementById("registerError");
    const button = document.getElementById("registerBtn");
    errorBox.classList.add("d-none");

    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;
    if (password !== document.getElementById("confirm").value) {
        errorBox.textContent = "Passwords do not match.";
        errorBox.classList.remove("d-none");
        return;
    }

    button.disabled = true;
    try {
        const response = await fetch("/api/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });
        if (response.ok) {
            window.location.href = "/login.html?registered=1";
            return;
        }
        let body = null;
        try { body = await response.json(); } catch (e) { /* no body */ }
        const details = body?.fieldErrors ? Object.values(body.fieldErrors).join(" ") : "";
        errorBox.textContent = details || body?.message || "Sign up failed.";
    } catch (e) {
        errorBox.textContent = "Cannot reach the server. Is it running?";
    }
    errorBox.classList.remove("d-none");
    button.disabled = false;
});
