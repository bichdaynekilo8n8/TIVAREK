
"use strict";

document.addEventListener("DOMContentLoaded", () => {
    const form = document.getElementById("repair-form");

    if (!form) return;

    const nameInput = document.getElementById("full-name");
    const phoneInput = document.getElementById("phone");
    const deviceInput = document.getElementById("device");
    const issueInput = document.getElementById("issue");
    const photoInput = document.getElementById("photo");
    const consentInput = document.getElementById("demo-consent");

    const feedback = document.getElementById("form-feedback");
    const counter = document.getElementById("issue-counter");
    const fileHint = document.getElementById("file-hint");

    const MAX_FILE_SIZE = 5 * 1024 * 1024;

    function showFeedback(message, success) {
        feedback.hidden = false;
        feedback.textContent = message;

        feedback.className =
            "form-feedback " + (success ? "success" : "error");

        feedback.focus();
    }

    function validatePhone() {
        const phone = phoneInput.value
            .trim()
            .replace(/[\s().-]/g, "");

        // Số di động Việt Nam: 03, 05, 07, 08, 09
        const regex = /^(0[35789]\d{8}|\+84[35789]\d{8})$/;

        phoneInput.setCustomValidity("");

        if (!regex.test(phone)) {
            phoneInput.setCustomValidity(
                "Nhập số di động Việt Nam hợp lệ (10 số hoặc +84)."
            );
        }
    }

    function validatePhoto() {
        photoInput.setCustomValidity("");

        const file = photoInput.files[0];

        if (!file) {
            fileHint.textContent =
                "JPG, PNG, WebP — tối đa 5 MB.";
            return;
        }

        const validExtension = /\.(jpg|jpeg|png|webp)$/i
            .test(file.name);

        const validType = !file.type ||
            ["image/jpeg", "image/png", "image/webp"]
                .includes(file.type);

        if (!validExtension || !validType) {
            photoInput.setCustomValidity(
                "Chỉ chấp nhận ảnh JPG, PNG hoặc WebP."
            );
            return;
        }

        if (file.size > MAX_FILE_SIZE) {
            photoInput.setCustomValidity(
                "Dung lượng ảnh không được vượt quá 5 MB."
            );
            return;
        }

        fileHint.textContent =
            `Đã chọn: ${file.name} (${(
                file.size / 1024 / 1024
            ).toFixed(2)} MB)`;
    }

    issueInput.addEventListener("input", () => {
        counter.textContent =
            `${issueInput.value.length}/1000 ký tự`;
    });

    phoneInput.addEventListener("input", () => {
        phoneInput.setCustomValidity("");
    });

    photoInput.addEventListener("change", validatePhoto);

    form.addEventListener("input", () => {
        // Ẩn thông báo cũ khi người dùng sửa dữ liệu
        feedback.hidden = true;
    });

    form.addEventListener("submit", event => {
        event.preventDefault();

        validatePhone();
        validatePhoto();

        // Chuẩn hóa khoảng trắng trong họ tên
        nameInput.value = nameInput.value
            .trim()
            .replace(/\s+/g, " ");

        issueInput.value = issueInput.value.trim();

        if (!form.checkValidity()) {
            form.reportValidity();

            showFeedback(
                "Vui lòng kiểm tra các trường bắt buộc và dữ liệu nhập.",
                false
            );

            return;
        }

        if (!consentInput.checked) {
            showFeedback(
                "Bạn cần xác nhận đây là form demo.",
                false
            );
            return;
        }

        // Frontend demo: KHÔNG gửi HTTP POST,
        // KHÔNG lưu localStorage và KHÔNG gửi lên Database.
        const deviceName = deviceInput.value;

        showFeedback(
            `✓ Dữ liệu hợp lệ cho thiết bị: ${deviceName}. ` +
            "Đây chỉ là kết quả kiểm tra thử nghiệm. " +
            "Thông tin CHƯA được gửi đến cửa hàng. " +
            "Cần xây dựng backend để tiếp nhận yêu cầu thật.",
            true
        );
    });
});
