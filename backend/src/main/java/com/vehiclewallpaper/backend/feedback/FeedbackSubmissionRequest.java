package com.vehiclewallpaper.backend.feedback;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class FeedbackSubmissionRequest {

    @NotBlank(message = "请输入姓名。")
    @Size(max = 80, message = "姓名长度不能超过 80 个字符。")
    private String name;

    @NotBlank(message = "请输入邮箱地址。")
    @Email(message = "请输入有效的邮箱地址。")
    @Size(max = 160, message = "邮箱长度不能超过 160 个字符。")
    private String email;

    @NotBlank(message = "请输入反馈内容。")
    @Size(min = 5, max = 2000, message = "反馈内容长度需要在 5 到 2000 个字符之间。")
    private String message;

    @Size(max = 255, message = "页面地址过长。")
    private String page;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }
}
