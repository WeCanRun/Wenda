package com.nowcoder.async.handler;

import com.nowcoder.async.EventHandler;
import com.nowcoder.async.EventModel;
import com.nowcoder.async.EventType;
import com.nowcoder.util.MailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class ActiveHandler implements EventHandler {
    @Autowired
    MailSender mailSender;

    @Override
    public void doHandle(EventModel model) {

        Map<String, Object> map = new HashMap<>();
        map.put("username", model.getExt("username"));
        map.put("email", model.getExt("email"));
        map.put("code",model.getExt("code"));
        map.put("actor",model.getActorId());
        mailSender.sendWithHTMLTemplate(model.getExt("email"), "账户激活", "mails/active.html", map);
    }

    @Override
    public List<EventType> getSupportEventTypes() {
        return Arrays.asList(EventType.ACTIVE);
    }
}
