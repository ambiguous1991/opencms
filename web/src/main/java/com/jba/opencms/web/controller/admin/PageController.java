package com.jba.opencms.web.controller.admin;

import com.jba.opencms.page.PageService;
import com.jba.opencms.type.page.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;

@Controller
@RequestMapping("/dashboard/page")
public class PageController {

    @Autowired private PageService pageService;

    @RequestMapping(method = RequestMethod.GET)
    public String getLandingPage(Model model){
        List<Page> pages = pageService.findAll(true);

        model.addAttribute("pages", pages);

        return "dashboard/page/pages";
    }

    @RequestMapping(value = "/new", method = RequestMethod.POST)
    public RedirectView createNewPage(){
        Page page = new Page();

        page.setTitle("New page");
        page.setVisible(false);

        pageService.create(page);

        return new RedirectView("/dashboard/page/"+page.getId());
    }

    @RequestMapping(value = "/{pageId}", method = RequestMethod.GET)
    public String getPageDetails(
            @PathVariable("pageId") Long pageId,
            Model model){
        Page page = pageService.findOne(pageId, true);

        model.addAttribute("page", page);

        return "dashboard/page/page-details";
    }
}
