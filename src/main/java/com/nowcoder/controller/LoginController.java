package com.nowcoder.controller;

import com.nowcoder.async.EventModel;
import com.nowcoder.async.EventProducer;
import com.nowcoder.async.EventType;
import com.nowcoder.model.User;
import com.nowcoder.service.UserService;
import com.nowcoder.util.JedisAdapter;
import com.nowcoder.util.RedisKeyUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sun.security.krb5.internal.Ticket;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;


@Controller
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    UserService userService;

    @Autowired
    EventProducer eventProducer;

    @Autowired
    JedisAdapter jedisAdapter;

    @RequestMapping(path = {"/reg/"}, method = {RequestMethod.POST})
    public String reg(Model model, @RequestParam("username") String username,
                      @RequestParam("email") String email,
                      @RequestParam("password") String password,
                      @RequestParam("next") String next,
                      @RequestParam(value = "rememberme", defaultValue = "false") boolean rememberme,
                      HttpServletResponse response) {
        try {
            Map<String, Object> map = userService.register(username, email, password);

            model.addAttribute("msg", map.get("msg"));
            return "login";

        } catch (Exception e) {
            logger.error("注册异常" + e.getMessage());
            model.addAttribute("msg", "服务器错误");
            return "login";
        }
    }

    @RequestMapping(path = {"/reglogin"}, method = {RequestMethod.GET})
    public String regloginPage(Model model, @RequestParam(value = "next", required = false) String next) {
        model.addAttribute("next", next);
        return "login";
    }

    @RequestMapping(path = {"/login/"}, method = {RequestMethod.POST})
    public String login(Model model, @RequestParam(value = "username", required = false) String username,
                        @RequestParam("email") String email,
                        @RequestParam("password") String password,
                        @RequestParam(value = "next", required = false) String next,
                        @RequestParam(value = "rememberme", defaultValue = "false") boolean rememberme,
                        HttpServletResponse response) {
        try {
            Map<String, Object> map = userService.login(username, email, password);
            if (map.containsKey("ticket")) {
                Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
                cookie.setPath("/");
                if (rememberme) {
                    cookie.setMaxAge(3600 * 24 * 5);
                }
                response.addCookie(cookie);


                if (StringUtils.isNotBlank(next)) {
                    return "redirect:" + next;
                }
                return "redirect:/";
            } else { // 登录失败
                model.addAttribute("msg", map.get("msg"));
                return "login";
            }

        } catch (Exception e) {
            logger.error("登陆异常:" + e.getMessage());
            return "login";
        }
    }

    @RequestMapping(path = {"/active/"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String active(Model model, @RequestParam("actor") int actor,
                         @RequestParam("code") String code) {
        String key = RedisKeyUtil.getActiveKey(actor);
        String getCode = jedisAdapter.get(key);
        jedisAdapter.delete(key);

        if (!getCode.equals(code)) {
            userService.activeFail(actor);
            model.addAttribute("msg", "账户激活失败");
            return "login";
        } else {
            userService.active(actor);

            model.addAttribute("msg", "激活成功，现在可以继续登录了！");
            return "login";
        }

    }

    /**
     * 退出登录
     * @param ticket
     * @return
     */
    @RequestMapping(path = {"/logout"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);
        return "login";
    }

    /**
     * 修改密码打开校验邮箱页面
     * @return
     */
    @RequestMapping(path = {"/check"}, method = {RequestMethod.GET})
    public String check() {

        return "check";
    }

    /**
     * 修改密码时校验邮箱
     * @param model
     * @param email
     * @return
     */
    @RequestMapping(path = {"/check"}, method = {RequestMethod.POST})
    public String reset(Model model, @RequestParam(value = "email") String email) {
        Map<String, String> map = userService.checkEmail(email);
        // 邮箱校验没有错误
        if (!map.containsKey("msg")) {
            String confirm = UUID.randomUUID().toString().replaceAll("-", "");
            String key = RedisKeyUtil.getConfirmKey(email);
            jedisAdapter.setex(key, 10 * 60, confirm);
            User user = userService.selectByEmail(email);

            eventProducer.fireEvent(new EventModel(EventType.RESETPASSWORD)
                    .setExt("username", user.getName())
                    .setExt("email", email)
                    .setExt("confirm", confirm));
            model.addAttribute("msg", "请查看邮件并确认修改");

            return "login";
        }
        model.addAttribute("msg", map.get("msg"));
        return "check";
    }

    /**
     * 校验是否可以重置密码
     * @param model
     * @param email
     * @param confirm
     * @return
     */
    @RequestMapping(path = {"/reset/"}, method = {RequestMethod.GET,RequestMethod.POST})
    public String reset(Model model, @RequestParam(value = "email") String email,
                        @RequestParam("confirm") String confirm) {
        try {
            String confirmKey = RedisKeyUtil.getConfirmKey(email);
            String check = jedisAdapter.get(confirmKey);
            jedisAdapter.delete(confirmKey);

            if (confirm.equals(check)) {
                model.addAttribute("msg", "现在可以设置你的新密码了");
                model.addAttribute("email", email);
                return "reset";

            }
            model.addAttribute("msg", "重置密码请求失败，请重新尝试");

        } catch (Exception e) {
            logger.error("重置密码失败：" + e.getMessage());
            model.addAttribute("msg", "重置密码请求失败，请重新尝试");
        }

        return "check";
    }

    /**
     * 重置密码
     * @param model
     * @param password
     * @param repassword
     * @param email
     * @param response
     * @return
     */
    @RequestMapping(path = {"/reset/password/"}, method = {RequestMethod.GET, RequestMethod.POST})
    public String reset(Model model, @RequestParam("password") String password,
                        @RequestParam("repassword") String repassword,
                        @RequestParam(value = "email") String email,
                        HttpServletResponse response) {
        try {
            Map<String, String> map = userService.checkPassword(password, repassword);
            if (!map.containsKey("msg")) {
                map = userService.resetPassword(email, password);

                if (map.containsKey("ticket")) {
                    Cookie cookie = new Cookie("ticket", map.get("ticket"));
                    cookie.setPath("/");

                    response.addCookie(cookie);
                }
            } else {
                model.addAttribute("msg", map.get("msg"));
            }
        } catch (Exception e) {
            logger.error("重置失败：" + e.getMessage());
            model.addAttribute("msg", "重置密码失败");
            return "reset";
        }
        return "redirect:/";
    }

}
