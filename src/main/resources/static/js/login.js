// Posts credentials to Spring Security. The server checks them against BCrypt hashes in MySQL.
// Nothing secret lives in this file.
if (new URLSearchParams(window.location.search).get("registered")) {
    document.getElementById("registeredMsg").classList.remove("d-none");
}

document.getElementById("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const errorBox = document.getElementById("loginError");
    const button = document.getElementById("loginBtn");
    errorBox.classList.add("d-none");
    button.disabled = true;

    const body = new URLSearchParams({
        username: document.getElementById("username").value.trim(),
        password: document.getElementById("password").value
    });

    try {
        const response = await fetch("/api/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body
        });
        if (response.ok) {
            window.location.href = "/index.html";
            return;
        }
        errorBox.textContent = "Invalid username or password.";
    } catch (e) {
        errorBox.textContent = "Cannot reach the server. Is it running?";
    }
    errorBox.classList.remove("d-none");
    button.disabled = false;
});
