const path = location.pathname;

const title = document.getElementById("loginTitle");
const icon = document.getElementById("loginIcon");
const button = document.getElementById("btnLogin");
const registerLink = document.getElementById("registerLink");
const adminSubTitle = document.getElementById("adminSubTitle");
const container = document.querySelector(".container");
const backLink = document.getElementById("backLink");

if (path.includes("/parent/login")) {
	document.body.classList.add("parent-login");

	title.textContent = "保護者ログイン";
	icon.textContent = "👨‍👩‍👧";
	button.className = "parent-button";
	registerLink.style.display = "block";

} else if (path.includes("/admin/login")) {
	document.body.classList.add("admin-login");

	title.textContent = "管理者ログイン";
	icon.textContent = "🛡";
	button.className = "admin-button";
	registerLink.style.display = "none";
	adminSubTitle.style.display = "block";
	container.classList.add("admin");
	backLink.style.display = "none";

} else {
	title.textContent = "こどもログイン";
	icon.textContent = "🌸";
	button.className = "child-button";
	registerLink.style.display = "none";
}

const mailAddress = document.getElementById("mailAddress");
const password = document.getElementById("password");

// 初期状態でボタンを非活性に
button.disabled = true;

function checkLoginForm() {
	const isValid =
		mailAddress.value.trim() !== "" &&
		password.value.trim() !== "";

	if (isValid) {
		button.classList.add("active");
		button.disabled = false;
	} else {
		button.classList.remove("active");
		button.disabled = true;
	}
}


mailAddress.addEventListener("input", checkLoginForm);
password.addEventListener("input", checkLoginForm);

checkLoginForm();