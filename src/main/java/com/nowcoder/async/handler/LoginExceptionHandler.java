package com.nowcoder.async.handler;

import com.nowcoder.async.EventHandler;
import com.nowcoder.async.EventModel;
import com.nowcoder.async.EventType;
import com.nowcoder.util.MailSender;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class LoginExceptionHandler implements EventHandler {
    @Autowired
    MailSender mailSender;

    @Override
    public void doHandle(EventModel model) {
        // xxxx判断发现这个用户登陆异常
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("username", model.getExt("username"));
        map.put("date", new Date());
        mailSender.sendWithHTMLTemplate(model.getExt("email"), "账户登录异常", "mails/login_exception.html", map);
    }

    @Override
    public List<EventType> getSupportEventTypes() {
        return Arrays.asList(EventType.LOGIN);
    }
}
