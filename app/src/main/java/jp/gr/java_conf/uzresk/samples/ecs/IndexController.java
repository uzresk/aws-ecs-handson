package jp.gr.java_conf.uzresk.samples.ecs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.net.Inet4Address;

@Controller
public class IndexController {

    @Autowired
    private HttpServletRequest request;

    @GetMapping("/")
    public String messages(Model model) throws Exception {

        model.addAttribute("ipAddress", Inet4Address.getLocalHost().toString());
        model.addAttribute("cookies", request.getCookies());

        return "index";
    }
}
