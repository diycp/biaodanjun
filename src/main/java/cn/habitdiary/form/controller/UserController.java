package cn.habitdiary.form.controller;
import cn.habitdiary.form.entity.User;
import cn.habitdiary.form.service.UserService;
import cn.habitdiary.form.utils.CookieUtil;
import cn.habitdiary.form.utils.EmailUtil;
import cn.habitdiary.form.utils.UUIDUtil;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Controller
public class UserController {
    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailUtil emailUtil;

    /**
     * 检查用户名是否已被注册
     * @param json
     * @return
     * @throws JSONException
     */
    @PostMapping(value = "/checkUser", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String checkUser(@RequestBody String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = new JSONObject();
        String username = (String)jsonObject.get("username");
        if(userService.checkUser(username)){
            data.put("msg","no");
        }
        else{
            data.put("msg","ok");
        }
        return data.toString();
    }

    /**
     * 检查邮箱是否已被绑定
     * @param json
     * @return
     * @throws JSONException
     */
    @PostMapping(value = "/checkEmail", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String checkEmail(@RequestBody String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = new JSONObject();
        String email = (String)jsonObject.get("email");
        if(userService.checkEmail(email)){
            data.put("msg","no");
        }
        else{
            data.put("msg","ok");
        }
        return data.toString();
    }

    /**
     * 注册
     * @param user
     * @param bindingResult
     * @param session
     * @return
     * @throws JSONException
     */
    //要设置produces,否则回调函数会出现中文乱码
    @PostMapping(value = "/doRegister",produces = "application/json;charset=utf-8")
    @ResponseBody
    public String doRegister(@Valid User user, BindingResult bindingResult,HttpSession session) throws JSONException {
        JSONObject data = new JSONObject();
        if (bindingResult.hasErrors()) {
            data.put("title","注册失败");
            data.put("content","请重新注册");
            return data.toString();
        }
        String pwd = user.getPassword().trim();
        String username = user.getUsername().trim();
        String email = user.getEmail().trim();
        pwd = DigestUtils.sha1Hex(pwd); //Sha1加密入库
        user = new User(username,pwd,email);
        userService.addUser(user);
        user = userService.selectUser(null,username,null);
        session.setAttribute("loginUser",user);
        Map<String,Object> mp = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new Date());
        mp.put("date",date);
        mp.put("username",username);
        try {
            emailUtil.sendTemplateMail(email, username + "，很高兴认识你", "greeting", mp);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            data.put("title","注册成功");
            data.put("content","欢迎来到表单君的世界");
            return data.toString();
        }


    }


    /**
     * 登录
     * @param json
     * @param session
     * @return
     * @throws JSONException
     */
    @PostMapping(value = "/doLogin", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String doLogin(@RequestBody String json,HttpSession session, HttpServletRequest httpServletRequest,
                          HttpServletResponse httpServletResponse) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = new JSONObject();
        String username = (String)jsonObject.get("username");
        String password = (String)jsonObject.get("password");
        Boolean rememberme = (Boolean) jsonObject.get("rememberme");

        String shapwd = DigestUtils.sha1Hex(password);
        String code = (String)jsonObject.get("code");
        User user1 = userService.selectUser(null,username,null);
        User user2 = userService.selectUser(null,null,username);
        String kaptchaExpected = (String)session.getAttribute("vrifyCode");
        if(!code.equalsIgnoreCase(kaptchaExpected)){
            data.put("title","登录失败");
            data.put("content","验证码错误");
            return data.toString();
        }
        if(user1 != null){
            if(shapwd.equals(user1.getPassword())){
                    session.setAttribute("loginUser",user1);
                    data.put("title","登录成功");
                    data.put("content","欢迎使用表单君");
                    if(rememberme.booleanValue() == true){
                        CookieUtil.setCookie(httpServletResponse,"username",username,CookieUtil.COOKIE_MAX_AGE);
                        CookieUtil.setCookie(httpServletResponse,"password",password,CookieUtil.COOKIE_MAX_AGE);
                    }else{
                        CookieUtil.removeCookie(httpServletRequest,httpServletResponse,"username");
                        CookieUtil.removeCookie(httpServletRequest,httpServletResponse,"password");
                    }
            }
            else{
                data.put("title","登录失败");
                data.put("content","用户名或密码错误");
            }
        }
        else if(user2 != null){
            if(shapwd.equals(user2.getPassword())){
                session.setAttribute("loginUser",user2);
                data.put("title","登录成功");
                data.put("content","欢迎使用表单君");
                if(rememberme.booleanValue() == true){
                    CookieUtil.setCookie(httpServletResponse,"username",username,CookieUtil.COOKIE_MAX_AGE);
                    CookieUtil.setCookie(httpServletResponse,"password",password,CookieUtil.COOKIE_MAX_AGE);
                }else{
                    CookieUtil.removeCookie(httpServletRequest,httpServletResponse,"username");
                    CookieUtil.removeCookie(httpServletRequest,httpServletResponse,"password");
                }
            }
            else{
                data.put("title","登录失败");
                data.put("content","用户名或密码错误");
            }
        }
        else{
            data.put("title","登录失败");
            data.put("content","用户不存在");

        }
        return data.toString();

    }

    /**
     * 尝试登录
     * @param json
     * @return
     * @throws JSONException
     */
    @PostMapping(value = "/tryLogin", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String tryLogin(@RequestBody String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = new JSONObject();
        String username = (String)jsonObject.get("username");
        String password = (String)jsonObject.get("password");
        String shapwd = DigestUtils.sha1Hex(password);
        User user = userService.selectUser(null,username,null);

        if(user != null){
            if(shapwd.equals(user.getPassword())){
                data.put("msg","ok");
            }
            else{
                data.put("msg","no");
            }
        }
        else{
            data.put("msg","no");
        }
        return data.toString();

    }


    /**
     * 修改密码
     * @param json
     * @return
     * @throws JSONException
     */
    @PostMapping(value = "/doChange", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String doChange(@RequestBody String json) throws JSONException {
        System.out.println(json);
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = new JSONObject();
        String username = (String)jsonObject.get("username");
        String newPwd = (String)jsonObject.get("newPwd");
        String shapwd = DigestUtils.sha1Hex(newPwd);
        userService.changePassword(username,shapwd);
        data.put("title","修改成功");
        data.put("content","快去登陆吧！");

        return data.toString();

    }


    @PostMapping(value = "/findBack", produces = "application/json;charset=utf-8")
    @ResponseBody
    public String findBack(@RequestBody String json) throws JSONException {
        JSONObject jsonObject = new JSONObject(json);
        JSONObject data = new JSONObject();
        String username = (String)jsonObject.get("username");
        User user = userService.selectUser(null,username,null);
       String email = user.getEmail();
        String newPwd = UUIDUtil.getUUID().substring(0,16);
        String shapwd = DigestUtils.sha1Hex(newPwd);
        userService.changePassword(username,shapwd);
        Map<String,Object> mp = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new Date());
        mp.put("date",date);
        mp.put("username",username);
        mp.put("password",newPwd);
        emailUtil.sendTemplateMail(email,"表单君找回密码","findBack",mp);
        data.put("title","操作成功");
        data.put("body1","临时密码已发送到您的邮箱：" + email);
        data.put("body2","请尽快前往修改密码");


        return data.toString();

    }








    /**
     * 退出登录
     * @param session
     * @param sessionStatus
     * @return
     */
    @GetMapping("/exit")
    public String exit(HttpSession session, SessionStatus sessionStatus,HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse){
        session.invalidate();
        CookieUtil.removeCookie(httpServletRequest,httpServletResponse,"username");
        CookieUtil.removeCookie(httpServletRequest,httpServletResponse,"password");
        return "redirect:/index";
    }





    @RequestMapping("/defaultKaptcha")
    public void defaultKaptcha(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception{
        byte[] captchaChallengeAsJpeg = null;
        ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
        try {
            //生产验证码字符串并保存到session中
            String createText = defaultKaptcha.createText();
            httpServletRequest.getSession().setAttribute("vrifyCode", createText);
            //使用生产的验证码字符串返回一个BufferedImage对象并转为byte写入到byte数组中
            BufferedImage challenge = defaultKaptcha.createImage(createText);
            ImageIO.write(challenge, "jpg", jpegOutputStream);
        } catch (IllegalArgumentException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        //定义response输出类型为image/jpeg类型，使用response输出流输出图片的byte数组
        captchaChallengeAsJpeg = jpegOutputStream.toByteArray();
        httpServletResponse.setHeader("Cache-Control", "no-store");
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setDateHeader("Expires", 0);
        httpServletResponse.setContentType("image/jpeg");
        ServletOutputStream responseOutputStream =
                httpServletResponse.getOutputStream();
        responseOutputStream.write(captchaChallengeAsJpeg);
        responseOutputStream.flush();
        responseOutputStream.close();
    }




}
