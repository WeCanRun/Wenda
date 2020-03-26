package com.nowcoder.service;

import com.nowcoder.async.EventModel;
import com.nowcoder.async.EventProducer;
import com.nowcoder.async.EventType;
import com.nowcoder.dao.LoginTicketDAO;
import com.nowcoder.dao.UserDAO;
import com.nowcoder.model.LoginTicket;
import com.nowcoder.model.User;
import com.nowcoder.util.JedisAdapter;
import com.nowcoder.util.RedisKeyUtil;
import com.nowcoder.util.WendaUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;


@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private LoginTicketDAO loginTicketDAO;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private JedisAdapter jedisAdapter;

    public User selectByName(String name) {
        return userDAO.selectByName(name);
    }

    public void active(int id) {
        userDAO.updateActive(id, 1);
    }


    public Map<String, Object> register(String username, String email, String password) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (StringUtils.isBlank(username)) {
            map.put("msg", "用户名不能为空");
            return map;
        }

        if (StringUtils.isBlank(email)) {
            map.put("msg", "邮箱不能为空");
            return map;
        }


        if (StringUtils.isBlank(password)) {
            map.put("msg", "密码不能为空");
            return map;
        }

        String pattern = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?";
        boolean isMatch = Pattern.matches(pattern, email);
        if (!isMatch){
            map.put("msg", "请输入正确的邮箱地址");
            return map;
        }

        User user = userDAO.selectByEmail(email);

        if (user != null) {
            map.put("msg", "邮箱已经被注册");
            return map;
        }

        user = userDAO.selectByName(username);

        if (user != null) {
            map.put("msg", "用户名已经被注册");
            return map;
        }

        // 密码强度
        user = new User();
        user.setName(username);
        user.setEmail(email);
        user.setActive(0);
        user.setSalt(UUID.randomUUID().toString().substring(0, 5));
        user.setCode(UUID.randomUUID().toString().replaceAll("-", ""));
        String head = String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000));
        user.setHeadUrl(head);
        user.setPassword(WendaUtil.MD5(password + user.getSalt()));
        userDAO.addUser(user);

        //发送激活邮件
        eventProducer.fireEvent(new EventModel(EventType.ACTIVE)
                .setExt("username", username).setExt("email", email)
                .setExt("code", user.getCode())
                .setActorId(user.getId()));

        // 登陆
        //String ticket = addLoginTicket(user.getId());
        //map.put("ticket", ticket);
        map.put("msg", "请查看邮件并激活账户");
        jedisAdapter.setex(RedisKeyUtil.getActiveKey(user.getId()),  60 * 60, user.getCode());
        return map;
    }


    public Map<String, Object> login(String username, String email, String password) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isBlank(username)) {
            map.put("msg", "用户名不能为空");
            return map;
        }

        if (StringUtils.isBlank(email)) {
            map.put("msg", "邮箱不能为空");
            return map;
        }

        if (StringUtils.isBlank(password)) {
            map.put("msg", "密码不能为空");
            return map;
        }

        User user1 = userDAO.selectByEmail(email);
        User user2 = userDAO.selectByName(username);
        if (user1 == null || user2 == null) {
            if (user1 == null) {
                map.put("msg", "邮箱不存在");
                return map;
            }

            map.put("msg", "用户名不存在");
            return map;
        }

        if (!user1.getName().equals(username) || !user2.getEmail().equals(email)) {
            map.put("msg", "请输入正确的用户名或邮箱");
            return map;
        }

        if (!WendaUtil.MD5(password + user1.getSalt()).equals(user1.getPassword())) {
            map.put("msg", "密码不正确");

            eventProducer.fireEvent(new EventModel(EventType.LOGIN)
                    .setExt("username", username).setExt("email", email)
                    .setActorId(user1.getId()));

            return map;
        }

        if (user1.getActive() == 0) {
            map.put("msg", "账户未激活，请前往邮箱查看激活邮件并激活");
            return map;
        }

        String ticket = addLoginTicket(user1.getId());
        map.put("ticket", ticket);
        map.put("userId", user1.getId());
        return map;
    }

    public String addLoginTicket(int userId) {
        LoginTicket ticket = new LoginTicket();
        ticket.setUserId(userId);
        Date date = new Date();
        date.setTime(date.getTime() + 1000 * 3600 * 24);
        ticket.setExpired(date);
        ticket.setStatus(0);
        ticket.setTicket(UUID.randomUUID().toString().replaceAll("-", ""));
        loginTicketDAO.addTicket(ticket);
        return ticket.getTicket();
    }

    public User getUser(int id) {
        return userDAO.selectById(id);
    }

    public void logout(String ticket) {
        loginTicketDAO.updateStatus(ticket, 1);
    }

    public void activeFail(int id) {
        userDAO.deleteById(id);
    }

    public Map<String, String> checkEmail(String email) {
        Map<String,String> map = new HashMap<>();
        if (email == null){
            map.put("mag", "邮箱不能为空");
        }

        String pattern = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?";
        boolean isMatch = Pattern.matches(pattern, email);
        if (!isMatch){
            map.put("msg", "请输入正确的邮箱地址");
            return map;
        }

        return map;
    }

    public Map<String,String> checkPassword(String password, String repassword) {
        Map<String,String> map = new HashMap<>();


        if (password == null){
            map.put("mag", "新密码不能为空");
            return map;
        }

        if (repassword == null){
            map.put("mag", "重复密码不能为空");
            return map;
        }

        if (!password.equals(repassword)){
            map.put("mag", "两次输入的密码不一致");
            return map;
        }

        if(password.length() < 6){
            map.put("mag", "密码长度需在 6 位以上");
            return map;
        }

        return map;
    }

    public Map<String ,String > resetPassword(String email, String password) {
        Map<String ,String > map = new HashMap<>();
        User user = userDAO.selectByEmail(email);
        String ticket = addLoginTicket(user.getId());
        map.put("ticket",ticket);
        userDAO.updatePassword(email, WendaUtil.MD5(password + user.getSalt()));

        return map;
    }

    public User selectByEmail(String email) {
       return userDAO.selectByEmail(email);
    }


}
