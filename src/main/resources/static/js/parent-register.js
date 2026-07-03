const userName = document.getElementById("userName");
const mailAddress = document.getElementById("mailAddress");
const password = document.getElementById("password");
const passwordConfirm = document.getElementById("passwordConfirm");
const btnRegister = document.getElementById("btnRegister");

function checkRegisterForm() {

    const isValid =
        userName.value.trim() !== "" &&
        mailAddress.value.trim() !== "" &&
        password.value.trim() !== "" &&
        passwordConfirm.value.trim() !== "";

    if (isValid) {
        btnRegister.classList.add("active");
        btnRegister.disabled = false;
    } else {
        btnRegister.classList.remove("active");
        btnRegister.disabled = true;
    }
}

userName.addEventListener("input", checkRegisterForm);
mailAddress.addEventListener("input", checkRegisterForm);
password.addEventListener("input", checkRegisterForm);
passwordConfirm.addEventListener("input", checkRegisterForm);

checkRegisterForm();