package com.tivarek.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Chỉ nhận dữ liệu của khách hàng, không có trường RoleID/IsAdmin. */
public record RegistrationInput(
    @NotBlank(message="Nhập tên đăng nhập.")
    @Pattern(regexp="[A-Za-z][A-Za-z0-9._-]{3,29}",message="Tên đăng nhập dài 4–30 ký tự, bắt đầu bằng chữ cái, chỉ dùng chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.")
    String username,

    @NotBlank(message="Nhập họ và tên.")
    @Size(max=180,message="Họ tên tối đa 180 ký tự.")
    String fullName,

    @NotBlank(message="Nhập số điện thoại.")
    @Pattern(regexp="\\+?[0-9]{9,15}",message="Số điện thoại gồm 9–15 chữ số, có thể bắt đầu bằng +.")
    String phone,

    @Email(message="Email không đúng định dạng.")
    @Size(max=254,message="Email quá dài.")
    String email,

    @NotBlank(message="Nhập mật khẩu.")
    @Size(min=12,max=72,message="Mật khẩu cần từ 12 đến 72 ký tự.")
    String password,

    @NotBlank(message="Nhập lại mật khẩu.")
    String confirmPassword
) {
    public static RegistrationInput empty() {
        return new RegistrationInput("","","","","","");
    }
}
