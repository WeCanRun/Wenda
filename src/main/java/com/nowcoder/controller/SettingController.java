package com.nowcoder.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/** Created by nowcoder on 2016/7/10. */
@Controller
public class SettingController {

  @RequestMapping(
      path = {"/settings"},
      method = {RequestMethod.GET})
  @ResponseBody
  public String setting(HttpSession httpSession) {
    return "Setting OK. ";
  }
}
